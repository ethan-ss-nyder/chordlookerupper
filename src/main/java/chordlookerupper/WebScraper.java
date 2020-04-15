package chordlookerupper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.awt.Desktop;

import org.jsoup.Jsoup;

/**
 * A sample class demonstrating how to use Jsoup to parse all the results of a
 * Google page, given a query. If you're going to use this, please give credit!
 * 
 * <p>
 * To add the Jsoup dependency to your Gradle project, make sure you add
 * {@code implementation 'org.jsoup:jsoup:1.13.1'} to your {@code build.gradle}
 * 
 * @author Jordan Bancino
 */
public class WebScraper {

    public void browse(URI url) throws IOException {
        try {
            if (Desktop.isDesktopSupported()) {
                var desktop = Desktop.getDesktop();
                desktop.browse(url);
                return;
            }
        } catch (IllegalArgumentException e) {
            // GraalVM doesn't support the AWT Desktop, here's a fallback option.
            var os = System.getProperty("os.name").toLowerCase();
            var runtime = Runtime.getRuntime();
            if (os.contains("windows")) {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url.toString());
                return;
            } else if (os.contains("mac")) {
                runtime.exec("open " + url.toString());
                return;
            } else if (os.contains("nux") || os.contains("nix")) {
                runtime.exec("xdg-open" + url.toString());
                return;
            }
        }

        // If everything fails, there's always this. Kinda crappy UX, but at this
        // point it's all we've got.
        System.out.printf("Please open this URL in your browser: [ %s ]\n", url.toString());
    }

    /**
     * Use Jsoup to scrape a google page with the given query and get all the links
     * on the first result page. Usage is simple, just input your query, and this
     * method will take care of the rest.
     * 
     * @param query The normal, plain query to make. Nothing special needs to be
     *              done to this to prepare it, it will automatically be encoded.
     * @return A list of links relating to the query.
     * @throws MalformedURLException If something goes wrong with creating the URL.
     * @throws IOException           If there is an error connecting to google.
     * 
     * @author Jordan Bancino
     */
    public List<String> getGoogleLinks(String query) throws MalformedURLException, IOException {

        // Extracted from my browser. This is required to trick google into thinking
        // we're an actual user legitimately requesting something
        // because they block all automated requests normally.
        var userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:74.0) Gecko/20100101 Firefox/74.0";

        // The Google query that we can post our search terms to.
        var queryUrl = "https://www.google.com/search?q=";

        // Encode the inputted query so it can be appended to the query URL.
        var encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // Combine the url and the encoded query.
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(queryUrl).append(encodedQuery);

        // Open a connection.
        var connection = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
        connection.setRequestProperty("User-Agent", userAgent); // Make google think we're using Firefox.
        connection.setReadTimeout(2000); // Set a timeout of 2 seconds
        connection.connect();

        // Create a new string list for links.
        var returnLinks = new ArrayList<String>();

        // Let Jsoup take it from here.
        var source = Jsoup.parse(connection.getInputStream(), null, encodedQuery);
        // Google links are stored in div elements with a class "r".
        var elements = source.getElementsByClass("r");
        // Iterate over each element
        for (var element : elements) {
            // Get all the links in each query.
            var links = element.select("a[href]");
            // Iterate over each link
            for (var link : links) {
                // get the URL from the link
                var urlString = link.absUrl("href");
                // Only add the link if it actually has content, and if it isn't a webcache
                // link.
                if (!urlString.isBlank() && !urlString.contains("webcache.googleusercontent.com")) {
                    returnLinks.add(link.absUrl("href"));
                }
            }
        }
        // Return all the query result links
        return returnLinks;
    }
}