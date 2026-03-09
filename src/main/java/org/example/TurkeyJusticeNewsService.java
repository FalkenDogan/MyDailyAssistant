package org.example;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Türkiye hukuk ve yargı haberlerini RSS kaynaklarından çeken servis.
 * DeepSeek filtrelemesi olmadan, gün içindeki haberleri doğrudan döner.
 */
public class TurkeyJusticeNewsService {

    private static final int MAX_ITEMS = 5;

    private static final String[] RSS_URLS = {
            "https://www.sozcu.com.tr/feeds-rss-category-gundem",
            "https://www.birgun.net/rss/home",
            "https://artigercek.com/service/rss.php",
            "https://bianet.org/biamag.rss",
            "https://www.cumhuriyet.com.tr/rss/son_dakika.xml",
            "https://www.diken.com.tr/feed/",
            "https://rss.dw.com/rdf/rss-tur-all",
            "https://halktv.com.tr/service/rss.php"
    };

    /**
     * Belirtilen tarih için haber listesini döner.
     * Hiç haber bulunamazsa, tüm kaynaklardan en güncel haberleri döner.
     */
    public List<NewsItem> fetchNews(LocalDate targetDate) {
        ZoneId berlinZone = ZoneId.of("Europe/Berlin");
        List<NewsItem> items = new ArrayList<>();

        for (String urlStr : RSS_URLS) {
            try {
                URL feedUrl = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) feedUrl.openConnection();
                conn.setConnectTimeout(8_000);
                conn.setReadTimeout(8_000);
                SyndFeedInput input = new SyndFeedInput();
                try (XmlReader reader = new XmlReader(conn.getInputStream())) {
                    SyndFeed feed = input.build(reader);

                    for (SyndEntry entry : feed.getEntries()) {
                        Date pubDate = entry.getPublishedDate();
                        if (pubDate == null) continue;

                        LocalDate entryDate = pubDate.toInstant().atZone(berlinZone).toLocalDate();
                        if (entryDate.equals(targetDate)) {
                            String title = entry.getTitle();
                            String link = entry.getLink();
                            if (title != null && link != null) {
                                items.add(new NewsItem(title, link));
                            }
                        }

                        if (items.size() >= MAX_ITEMS) break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Hata: " + urlStr + " okunamadi. " + e.getMessage());
            }

            if (items.size() >= MAX_ITEMS) break;
        }

        return items;
    }

    /**
     * Haber listesini Telegram için formatlı metne çevirir.
     * Liste boşsa, "bugün haber yok" mesajı döner.
     */
    public String formatForTelegram(List<NewsItem> items, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Türkiye Hukuk ve Yargı Haberleri*\n");
        sb.append("📅 ").append(date.toString()).append("\n\n");

        if (items.isEmpty()) {
            sb.append("Bugün için haber bulunamadı.");
            return sb.toString();
        }

        for (NewsItem item : items) {
            // Başlıktaki özel Markdown karakterlerini temizle
            String safeTitle = item.title
                    .replace("*", "\\*")
                    .replace("_", "\\_")
                    .replace("[", "\\[")
                    .replace("`", "\\`");
            sb.append("🔹 ").append(safeTitle).append("\n");
            sb.append(item.link).append("\n\n");
        }

        return sb.toString().trim();
    }
}
