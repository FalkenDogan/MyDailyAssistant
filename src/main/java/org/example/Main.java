package org.example;

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
            String prayerJson = getHTML("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Germany&method=13");

            // 2. Hava Durumunu Çek
            String weatherJson = getHTML("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true");
            // Direkt current_weather Objekt finden (nicht current_weather_units)
            int currentWeatherStart = weatherJson.lastIndexOf("\"current_weather\":{");
            String currentWeatherPart = weatherJson.substring(currentWeatherStart);

            // 3. Sınav Geri Sayımı (Hedef: 28.04.2026)
            LocalDate bugun = LocalDate.now();
            LocalDate sinavTarihi = LocalDate.of(2026, 4, 28);
            long gunKaldi = ChronoUnit.DAYS.between(bugun, sinavTarihi);

            // Verileri Ayıkla
            String fajr = getValue(prayerJson, "Fajr");
            String dhuhr = getValue(prayerJson, "Dhuhr");
            String asr = getValue(prayerJson, "Asr");
            String maghrib = getValue(prayerJson, "Maghrib");
            String isha = getValue(prayerJson, "Isha");
            String temp = getSimpleValue(currentWeatherPart, "temperature");
            // Mesaj Formatı
            String dateStr = bugun.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String message = String.format(
                    "📅 *%s - Günlük Bilgilendirme*\n\n" +
                            "🎯 *SINAV DURUMU*\n" +
                            "🏁 Büyük sınava tam *%d gün* kaldı!\n" +
                            "🚀 Odaklanmaya devam et.\n\n" +
                            "☁️ *Hava Durumu:* %s°C\n\n" +
                            "🕋 *Namaz Vakitleri (%s):*\n" +
                            "🌅 İmsak: %s\n" +
                            "📅 Öğle: %s\n" +
                            "🕌 İkindi: %s\n" +
                            "🌆 Akşam: %s\n" +
                            "🌙 Yatsı: %s",
                    dateStr, gunKaldi, temp, city, fajr, dhuhr, asr, maghrib, isha
            );

            sendTelegram(botToken, chatId, message);
            System.out.println("Sınav geri sayımı ve vakitler gönderildi!");

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    static String getValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        return json.substring(start, json.indexOf("\"", start));
    }

    private static String getSimpleValue(String json, String key) {
        // 1. Anahtarın konumunu bul
        int keyPos = json.indexOf("\"" + key + "\":");
        if (keyPos == -1) return "Hata";

        // 2. İki noktadan (:) sonrasına git
        int startSearch = json.indexOf(":", keyPos) + 1;

        // 3. İlk rakamı, eksi işaretini veya noktayı bulana kadar ilerle
        int start = -1;
        for (int i = startSearch; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == '.') {
                start = i;
                break;
            }
        }

        if (start == -1) return "N/A";

        // 4. Sayı bitene kadar (rakam, nokta veya eksi olduğu sürece) ilerle
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-') {
                end++;
            } else {
                break;
            }
        }

        return json.substring(start, end);
    }

    private static void sendTelegram(String token, String chatId, String text) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" +
                java.net.URLEncoder.encode(text, "UTF-8") + "&parse_mode=Markdown";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.getInputStream().read();
    }
}