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
        double lat = 51.33;
        double lon = 6.56;

        try {

            // NAMAZ VAKİTLERİ
            String prayerRaw = getHTML("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Germany&method=13");

            JsonObject prayerData = JsonParser.parseString(prayerRaw)
                    .getAsJsonObject()
                    .get("data").getAsJsonObject()
                    .get("timings").getAsJsonObject();

            String fajr = prayerData.get("Fajr").getAsString();
            String dhuhr = prayerData.get("Dhuhr").getAsString();
            String asr = prayerData.get("Asr").getAsString();
            String maghrib = prayerData.get("Maghrib").getAsString();
            String isha = prayerData.get("Isha").getAsString();


            // HAVA DURUMU
            String weatherJson = getWeatherJson(lat, lon);

            double temp = Double.NaN;

            if (weatherJson != null) {
                temp = getTemperature(weatherJson);
            }


            // SINAV GERİ SAYIM
            long gunKaldi = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    LocalDate.of(2026, 4, 28)
            );


            // Hava durumu metni
            String weatherText;

            if (Double.isNaN(temp)) {
                weatherText = "Veri alınamadı";
            } else {
                weatherText = String.format("%.1f°C", temp);
            }


            // TELEGRAM MESAJI
            String message = String.format(
                    "📅 *%s - Günlük Bilgilendirme*\n\n" +
                            "🎯 *SINAV DURUMU*\n" +
                            "🏁 Büyük sınava tam *%d gün* kaldı!\n" +
                            "🚀 Odaklanmaya devam et.\n\n" +
                            "☁️ *Hava Durumu:* %s\n\n" +
                            "🕋 *Namaz Vakitleri (%s):*\n" +
                            "🌅 İmsak: %s\n" +
                            "📅 Öğle: %s\n" +
                            "🕌 İkindi: %s\n" +
                            "🌆 Akşam: %s\n" +
                            "🌙 Yatsı: %s",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    gunKaldi,
                    weatherText,
                    city,
                    fajr,
                    dhuhr,
                    asr,
                    maghrib,
                    isha
            );


            sendTelegram(botToken, chatId, message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // HTTP GET
    static String getHTML(String urlToRead) throws Exception {

        StringBuilder result = new StringBuilder();

        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (Scanner reader = new Scanner(conn.getInputStream())) {
            while (reader.hasNextLine()) {
                result.append(reader.nextLine());
            }
        }

        return result.toString();
    }


    // TELEGRAM GÖNDER
    static void sendTelegram(String token, String chatId, String text) throws Exception {

        String urlString =
                "https://api.telegram.org/bot" + token +
                        "/sendMessage?chat_id=" + chatId +
                        "&text=" + java.net.URLEncoder.encode(text, "UTF-8") +
                        "&parse_mode=Markdown";

        URL url = new URL(urlString);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        conn.getInputStream().read();
    }


    // JSON → TEMPERATURE
    public static double getTemperature(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // daha güvenli yol
            JsonNode currentWeather = root.path("current_weather");
            if (currentWeather.isMissingNode() || currentWeather.isNull()) {
                System.out.println("current_weather bulunamadı");
                return Double.NaN;
            }

            JsonNode tempNode = currentWeather.path("temperature");
            if (tempNode.isMissingNode() || tempNode.isNull()) {
                System.out.println("temperature bulunamadı");
                return Double.NaN;
            }

            return tempNode.asDouble();

        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    // HAVA DURUMU API
    public static String getWeatherJson(double lat, double lon) {

        try {

            String weatherUrl =
                    "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                            "&longitude=" + lon +
                            "&current_weather=true";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(weatherUrl))
                            .GET()
                            .build();

            HttpResponse<String> response =
                    client.send(
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