public class TestTemperature {
    public static void main(String[] args) {
        System.out.println("Test basliyor...");

        String json = "{\"latitude\":51.322,\"longitude\":6.554,\"generationtime_ms\":0.1342296600341797,\"utc_offset_seconds\":0,\"timezone\":\"GMT\",\"timezone_abbreviation\":\"GMT\",\"elevation\":42.0,\"current_weather_units\":{\"time\":\"iso8601\",\"interval\":\"seconds\",\"temperature\":\"°C\",\"windspeed\":\"km/h\",\"winddirection\":\"°\",\"is_day\":\"\",\"weathercode\":\"wmo code\"},\"current_weather\":{\"time\":\"2026-03-07T21:00\",\"interval\":900,\"temperature\":12.2,\"windspeed\":5.8,\"winddirection\":18,\"is_day\":0,\"weathercode\":3}}";

        // Test 1: current_weather_units içindeki temperature (string)
        int currentWeatherStart = json.indexOf("\"current_weather\":{");
        String currentWeatherPart = json.substring(currentWeatherStart);
        System.out.println("currentWeatherPart: " + currentWeatherPart.substring(0, Math.min(100, currentWeatherPart.length())));

        String temp = getSimpleValue(currentWeatherPart, "temperature");
        System.out.println("Sicaklik: " + temp + " derece C");
        System.out.println("Beklenen: 12.2");
    }

    private static String getSimpleValue(String json, String key) {
        // Anahtarın değerinin başladığı konumu bul: "temperature":12.2
        int keyPos = json.indexOf("\"" + key + "\":");
        if (keyPos == -1) return "";

        // ":" karakterinden sonra başla
        int start = keyPos + key.length() + 3; // "key": uzunluğu

        // Boşlukları atla
        while (start < json.length() && json.charAt(start) == ' ') {
            start++;
        }

        // Değerin bittiği yeri bul (sayı değeri için)
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            // Rakam, nokta veya eksi işareti değilse dur
            if (c != '.' && c != '-' && (c < '0' || c > '9')) {
                break;
            }
            end++;
        }

        return json.substring(start, end).trim();
    }
}

