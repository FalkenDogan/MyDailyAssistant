package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Main {
    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");

        String city = "Krefeld";
        String lat = "51.33";
        String lon = "6.56";

        try {
            // 1. Namaz Vakitlerini Çek
            String prayerRaw = getHTML("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Germany&method=13");
            JsonObject prayerData = JsonParser.parseString(prayerRaw).getAsJsonObject()
                    .get("data").getAsJsonObject()
                    .get("timings").getAsJsonObject();

            // 2. Hava Durumunu Çek
            String weatherRaw = getHTML("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true");
            JsonObject weatherCurrent = JsonParser.parseString(weatherRaw).getAsJsonObject()
                    .get("current_weather").getAsJsonObject();

            // 3. Verileri GSON ile Güvenli Şekilde Al
            double temp = weatherCurrent.get("temperature").getAsDouble();
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

        } catch (Exception e) { e.printStackTrace(); }
    }

    static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
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
}