package chordlookerupper;

import java.io.IOException;

import com.wrapper.spotify.model_objects.specification.Track;

public class App {
    public static Track track;
    public static void main(String[] args) throws IOException, InterruptedException {
        SpotifyHandler.promptUserForAuthentication(SpotifyHandler.authorizationCodeUriRequest.execute().toString());
        SpotifyHandler.buildAuthCodeRequestWithURI();
        SpotifyHandler.authorizationCode_Sync();
        SpotifyHandler.buildRefreshCodeRequest();
        while (true) {
            Thread.sleep(2000);
            SpotifyHandler.authorizationCodeRefresh_Sync();
            track = SpotifyHandler.getUsersCurrentlyPlayingTrack_Sync();
            System.out.println("Artist: " + track.getArtists()[0].getName().toString());
            System.out.println("Song: " + track.getName());
        }
    }
}
