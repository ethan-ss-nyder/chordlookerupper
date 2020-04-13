package chordlookerupper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;

public class Spotify {

    private final SpotifyApi api;
    private final String scope;
    private final int serverPort;

    private Thread updateCredentialsThread;

    public Spotify(String clientId, String clientSecret, String scope, int tokenPort) {
        this.serverPort = tokenPort;
        this.scope = scope;
        this.api = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(SpotifyHttpManager.makeUri("http://localhost:" + tokenPort))
            .build();
    }

    private void updateCredentials(AuthorizationCodeRefreshRequest refreshRequest)
            throws SpotifyWebApiException, IOException {
        var credentials = refreshRequest.execute();
        api.setAccessToken(credentials.getAccessToken());
        api.setRefreshToken(credentials.getRefreshToken());
    }

    public void authenticateUser(long timeout) throws IOException, SpotifyWebApiException {
        var codeUriRequest = api.authorizationCodeUri()
            .scope(scope)
            .show_dialog(false)
            .build();

        var browser = new WebScraper();
        browser.browse(codeUriRequest.execute());

        String code = null;

        ServerSocket server = new ServerSocket(serverPort);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime <= timeout) {
            Socket clientSocket = server.accept();
            InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (!line.isEmpty()) {
                String[] strArray = line.split("=", 2);
                code = strArray[1].substring(0, strArray[1].length() - 9);
                server.close();
                break;
            }
        }

        if (code != null) {
            var codeRequest = api.authorizationCode(code).build();
            final var credentials = codeRequest.execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

            var codeRefresh = api.authorizationCodeRefresh().build();
            updateCredentialsThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    try {
                        updateCredentials(codeRefresh);
                    } catch (IOException | SpotifyWebApiException e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            });

            updateCredentialsThread.start();
        } else {
            throw new IllegalStateException("Authorization failed.");
        }
    }

    public Track getCurrentlyPlayingTrack() throws SpotifyWebApiException, IOException {
        return api.getUsersCurrentlyPlayingTrack()
            .build()
            .execute()
            .getItem();
    }
}