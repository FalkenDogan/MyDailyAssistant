package org.example;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {

        String botToken = System.getenv("TELEGRAM_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");
        String city = "Krefeld"; // NRW bölgesi için merkez veya kendi şehrin
        String country = "Germany";

        try {
            // 1. Namaz Vakitlerini Getir (Method 13 = Diyanet)
            URL url = new URL("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=" + country + "&method=13");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder inline = new StringBuilder();
            while (sc.hasNext()) {
                inline.append(sc.nextLine());
            }
            sc.close();

            // 2. Basit JSON Parçalama (Kütüphane bağımlılığını azaltmak için manuel)
            String response = inline.toString();
            String fajr = getValue(response, "Fajr");
            String sunrise = getValue(response, "Sunrise");
            String dhuhr = getValue(response, "Dhuhr");
            String asr = getValue(response, "Asr");
            String maghrib = getValue(response, "Maghrib");
            String isha = getValue(response, "Isha");

            // 3. Mesaj Formatı
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String message = String.format(
                    "📅 *%s - %s Namaz Vakitleri*\n\n" +
                            "🌅 İmsak: %s\n" +
                            "☀️ Güneş: %s\n" +
                            "📅 Öğle: %s\n" +
                            "🕌 İkindi: %s\n" +
                            "🌆 Akşam: %s\n" +
                            "🌙 Yatsı: %s",
                    date, city, fajr, sunrise, dhuhr, asr, maghrib, isha
            );

            // 4. Telegram'a Gönder
            sendTelegram(botToken, chatId, message);
            System.out.println("Mesaj başarıyla gönderildi!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        int end = json.indexOf("\"", start);
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