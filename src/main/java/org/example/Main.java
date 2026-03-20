package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

            String prayerRaw = TelegramService.getHTML(prayerUrl);

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
            String weatherJson = TelegramService.getWeatherJson(lat, lon);
            double temp = TelegramService.getTemperature(weatherJson);

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
            TelegramService.sendMessage(botToken, chatId, message);
            System.out.println("Ana mesaj başarıyla gönderildi.");

            // Almanca kelime mesajını ayrı gönder (karakter limiti kaygısı)
            TelegramService.sendMessage(botToken, chatId, alancaKelimeText);
            System.out.println("Almanca kelime mesajı başarıyla gönderildi.");

        } catch (Exception e) {
            System.err.println("Genel hata oluştu:");
            e.printStackTrace();
        }
    }
}