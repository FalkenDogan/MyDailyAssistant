package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class KuranMealServisi {

    public static void main(String[] args) throws Exception {

        System.out.println(dailyKuranPage());
    }

    public static String dailyKuranPage() {
        try {
            String sayfaNo = String.valueOf(calculateDailyPageNumber());
            String apiUrl = "http://api.alquran.cloud/v1/page/" + sayfaNo + "/tr.diyanet";
            String jsonResponse = getRequest(apiUrl);
            List<String> ayetler = generateDailyPage(jsonResponse);
            return String.join("\n", ayetler);
        } catch (Exception e) {
            System.err.println("Kuran meali alınamadı: " + e.getMessage());
            return "Veri alınamadı";
        }
    }



    // API'den ham JSON verisini çeken metod
    public static String getRequest(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

    // JSON'u ayrıştırıp sadece textleri seçen metod
    public static List<String> generateDailyPage(String jsonString) {

        List<String> trMealMetni = new ArrayList();  // Türkçe meal metni

        try {
            JSONObject response = new JSONObject(jsonString);

            // "data" nesnesine giriyoruz
            JSONObject data = response.getJSONObject("data");

            // "ayahs" listesini alıyoruz
            JSONArray ayahs = data.getJSONArray("ayahs");

            System.out.println("--- GÜNLÜK KUR'AN-I KERİM MEALİ --- \n");

            for (int i = 0; i < ayahs.length(); i++) {
                JSONObject ayah = ayahs.getJSONObject(i);

                // Sadece "text" ve "numberInSurah" alanlarını seçiyoruz
                String mealMetni = ayah.getString("text");
                int ayetNo = ayah.getInt("numberInSurah");

                // Konsola veya metin kutusuna yazdırma
                trMealMetni.add("[" + ayetNo + "] " + mealMetni);
            }

        } catch (Exception e) {
            System.out.println("JSON Ayrıştırma Hatası: " + e.getMessage());
        }
    return trMealMetni;
    }
    public static int calculateDailyPageNumber() {
        java.time.LocalDate startDate = java.time.LocalDate.of(2026, 3, 8);
        java.time.LocalDate today = java.time.LocalDate.now();

        long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);

        if (daysSinceStart < 0) {
            return 1; // Vor dem Startdatum
        }

        int pageNumber = (int) (daysSinceStart + 1);
        return Math.min(pageNumber, 604); // Maximum bei 604
    }

}