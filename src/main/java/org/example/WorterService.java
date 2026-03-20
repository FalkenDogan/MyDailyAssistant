package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WorterService {

    public record WordExample(String sentence, String translation) {
    }

    public record DailyWord(int id, String word, String article, String type,
                           String meaningTr, String meaningDe,
                           List<String> synonyms, List<String> antonyms,
                           List<WordExample> examples, String tips) {
    }

    public static DailyWord selectWordByPageNumber(int pageNumber) {
        JsonArray words = loadWordsArray();
        if (words.isEmpty()) {
            throw new IllegalStateException("wort_list.json bos veya okunamadi");
        }

        int safePageNumber = Math.max(pageNumber, 1);
        int hedefId = safePageNumber; // Doğrudan sayfa numarasını id olarak kullan

        JsonObject wordObj = findById(words, hedefId);
        if (wordObj == null) {
            // Eğer exact id bulunamazsa, modulo ile döngü yap
            int index = (safePageNumber - 1) % words.size();
            wordObj = words.get(index).getAsJsonObject();
        }

        JsonObject meanings = wordObj.getAsJsonObject("meanings");
        return new DailyWord(
                getIntOrDefault(wordObj, "id", hedefId),
                getStringOrDefault(wordObj, "word", ""),
                getStringOrDefault(wordObj, "article", ""),
                getStringOrDefault(wordObj, "type", ""),
                meanings != null ? getStringOrDefault(meanings, "tr", "") : "",
                meanings != null ? getStringOrDefault(meanings, "de", "") : "",
                readStringArray(wordObj, "synonyms"),
                readStringArray(wordObj, "antonyms"),
                readExamples(wordObj),
                getStringOrDefault(wordObj, "tips", "")
        );
    }

    public static String buildDailyWordText(int pageNumber) {
        try {
            DailyWord word = selectWordByPageNumber(pageNumber);
            StringBuilder sb = new StringBuilder();

            sb.append("🇩🇪 *Günlük Almanca Kelime*\n")
                    .append("📚 Kelime No: *").append(word.id()).append("*\n\n");

            // Kelime ve tür
            sb.append("*").append(word.word().toUpperCase()).append("*");
            if (!word.article().isBlank()) {
                sb.append(" (").append(word.article()).append(")");
            }
            sb.append("\n");
            if (!word.type().isBlank()) {
                sb.append("Tür: _").append(word.type()).append("_\n");
            }
            sb.append("\n");

            // Anlamlar
            sb.append("📖 *Anlamlar*\n");
            if (!word.meaningTr().isBlank()) {
                sb.append("🇹🇷 Türkçe: ").append(word.meaningTr()).append("\n");
            }
            if (!word.meaningDe().isBlank()) {
                sb.append("🇩🇪 Almanca: ").append(word.meaningDe()).append("\n");
            }

            // Eş anlamlılar
            if (!word.synonyms().isEmpty()) {
                sb.append("\n🔄 *Eş Anlamlılar*\n");
                for (String synonym : word.synonyms()) {
                    if (!synonym.isBlank()) {
                        sb.append("• ").append(synonym).append("\n");
                    }
                }
            }

            // Zıt anlamlılar
            if (!word.antonyms().isEmpty()) {
                sb.append("\n❌ *Zıt Anlamlılar*\n");
                for (String antonym : word.antonyms()) {
                    if (!antonym.isBlank()) {
                        sb.append("• ").append(antonym).append("\n");
                    }
                }
            }

            // Örnek cümleler
            if (!word.examples().isEmpty()) {
                sb.append("\n✍️ *Örnek Cümleler*\n");
                for (WordExample example : word.examples()) {
                    sb.append("• \"").append(example.sentence()).append("\"\n");
                    sb.append("  _(").append(example.translation()).append(")_\n\n");
                }
            }

            // İpuçları
            if (!word.tips().isBlank()) {
                sb.append("\n💡 *İpucu*\n");
                sb.append(word.tips());
            }

            return sb.toString();
        } catch (Exception e) {
            System.err.println("Almanca kelime alinamadi: " + e.getMessage());
            return "🇩🇪 *Günlük Almanca Kelime*\nVeri alinamadi";
        }
    }

    private static JsonArray loadWordsArray() {
        String jsonText = loadWordsText();
        JsonElement root = JsonParser.parseString(jsonText);

        if (!root.isJsonArray()) {
            throw new IllegalStateException("wort_list.json dizi formatinda degil");
        }

        return root.getAsJsonArray();
    }


    private static String loadWordsText() {
        try (InputStream in = WorterService.class.getClassLoader().getResourceAsStream("wort_list.json")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Classpath denemesi basarisiz olursa dosya sisteminden devam et.
        }

        try {
            Path localFile = Path.of("wort_list.json");
            if (!Files.exists(localFile)) {
                throw new IllegalStateException("wort_list.json bulunamadi");
            }
            return Files.readString(localFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("wort_list.json okunamadi", e);
        }
    }

    private static JsonObject findById(JsonArray words, int hedefId) {
        for (JsonElement element : words) {
            JsonObject aday = element.getAsJsonObject();
            if (getIntOrDefault(aday, "id", -1) == hedefId) {
                return aday;
            }
        }
        return null;
    }

    private static List<String> readStringArray(JsonObject obj, String key) {
        List<String> result = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray(key);
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

    private static List<WordExample> readExamples(JsonObject wordObj) {
        List<WordExample> result = new ArrayList<>();
        JsonArray arr = wordObj.getAsJsonArray("examples");
        if (arr == null) {
            return result;
        }

        for (JsonElement item : arr) {
            JsonObject example = item.getAsJsonObject();
            result.add(new WordExample(
                    getStringOrDefault(example, "sentence", ""),
                    getStringOrDefault(example, "translation", "")
            ));
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

