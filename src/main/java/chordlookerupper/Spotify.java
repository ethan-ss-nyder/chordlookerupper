package chordlookerupper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.Supplier;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Track;

/**
 * A simple wrapper for the Spotify Java web API that handles all the
 * boilerplate functionality required to connect to Spotify.
 * 
 * @author Jordan Bancino
 */
public class Spotify {

    /**
     * Get an array of the available scopes that the Spotify API supports.
     * 
     * @return An array that contains all of the possible Spotify API scopes. See
     *         https://developer.spotify.com/documentation/general/guides/scopes/
     *         for information.
     */
    public static final String[] getAvailableScopes() {
        return new String[] {
                // Images
                "ugc-image-upload",

                // Spotify Connect
                "user-read-playback-state", "user-modify-playback-state", "user-read-currently-playing",

                // Playback
                "streaming", "app-remote-control",

                // Users
                "user-read-email", "user-read-private",

                // Playlists
                "playlist-read-collaborative", "playlist-modify-public", "playlist-read-private",
                "playlist-modify-private",

                // Library
                "user-library-modify", "user-library-read",

                // Listening History
                "user-top-read", "user-read-playback-position", "user-read-recently-played",

                // Follow
                "user-follow-read", "user-follow-modify" };
    }

    /**
     * A simple supplier that converts the available scopes to a string that the
     * Spotify API will accept.
     */
    private static final Supplier<String> scopeStrSupplier = () -> {
        var arrStr = Arrays.toString(getAvailableScopes());
        return arrStr.substring(1, arrStr.length() - 1).replace(" ", "");
    };

    private final SpotifyApi api;
    private final int serverPort;

    /**
     * Update the credentials every minute because Spotify expires them every so
     * often.
     */
    private final Runnable updateCredentials = () -> {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(60000);
                updateCredentials();
            } catch (IOException | SpotifyWebApiException e) {
                e.printStackTrace();
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Spotify credentials updater thread was interrupted.");
                break;
            }
        }
    };

    /**
     * Create a new Spotify wrapper.
     * 
     * @param clientId     The Spotify client ID to use. This is generated from the
     *                     developer console.
     * @param clientSecret The Spotify client secret to use. This is issued by the
     *                     developer console.
     * @param tokenPort    The port to run a server socket on to retrieve the
     *                     Spotify authorization token. This should match the port
     *                     specified on your Spotify developer console. Make sure
     *                     the URL is set to 'http://localhost:tokenPort', where
     *                     {@code tokenPort} is the port you put here.
     */
    public Spotify(String clientId, String clientSecret, int tokenPort) {
        this.serverPort = tokenPort;
        this.api = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret)
                .setRedirectUri(SpotifyHttpManager.makeUri("http://localhost:" + tokenPort)).build();
    }

    /**
     * Update our authorization credentials, because Spotify expires them after so
     * long.
     */
    private void updateCredentials() throws SpotifyWebApiException, IOException {
        var refreshRequest = api.authorizationCodeRefresh().build();
        var credentials = refreshRequest.execute();
        api.setAccessToken(credentials.getAccessToken());
        api.setRefreshToken(credentials.getRefreshToken());
    }

    /**
     * Get user authorization by opening a new browser window so the user an
     * authenticate us, then open a server socket so we can collect the Spotify
     * token.
     * 
     * @param timeout The maximum time, in milliseconds, that the server socket
     *                should remain open. This method will block the current thread
     *                for a maximum of this time, but maybe less if we get the token
     *                sooner.
     * 
     * @throws IOException            If there is an IO error
     * @throws SpotifyWebApiException If there is an authentication error.
     * @throws IllegalStateException  If there was an error retrieving the code. You
     *                                can assume that if this method returns, the
     *                                authorization completed.
     */
    public void authenticateUser() throws IOException, SpotifyWebApiException {
        // Build the initial authorization code request.
        var codeUriRequest = api.authorizationCodeUri().scope(scopeStrSupplier.get()).show_dialog(false).build();

        // Set up the browser to prompt the user for authentication.
        var browser = new WebScraper();
        browser.browse(codeUriRequest.execute());

        // This is the authentication token.
        String code = null;

        // Set up a server socket to recieve the authentication token,
        // because Spotify will redirect to localhost, if set up properly.
        ServerSocket server = new ServerSocket(serverPort);

        // We don't need this code in a loop because this will block the
        // current thread until a request is recieved.
        Socket clientSocket = server.accept();

        // Read the input stream and produce a line of client input
        // which is coming from the user's browser, because Spotify
        // will have redirected it to localhost.
        InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
        BufferedReader reader = new BufferedReader(isr);
        String line = reader.readLine();

        // This should close the current browser tab after the tocken has
        // recieved.
        OutputStreamWriter osw = new OutputStreamWriter(clientSocket.getOutputStream());
        osw.write("<script>window.close();</script>");

        // Close all our streams and sockets.
        osw.close();
        reader.close();
        isr.close();
        clientSocket.close();
        server.close();

        // Validate the client input.
        if (!line.isEmpty()) {
            String[] strArray = line.split("=", 2);
            code = strArray[1].substring(0, strArray[1].length() - 9);
        } else {
            throw new IOException("Invalid socket request.");
        }

        // If we have a code, set up the Spotify API to use it.
        if (code != null) {
            var codeRequest = api.authorizationCode(code).build();
            final var credentials = codeRequest.execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

            // Run a thread that updates the credentials every minute automatically.
            new Thread(updateCredentials).start();
        } else {
            throw new IllegalStateException("Authorization failed.");
        }
    }

    /**
     * Get the currently playing track from the Spotify API.
     * 
     * @return The currently playing track, or {@code null} if there isn't one.
     * 
     * @throws IOException            If there is an API error.
     * @throws SpotifyWebApiException If there is an API error.
     */
    public Track getCurrentlyPlayingTrack() throws SpotifyWebApiException, IOException {
        return api.getUsersCurrentlyPlayingTrack().build().execute().getItem();
    }
}