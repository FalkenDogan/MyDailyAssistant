package org.example;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Türkiye hukuk ve yargı haberlerini Telegram'a gönderen giriş noktası.
 * DeepSeek filtrelemesi yapmadan doğrudan en güncel haberleri gönderir,
 * böylece AI filtrelemesinin boş sonuç döndürmesi durumunda mesaj kaybı yaşanmaz.
 */
public class SendNewsMain {

    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_TOKEN");
        String chatId   = System.getenv("TELEGRAM_CHAT_ID");

        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            System.err.println("HATA: TELEGRAM_TOKEN veya TELEGRAM_CHAT_ID environment variable tanımlı değil!");
            System.exit(1);
        }

        try {
            ZoneId berlinZone = ZoneId.of("Europe/Berlin");
            ZonedDateTime berlinNow = ZonedDateTime.now(berlinZone);
            // Önceki günün haberlerini çek (sabah 06:00 çalışma için dünün haberleri)
            LocalDate targetDate = berlinNow.toLocalDate().minusDays(1);

            System.out.println("Haberler çekiliyor. Hedef gün: " + targetDate + " (Europe/Berlin)");

            TurkeyJusticeNewsService newsService = new TurkeyJusticeNewsService();
            List<NewsItem> news = newsService.fetchNews(targetDate);

            System.out.println(news.size() + " haber bulundu.");

            String message = newsService.formatForTelegram(news, targetDate);

            System.out.println("Telegram'a gönderiliyor...");
            sendTelegram(botToken, chatId, message);
            System.out.println("Telegram'a başarıyla gönderildi.");

        } catch (Exception e) {
            System.err.println("Hata oluştu: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Telegram'a POST ile mesaj gönderir.
     * GET yerine POST kullanılır; uzun mesajlarda URL uzunluğu sorunu yaşanmaz.
     */
    static void sendTelegram(String token, String chatId, String text) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";

        byte[] body = ("chat_id=" + java.net.URLEncoder.encode(chatId, "UTF-8")
                + "&text=" + java.net.URLEncoder.encode(text, "UTF-8")
                + "&parse_mode=Markdown"
                + "&disable_web_page_preview=true")
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
            // Hata durumunda hata akışından yanıtı oku
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
