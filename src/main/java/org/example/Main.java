package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
public class Main {
    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");

        String city = "Krefeld";
        Double lat = 51.33;
        Double lon = 6.56;

        try {
            // 1. Namaz Vakitlerini Çek
            String prayerRaw = getHTML("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Germany&method=13");
            JsonObject prayerData = JsonParser.parseString(prayerRaw).getAsJsonObject()
                    .get("data").getAsJsonObject()
                    .get("timings").getAsJsonObject();

            // 2. Hava Durumunu Çek
            String json = getWeatherJson(lat, lon);

            double temp = getTemperature(json);

            // Namaz vakitleri
            String fajr = prayerData.get("Fajr").getAsString();
            String dhuhr = prayerData.get("Dhuhr").getAsString();
            String asr = prayerData.get("Asr").getAsString();
            String maghrib = prayerData.get("Maghrib").getAsString();
            String isha = prayerData.get("Isha").getAsString();

            // Sınav Geri Sayımı (28.04.2026)
            long gunKaldi = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2026, 4, 28));

            // Mesaj Formatı
            String message = String.format(
                    "📅 *%s - Günlük Bilgilendirme*\n\n" +
                            "🎯 *SINAV DURUMU*\n" +
                            "🏁 Büyük sınava tam *%d gün* kaldı!\n" +
                            "🚀 Odaklanmaya devam et.\n\n" +
                            "☁️ *Hava Durumu:* %.1f°C\n\n" +
                            "🕋 *Namaz Vakitleri (%s):*\n" +
                            "🌅 İmsak: %s\n" +
                            "📅 Öğle: %s\n" +
                            "🕌 İkindi: %s\n" +
                            "🌆 Akşam: %s\n" +
                            "🌙 Yatsı: %s",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    gunKaldi, temp, city, fajr, dhuhr, asr, maghrib, isha
            );

            sendTelegram(botToken, chatId, message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (Scanner reader = new Scanner(conn.getInputStream())) {
            while (reader.hasNextLine()) result.append(reader.nextLine());
        }

        return result.toString();
    }

    static void sendTelegram(String token, String chatId, String text) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId +
                "&text=" + java.net.URLEncoder.encode(text, "UTF-8") + "&parse_mode=Markdown";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.getInputStream().read();
    }

    public static double getTemperature(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            return root
                    .get("current_weather")
                    .get("temperature")
                    .asDouble();

        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    public static String getWeatherJson(double lat, double lon) {
        try {
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                    "&longitude=" + lon + "&current_weather=true";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
