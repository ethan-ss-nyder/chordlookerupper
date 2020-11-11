package chordlookerupper;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class WebScraperTest {
    @Test
    public void testWebScraper() throws Exception {
        var scraper = new WebScraper();
        var links = scraper.getGoogleLinks("Hello World");
        assertTrue(links.size() > 0);
    }
}