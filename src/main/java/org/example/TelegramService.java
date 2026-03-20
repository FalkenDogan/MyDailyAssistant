package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class TelegramService {

    /**
     * Telegram'a mesaj gönderi
     */
    public static void sendMessage(String token, String chatId, String text) throws Exception {
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

    /**
     * Hava durumu API'sinden JSON verisini alır
     */
    public static String getWeatherJson(double lat, double lon) {
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

    /**
     * JSON'dan sıcaklık değerini çıkarır
     */
    public static double getTemperature(String json) {
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

    /**
     * URL'den HTML/JSON verisini alır
     */
    public static String getHTML(String urlToRead) throws Exception {
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
}

