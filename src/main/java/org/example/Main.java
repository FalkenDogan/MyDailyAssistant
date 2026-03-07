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
            int currentWeatherStart = weatherJson.indexOf("\"current_weather\":{");
            int currentWeatherEnd = weatherJson.indexOf("}", currentWeatherStart + 20) + 1;
            String currentWeatherPart = weatherJson.substring(currentWeatherStart, currentWeatherEnd);

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
        // Anahtarın değerinin başladığı konumu bul: "temperature":12.2
        int keyPos = json.indexOf("\"" + key + "\":");
        if (keyPos == -1) return "";

        // ":" karakterinden sonra başla
        int start = keyPos + key.length() + 3; // "key": uzunluğu

        // Boşlukları atla
        while (start < json.length() && json.charAt(start) == ' ') {
            start++;
        }

        // Değerin bittiği yeri bul (sayı değeri için)
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            // Rakam, nokta veya eksi işareti değilse dur
            if (c != '.' && c != '-' && (c < '0' || c > '9')) {
                break;
            }
            end++;
        }

        return json.substring(start, end).trim();
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