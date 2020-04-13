package chordlookerupper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;

public class App implements Runnable {
    private final String clientId = "18917dddb06542c1b26f9efc141d9286";
    private final String clientSecret = "9f5f25f7384146a492f5ef26d1fb073c";
    private final String scope = "user-read-currently-playing";
    private final int tokenRecievePort = 8888;
    private final long updateInterval = 1000;

    private final Spotify spotify = new Spotify(clientId, clientSecret, scope, tokenRecievePort);

    private final String queryFormat = "%s chords";

    public static void main(String[] args) throws InterruptedException {
        Thread trackMonitor = new Thread(new App());
        trackMonitor.start();
        System.out.println("Started monitoring track changes.");
        trackMonitor.join();
    }

    @Override
    public void run() {
        try {
            spotify.authenticateUser(10000);
        } catch (SpotifyWebApiException | IOException e) {
            e.printStackTrace();
            System.err.println("Authentication error.");
            return;
        }
        
        var scraper = new WebScraper();
        String lastTrack = null;

        while (!Thread.interrupted()) {
            try {
                var track = spotify.getCurrentlyPlayingTrack();
                
                if (lastTrack == null) {
                    lastTrack = track.getName();
                }

                if (!lastTrack.equals(track.getName())) {
                    System.out.println("Track changed: " + track.getName());
                    var links = scraper.getGoogleLinks(String.format(queryFormat, track.getName()));
                    scraper.browse(new URI(links.get(0)));
                    lastTrack = track.getName();
                }
                
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
