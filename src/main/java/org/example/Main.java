import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");
        String city = "Krefeld"; // Şehrini buradan güncelleyebilirsin

        // Krefeld Koordinatları (Hava durumu için hassas veri)
        String lat = "51.33";
        String lon = "6.56";

        try {
            // 1. Namaz Vakitlerini Çek (Diyanet Metodu)
            String prayerJson = getHTML("https://api.aladhan.com/v1/timingsByCity?city=" + city + "&country=Germany&method=13");

            // 2. Hava Durumunu Çek (Open-Meteo)
            String weatherJson = getHTML("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true");

            // Verileri Ayıkla
            String fajr = getValue(prayerJson, "Fajr");
            String dhuhr = getValue(prayerJson, "Dhuhr");
            String asr = getValue(prayerJson, "Asr");
            String maghrib = getValue(prayerJson, "Maghrib");
            String isha = getValue(prayerJson, "Isha");

            // Hava Durumu Verileri
            String temp = getSimpleValue(weatherJson, "temperature");
            String wind = getSimpleValue(weatherJson, "windspeed");

            // Mesajı Oluştur
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String message = String.format(
                    "📅 *%s - %s Özeti*\n\n" +
                            "☁️ *Hava Durumu:* %s°C\n" +
                            "💨 *Rüzgar:* %s km/s\n\n" +
                            "🕋 *Namaz Vakitleri:*\n" +
                            "🌅 İmsak: %s\n" +
                            "📅 Öğle: %s\n" +
                            "🕌 İkindi: %s\n" +
                            "🌆 Akşam: %s\n" +
                            "🌙 Yatsı: %s",
                    date, city, temp, wind, fajr, dhuhr, asr, maghrib, isha
            );

            sendTelegram(botToken, chatId, message);
            System.out.println("Hava durumu ve vakitler gönderildi!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (Scanner reader = new Scanner(conn.getInputStream())) {
            while (reader.hasNextLine()) result.append(reader.nextLine());
        }
        return result.toString();
    }

    private static String getValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        return json.substring(start, json.indexOf("\"", start));
    }

    private static String getSimpleValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 2;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
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