package chordlookerupper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;

public class SpotifyHandler {

    private static final int serverPort = 8888;

    /** Information */
    private static final String clientId = "18917dddb06542c1b26f9efc141d9286";
    private static final String clientSecret = "9f5f25f7384146a492f5ef26d1fb073c";
    private static URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:" + serverPort);
    private static String code = "";

    /**
     * The builders, I don't even really know why. They do their job and make father
     * Spotify happy.
     */
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder().setClientId(clientId)
            .setClientSecret(clientSecret).setRedirectUri(redirectUri).build();
    private static AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
    private static final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("user-read-currently-playing").show_dialog(false).build();
    private static AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi
            .authorizationCodeRefresh().build();

    public static void init() throws IOException {
        var browser = new Browser();
        browser.openURI(authorizationCodeUriRequest.execute());
        buildAuthCodeRequestWithURI();
        authorizationCode_Sync();
        buildRefreshCodeRequest();
    }

    /** Used to convert the URI code into a useable access token. */
    public static void authorizationCode_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            // System.out.println("Authentication token recieved");
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Refreshes an access token.
     */
    public static void authorizationCodeRefresh_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            // System.out.println("Refresh token recieved");
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Handles getting initial access code from URI.
     * 
     * @return String that acts as the initial access code.
     * @throws IOException
     */
    public static String getURI() throws IOException {
        ServerSocket server = new ServerSocket(serverPort);
        while (true) {
            Socket clientSocket = server.accept();
            InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (!line.isEmpty()) {
                String[] strArray = line.split("=", 2);
                strArray[1] = strArray[1].substring(0, strArray[1].length() - 9);
                server.close();
                return strArray[1];
            }
        }
    }

    /**
     * To be used before first-time authorization code requests.
     * 
     * @throws IOException
     */
    public static void buildAuthCodeRequestWithURI() throws IOException {
        authorizationCodeRequest = spotifyApi.authorizationCode(getURI()).build();
    }

    /**
     * To be used before first-time refresh code requests.
     * 
     * @throws IOException
     */
    public static void buildRefreshCodeRequest() throws IOException {
        authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
    }

    /** Fetches the users currently playing track. */
    public static Track getUsersCurrentlyPlayingTrack_Sync() {
        try {
            final GetUsersCurrentlyPlayingTrackRequest getUsersCurrentlyPlayingTrackRequest = spotifyApi
                    .getUsersCurrentlyPlayingTrack().build();
            final CurrentlyPlaying currentlyPlaying = getUsersCurrentlyPlayingTrackRequest.execute();
            return currentlyPlaying.getItem();
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
}