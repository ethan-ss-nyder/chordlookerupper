package chordlookerupper;

import java.io.IOException;

import com.wrapper.spotify.model_objects.specification.Track;

public class App {
    public static void main(String[] args) throws IOException {
        SpotifyHandler.init();

        var refreshThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                SpotifyHandler.authorizationCodeRefresh_Sync();
                Track track = SpotifyHandler.getUsersCurrentlyPlayingTrack_Sync();
                System.out.println("Artist: " + track.getArtists()[0].getName().toString());
                System.out.println("Song: " + track.getName());
            }
        });
        refreshThread.start();
    }
}
