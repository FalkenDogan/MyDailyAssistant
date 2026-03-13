package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HadisServisi {

    public record HadisSecimi(String hadisNo, String rivayetEden, String hadis, String kaynak) {
    }

    public static HadisSecimi secHadisByPageNumber(int pageNumber) {
        JsonArray hadisler = loadHadisArray();
        if (hadisler.isEmpty()) {
            throw new IllegalStateException("hadis.json bos veya okunamadi");
        }

        int safePageNumber = Math.max(pageNumber, 1);
        int index = (safePageNumber - 1) % hadisler.size();

        JsonObject hadisObj = hadisler.get(index).getAsJsonObject();
        return new HadisSecimi(
                getStringOrDefault(hadisObj, "hadisNo", String.valueOf(index + 1)),
                getStringOrDefault(hadisObj, "rivayetEden", ""),
                getStringOrDefault(hadisObj, "hadis", ""),
                getStringOrDefault(hadisObj, "kaynak", "")
        );
    }

    public static String buildDailyHadisText(int pageNumber) {
        try {
            HadisSecimi secim = secHadisByPageNumber(pageNumber);
            return String.format(
                    "\ud83d\udd4a\ufe0f *Gunluk Hadis*\n" +
                            "\ud83e\uddfe Hadis No: *%s*\n" +
                            "%s\n\n" +
                            "%s\n\n" +
                            "\ud83d\udcda Kaynak: %s",
                    secim.hadisNo(),
                    secim.rivayetEden(),
                    secim.hadis(),
                    secim.kaynak().isBlank() ? "Belirtilmedi" : secim.kaynak()
            );
        } catch (Exception e) {
            System.err.println("Hadis alinamadi: " + e.getMessage());
            return "\ud83d\udd4a\ufe0f *Gunluk Hadis*\nVeri alinamadi";
        }
    }

    private static JsonArray loadHadisArray() {
        String jsonText = loadHadisText();
        JsonElement root = JsonParser.parseString(jsonText);
        if (!root.isJsonArray()) {
            throw new IllegalStateException("hadis.json dizi formatinda degil");
        }
        return root.getAsJsonArray();
    }

    private static String loadHadisText() {
        try (InputStream in = HadisServisi.class.getClassLoader().getResourceAsStream("hadis.json")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Classpath denemesi basarisiz olursa dosya sisteminden devam et.
        }

        try {
            Path localFile = Path.of("hadis.json");
            if (!Files.exists(localFile)) {
                throw new IllegalStateException("hadis.json bulunamadi");
            }
            return Files.readString(localFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("hadis.json okunamadi", e);
        }
    }

    private static String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsString();
    }
}

