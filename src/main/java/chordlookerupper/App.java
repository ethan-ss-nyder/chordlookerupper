package chordlookerupper;

import java.io.IOException;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Track;

public class App {
    public static void main(String[] args) throws IOException {
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
        var browser = new Browser();
        String lastTrack = null;
        while (true) {
            try {
                var track = spotify.getCurrentlyPlayingTrack();
                System.out.printf("Currently playing track: " + track.getName());
                
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
