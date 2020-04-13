package chordlookerupper;

import java.io.IOException;
import java.net.URI;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;

public class App {
    public static void main(String[] args) throws IOException, SpotifyWebApiException {
        // SpotifyHandler.init();
        //
        // var refreshThread = new Thread(() -> {
        // while (!Thread.interrupted()) {
        // try {
        // Thread.sleep(2000);
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // break;
        // }
        // SpotifyHandler.authorizationCodeRefresh_Sync();
        // Track track = SpotifyHandler.getUsersCurrentlyPlayingTrack_Sync();
        // System.out.println("Artist: " + track.getArtists()[0].getName().toString());
        // System.out.println("Song: " + track.getName());
        // }
        // });
        // refreshThread.start();

        final String clientId = "18917dddb06542c1b26f9efc141d9286";
        final String clientSecret = "9f5f25f7384146a492f5ef26d1fb073c";
        final String redirectUri = "http://localhost:8888";
        final String scope = "user-read-currently-playing";

        var spotify = Spotify.createInstance(clientId, clientSecret, scope, redirectUri);
        spotify.authenticateUser(10000);
        var scraper = new WebScraper();
        String lastTrack = null;
        while (true) {
            try {
                var track = spotify.getCurrentlyPlayingTrack();
                System.out.println("Currently playing track: " + track.getName());
                if (lastTrack == null) {
                    lastTrack = track.getName();
                }

                if (!lastTrack.equals(track.getName())) {
                    var links = scraper.getGoogleLinks(String.format("%s chords", track.getName()));
                    scraper.browse(new URI(links.get(0)));
                    lastTrack = track.getName();
                }
                
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
