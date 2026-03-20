package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HadisServisi {

    public record DigerRivayet(String aciklama, String metin, String kaynak) {
    }

    public record HadisSecimi(int id, String konu, String rivayetEden, String hadis, String kaynak,
                              List<DigerRivayet> digerRivayetler, List<String> ekBilgi) {
    }

    public static HadisSecimi secHadisByPageNumber(int pageNumber) {
        JsonArray hadisler = loadHadisArray();
        if (hadisler.isEmpty()) {
            throw new IllegalStateException("temali_secme_hadisler.json bos veya okunamadi");
        }

        int safePageNumber = Math.max(pageNumber, 1);
        int hedefId = safePageNumber; // Doğrudan sayfa numarasını id olarak kullan

        JsonObject hadisObj = findById(hadisler, hedefId);
        if (hadisObj == null) {
            // Eğer exact id bulunamazsa, modulo ile döngü yap
            int index = (safePageNumber - 1) % hadisler.size();
            hadisObj = hadisler.get(index).getAsJsonObject();
        }

        return new HadisSecimi(
                getIntOrDefault(hadisObj, "id", hedefId),
                getStringOrDefault(hadisObj, "konu", ""),
                getStringOrDefault(hadisObj, "ravi", ""),
                getStringOrDefault(hadisObj, "metin", ""),
                getStringOrDefault(hadisObj, "kaynak", ""),
                readDigerRivayetler(hadisObj),
                readEkBilgi(hadisObj)
        );
    }

    public static String buildDailyHadisText(int pageNumber) {
        try {
            HadisSecimi secim = secHadisByPageNumber(pageNumber);
            StringBuilder sb = new StringBuilder();
            sb.append("\ud83d\udd4a\ufe0f *Gunluk Hadis*\n")
                    .append("\ud83e\uddfe Hadis No: *").append(secim.id()).append("*\n");

            if (!secim.konu().isBlank()) {
                sb.append("\ud83c\udff7\ufe0f Konu: *").append(secim.konu()).append("*\n");
            }

            if (!secim.rivayetEden().isBlank()) {
                sb.append("\ud83d\udcdc Rivayet eden: ").append(secim.rivayetEden()).append("\n\n");
            } else {
                sb.append("\n");
            }

            sb.append("\"_").append(secim.hadis()).append("_\"\n\n")
                    .append("\ud83d\udcda Kaynak: ")
                    .append(secim.kaynak().isBlank() ? "Belirtilmedi" : secim.kaynak());

            if (!secim.digerRivayetler().isEmpty()) {
                sb.append("\n\n\ud83d\udd01 *Diger Rivayetler*");
                for (DigerRivayet rivayet : secim.digerRivayetler()) {
                    sb.append("\n");
                    if (!rivayet.aciklama().isBlank()) {
                        sb.append("\n\u2022 ").append(rivayet.aciklama());
                    }
                    if (!rivayet.metin().isBlank()) {
                        sb.append("\n\"").append(rivayet.metin()).append("\"");
                    }
                    if (!rivayet.kaynak().isBlank()) {
                        sb.append("\nKaynak: ").append(rivayet.kaynak());
                    }
                }
            }

            if (!secim.ekBilgi().isEmpty()) {
                sb.append("\n\n\ud83d\udca1 *Aciklama*");
                for (String aciklama : secim.ekBilgi()) {
                    if (!aciklama.isBlank()) {
                        sb.append("\n\u2022 ").append(aciklama);
                    }
                }
            }

            return sb.toString();
        } catch (Exception e) {
            System.err.println("Hadis alinamadi: " + e.getMessage());
            return "\ud83d\udd4a\ufe0f *Gunluk Hadis*\nVeri alinamadi";
        }
    }

    private static JsonArray loadHadisArray() {
        String jsonText = loadHadisText();
        JsonElement root = JsonParser.parseString(jsonText);

        if (!root.isJsonObject()) {
            throw new IllegalStateException("temali_secme_hadisler.json nesne formatinda degil");
        }

        JsonObject rootObj = root.getAsJsonObject();
        JsonArray hadisler = rootObj.getAsJsonArray("hadisler");
        if (hadisler == null) {
            throw new IllegalStateException("JSON icinde hadisler dizisi bulunamadi");
        }
        return hadisler;
    }

    private static String loadHadisText() {
        try (InputStream in = HadisServisi.class.getClassLoader().getResourceAsStream("temali_secme_hadisler.json")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Classpath denemesi basarisiz olursa dosya sisteminden devam et.
        }

        try {
            Path localFile = Path.of("temali_secme_hadisler.json");
            if (!Files.exists(localFile)) {
                throw new IllegalStateException("temali_secme_hadisler.json bulunamadi");
            }
            return Files.readString(localFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("temali_secme_hadisler.json okunamadi", e);
        }
    }

    private static JsonObject findById(JsonArray hadisler, int hedefId) {
        for (JsonElement element : hadisler) {
            JsonObject aday = element.getAsJsonObject();
            if (getIntOrDefault(aday, "id", -1) == hedefId) {
                return aday;
            }
        }
        return null;
    }

    private static List<DigerRivayet> readDigerRivayetler(JsonObject hadisObj) {
        List<DigerRivayet> result = new ArrayList<>();
        JsonArray arr = hadisObj.getAsJsonArray("diger_rivayetler");
        if (arr == null) {
            return result;
        }

        for (JsonElement item : arr) {
            JsonObject rivayet = item.getAsJsonObject();
            result.add(new DigerRivayet(
                    getStringOrDefault(rivayet, "aciklama", ""),
                    getStringOrDefault(rivayet, "metin", ""),
                    getStringOrDefault(rivayet, "kaynak", "")
            ));
        }
        return result;
    }

    private static List<String> readEkBilgi(JsonObject hadisObj) {
        List<String> result = new ArrayList<>();
        JsonArray arr = hadisObj.getAsJsonArray("ek_bilgi");
        if (arr == null) {
            return result;
        }

        for (JsonElement item : arr) {
            if (!item.isJsonNull()) {
                result.add(item.getAsString());
            }
        }
        return result;
    }

    private static int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsString();
    }
}

