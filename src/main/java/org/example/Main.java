package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        String botToken  = System.getenv("TELEGRAM_TOKEN");
        String chatId    = System.getenv("TELEGRAM_CHAT_ID");

        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            System.err.println("HATA: TELEGRAM_TOKEN veya TELEGRAM_CHAT_ID environment variable tanımlı değil!");
            return;
        }

        String city = "Krefeld";
        double lat  = 51.33;
        double lon  = 6.56;

        try {
            // ────────────────────────────────────────────────
            // 1. Namaz vakitleri
            // ────────────────────────────────────────────────
            String prayerUrl = "https://api.aladhan.com/v1/timingsByCity?city=" + city +
                    "&country=Germany&method=13";

            String prayerRaw = getHTML(prayerUrl);

            JsonObject prayerData = JsonParser.parseString(prayerRaw)
                    .getAsJsonObject()
                    .get("data").getAsJsonObject()
                    .get("timings").getAsJsonObject();

            String fajr    = prayerData.get("Fajr").getAsString();
            String dhuhr   = prayerData.get("Dhuhr").getAsString();
            String asr     = prayerData.get("Asr").getAsString();
            String maghrib = prayerData.get("Maghrib").getAsString();
            String isha    = prayerData.get("Isha").getAsString();

            // ────────────────────────────────────────────────
            // 2. Hava durumu
            // ────────────────────────────────────────────────
            String weatherJson = getWeatherJson(lat, lon);
            double temp = getTemperature(weatherJson);

            String weatherText = Double.isNaN(temp)
                    ? "Veri alınamadı"
                    : String.format("%.1f°C", temp);

            // ────────────────────────────────────────────────
            // 3. Günlük Kur'an-ı Kerim Meali
            // ────────────────────────────────────────────────
            int kuranSayfaNo  = KuranMealServisi.calculateDailyPageNumber();
            String kuranMeali = KuranMealServisi.dailyKuranPage();
            String hadisText  = HadisServisi.buildDailyHadisText(kuranSayfaNo);

            // ────────────────────────────────────────────────
            // 4. Günlük Almanca Kelime
            // ────────────────────────────────────────────────
            String alancaKelimeText = WorterService.buildDailyWordText(kuranSayfaNo);

            // ────────────────────────────────────────────────
            // 5. Sınav geri sayım
            // ────────────────────────────────────────────────
            LocalDate today     = LocalDate.now();
            LocalDate examDate  = LocalDate.of(2026, 4, 28);
            long gunKaldi       = ChronoUnit.DAYS.between(today, examDate);

            // ────────────────────────────────────────────────
            // 6. Mesaj oluştur
            // ────────────────────────────────────────────────
            String message = String.format(
                    "📅 *%s - Günlük Bilgilendirme*\n\n" +
                            "🎯 *SINAV DURUMU*\n" +
                            "🏁 Büyük sınava tam *%d gün* kaldı!\n" +
                            "🚀 Odaklanmaya devam et.\n\n" +
                            "☁️ *Hava Durumu (%s):* %s\n\n" +
                            "🕋 *Namaz Vakitleri (Krefeld):*\n" +
                            "🌅 İmsak:   %s\n" +
                            "🕌 Öğle:    %s\n" +
                            "🕌 İkindi:  %s\n" +
                            "🌆 Akşam:   %s\n" +
                            "🌙 Yatsı:   %s\n\n" +
                            "📖 *Günlük Kur'an-ı Kerim Meali*\n" +
                            "📄 Sayfa Numarası: *%d*\n\n" +
                            "%s\n\n" +
                            "%s",
                    today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    gunKaldi,
                    city,
                    weatherText,
                    fajr,
                    dhuhr,
                    asr,
                    maghrib,
                    isha,
                    kuranSayfaNo,
                    kuranMeali,
                    hadisText
            );

            // ────────────────────────────────────────────────
            // 7. Telegram'a mesajları gönder
            // ────────────────────────────────────────────────
            sendTelegram(botToken, chatId, message);
            System.out.println("Ana mesaj başarıyla gönderildi.");

            // Almanca kelime mesajını ayrı gönder (karakter limiti kaygısı)
            sendTelegram(botToken, chatId, alancaKelimeText);
            System.out.println("Almanca kelime mesajı başarıyla gönderildi.");

        } catch (Exception e) {
            System.err.println("Genel hata oluştu:");
            e.printStackTrace();
        }
    }

    // ────────────────────────────────────────────────────────────
    //   Yardımcı metodlar
    // ────────────────────────────────────────────────────────────

    static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        try (Scanner reader = new Scanner(conn.getInputStream())) {
            while (reader.hasNextLine()) {
                result.append(reader.nextLine());
            }
        }

        return result.toString();
    }

    static String getWeatherJson(double lat, double lon) {
        try {
            String urlStr = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
                    lat, lon);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Hava API HTTP hatası: " + response.statusCode());
                return null;
            }

            return response.body();

        } catch (Exception e) {
            System.err.println("Hava durumu alınırken hata:");
            e.printStackTrace();
            return null;
        }
    }

    static double getTemperature(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Double.NaN;
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject current = root.getAsJsonObject("current_weather");

            if (current == null || !current.has("temperature")) {
                System.err.println("current_weather veya temperature anahtarı yok");
                return Double.NaN;
            }

            return current.get("temperature").getAsDouble();

        } catch (Exception e) {
            System.err.println("Hava JSON parse hatası:");
            e.printStackTrace();
            return Double.NaN;
        }
    }

    static void sendTelegram(String token, String chatId, String text) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";

        byte[] body = ("chat_id=" + java.net.URLEncoder.encode(chatId, "UTF-8")
                + "&text=" + java.net.URLEncoder.encode(text, "UTF-8")
                + "&parse_mode=Markdown")
                .getBytes("UTF-8");

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        try (var out = conn.getOutputStream()) {
            out.write(body);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorBody = "";
            try (var err = conn.getErrorStream()) {
                if (err != null) errorBody = new String(err.readAllBytes(), "UTF-8");
            }
            throw new RuntimeException(
                    "Telegram gönderme hatası → HTTP " + responseCode + ": " + errorBody);
        }

        // Başarılı yanıtı temizle
        try (var in = conn.getInputStream()) {
            in.readAllBytes();
        }
    }
}