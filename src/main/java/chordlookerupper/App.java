package chordlookerupper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;

/**
 * The main class for this program. It sets up a thread that monitors changes in
 * the user's currently playing track, and Googles the track name, along with
 * the query if the track is new.
 * 
 * @author Jordan Bancino
 */
public class App implements Runnable {
    // You really shouldn't have your credentials programmed in like this.
    // Ideally, you'll have users create their own API app and use their
    // own account. But oh well!
    private final String clientId = "18917dddb06542c1b26f9efc141d9286";
    private final String clientSecret = "9f5f25f7384146a492f5ef26d1fb073c";
    // The reciever port. A server socket is opened on this port so make sure
    // this is higher than 1024 to avoid needing root access.
    private final int tokenRecievePort = 8888;
    // How often to check for track changes (in milliseconds). Smaller values
    // will be more responsive, but consume more CPU clock cycles.
    private final long updateInterval = 1000;

    // The query format. The track name is passed in for the first %s and the artist name is passed in for the second %s.
    // This entire string is passed into the Google query when the track changes.
    private final String queryFormat = "%s chords %s";

    // Set up the Spotify client.
    private final Spotify spotify = new Spotify(clientId, clientSecret, tokenRecievePort);

    /**
     * The main entry method. This authenticates the user, then starts the
     * monitoring thread.
     * 
     * @param args The command line arguments are not used in this simple program.
     */
    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        System.out.println("Waiting for authentication...");
        try {
            app.spotify.authenticateUser();
        } catch (SpotifyWebApiException | IOException e) {
            e.printStackTrace();
            System.err.println("Authentication error.");
            return;
        }

        System.out.println("Authentiated successfully.");

        Thread trackMonitor = new Thread(app);
        trackMonitor.start();
        System.out.println("Started monitoring track changes.");
        trackMonitor.join();
    }

    /**
     * Here's where all of the business logic happens.
     */
    @Override
    public void run() {
        // Create a new scraper to get links, and to open pages.
        var scraper = new WebScraper();

        // Store the last known track; this is how we monitor for changes.
        String lastTrack = null;

        // Run indefinitely, as long as the thread isn't interuppted.
        while (!Thread.interrupted()) {
            try {
                var track = spotify.getCurrentlyPlayingTrack();

                // The track API may return null if no track is playing.
                if (track != null) {
                    // Only open the track query if it was changed.
                    if (lastTrack == null || !lastTrack.equals(track.getName())) {
                        System.out.println("Track changed: " + track.getName() + " by " + track.getArtists()[0].getName());
                        // Scrape Google for links on this query. Check the WebScraper documentation for
                        // logistics.
                        var links = scraper.getGoogleLinks(String.format(queryFormat, track.getName(), track.getArtists()[0].getName()));
                        scraper.browse(new URI(links.get(0)));
                        lastTrack = track.getName();
                    }

                } else {
                    System.out.println("No track currently playing.");
                }
                // Wait the update interval.
                Thread.sleep(updateInterval);
            } catch (SpotifyWebApiException e) {
                e.printStackTrace();
                System.err.println("Spotify error occured.");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Unable to perform Google query.");
                return;
            } catch (URISyntaxException e) {
                e.printStackTrace();
                System.err.println("Google returned a malformed link.");
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
