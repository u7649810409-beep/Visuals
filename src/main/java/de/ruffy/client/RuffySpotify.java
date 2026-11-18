package de.ruffy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Optional Spotify Connect integration for Ruffy Visuals.
 *
 * Uses Spotify's Authorization Code + PKCE flow: no client secret is embedded
 * in the mod. Lyrics are looked up separately through LRCLIB and rendered as
 * ordinary, unsynchronised text in the Minecraft UI.
 */
public final class RuffySpotify {
    public static final String REDIRECT_URI = "http://127.0.0.1:43821/callback";

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String LYRICS_BASE = "https://lrclib.net/api/get";
    private static final String SCOPES = "user-read-currently-playing user-modify-playback-state";
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("ruffyclient-spotify.properties");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final ScheduledExecutorService POLLER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Ruffy Spotify Poller");
                thread.setDaemon(true);
                return thread;
            });

    private static final Properties STORE = new Properties();
    private static volatile String clientId = "";
    private static volatile String accessToken = "";
    private static volatile String refreshToken = "";
    private static volatile long expiresAt;
    private static volatile String state = "";
    private static volatile String verifier = "";
    private static volatile HttpServer callbackServer;

    private static volatile String status = "Not connected";
    private static volatile String trackId = "";
    private static volatile String trackName = "Nothing playing";
    private static volatile String artistName = "";
    private static volatile String albumName = "";
    private static volatile String lyrics = "Connect Spotify to show the current song's lyrics.";
    private static volatile boolean playing;
    private static volatile long progressMs;
    private static volatile long durationMs;
    private static volatile long progressUpdatedNanos;
    private static volatile String authorizationUrl = "";

    private RuffySpotify() {}

    /** Load local credentials and start a low-frequency metadata poller. */
    public static void init() {
        load();
        POLLER.scheduleAtFixedRate(() -> {
            if (refreshToken.isBlank()) return;
            try {
                pollPlayback();
            } catch (Exception exception) {
                status = "Spotify update failed: " + safeMessage(exception);
            }
        }, 1, 4, TimeUnit.SECONDS);
        if (!refreshToken.isBlank()) status = "Spotify session restored";
    }

    public static String getClientId() {
        return clientId;
    }

    public static synchronized void setClientId(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.equals(clientId)) return;
        if (!clientId.isBlank() && !clientId.equals(cleaned)) {
            accessToken = "";
            refreshToken = "";
            expiresAt = 0L;
            trackId = "";
            trackName = "Nothing playing";
            artistName = "";
            albumName = "";
            lyrics = "Reconnect Spotify to load lyrics.";
            playing = false;
            status = "Client ID changed — connect again";
        }
        clientId = cleaned;
        save();
    }

    public static String getStatus() {
        return status;
    }

    public static boolean isConnected() {
        return !refreshToken.isBlank() || !accessToken.isBlank();
    }

    public static String getTrackName() {
        return trackName;
    }

    public static String getArtistName() {
        return artistName;
    }

    public static String getAlbumName() {
        return albumName;
    }

    public static String getLyrics() {
        return lyrics;
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static long getDurationMs() {
        return durationMs;
    }

    public static long getProgressMs() {
        if (!playing || progressUpdatedNanos == 0L) return progressMs;
        long elapsed = Math.max(0L, (System.nanoTime() - progressUpdatedNanos) / 1_000_000L);
        return Math.min(durationMs, progressMs + elapsed);
    }

    public static String getAuthorizationUrl() {
        return authorizationUrl;
    }

    /** Starts the local PKCE callback and opens Spotify's consent page. */
    public static void connect() {
        if (clientId.isBlank()) {
            status = "Enter your Spotify Client ID first";
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                beginAuthorization();
            } catch (Exception exception) {
                status = "Could not start Spotify login: " + safeMessage(exception);
                stopCallbackServer();
            }
        });
    }

    private static void beginAuthorization() throws Exception {
        stopCallbackServer();
        String localState = randomUrlSafe(24);
        String localVerifier = randomUrlSafe(64);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256")
                        .digest(localVerifier.getBytes(StandardCharsets.US_ASCII)));
        state = localState;
        verifier = localVerifier;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 43821), 0);
        server.createContext("/callback", exchange -> handleCallback(exchange, localState, localVerifier));
        server.setExecutor(Runnable::run);
        callbackServer = server;
        server.start();

        authorizationUrl = "https://accounts.spotify.com/authorize"
                + "?client_id=" + encode(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&scope=" + encode(SCOPES)
                + "&code_challenge_method=S256"
                + "&code_challenge=" + encode(challenge)
                + "&state=" + encode(localState);
        status = "Waiting for Spotify authorization…";
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(authorizationUrl));
        } else {
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(authorizationUrl);
            status = "Login link copied to clipboard";
        }
    }

    private static void handleCallback(HttpExchange exchange,
                                       String expectedState,
                                       String codeVerifier) throws IOException {
        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String returnedState = query.getOrDefault("state", "");
            String code = query.get("code");
            String error = query.get("error");

            if (!expectedState.equals(returnedState)) {
                status = "Spotify login rejected: state did not match";
                sendCallbackPage(exchange, 400, "Login rejected. You can close this tab.");
                return;
            }
            if (error != null) {
                status = "Spotify authorization was cancelled";
                sendCallbackPage(exchange, 400, "Spotify authorization was not granted.");
                return;
            }
            if (code == null || code.isBlank()) {
                status = "Spotify callback did not contain an authorization code";
                sendCallbackPage(exchange, 400, "No authorization code was received.");
                return;
            }

            exchangeAuthorizationCode(code, codeVerifier);
            status = "Spotify connected";
            sendCallbackPage(exchange, 200, "Spotify is connected to Ruffy Visuals. You can close this tab.");
        } catch (Exception exception) {
            status = "Spotify login failed: " + safeMessage(exception);
            sendCallbackPage(exchange, 500, "Connection failed. Return to Minecraft for details.");
        } finally {
            stopCallbackServer();
            state = "";
            verifier = "";
        }
    }

    private static void sendCallbackPage(HttpExchange exchange, int code, String message)
            throws IOException {
        byte[] body = ("<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<title>Ruffy Visuals</title></head><body style=\"background:#100b20;color:#f7f2ff;"
                + "font:16px sans-serif;padding:48px\"><h2>Ruffy Visuals</h2><p>"
                + message + "</p></body></html>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void exchangeAuthorizationCode(String code, String codeVerifier) throws Exception {
        String body = form(Map.of(
                "client_id", clientId,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "code_verifier", codeVerifier
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Spotify token request returned HTTP " + response.statusCode());
        }
        acceptToken(JsonParser.parseString(response.body()).getAsJsonObject());
    }

    private static synchronized void refreshAccessToken() throws Exception {
        if (refreshToken.isBlank() || clientId.isBlank()) {
            throw new IOException("Spotify session expired — connect again");
        }
        String body = form(Map.of(
                "client_id", clientId,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Spotify token refresh returned HTTP " + response.statusCode());
        }
        acceptToken(JsonParser.parseString(response.body()).getAsJsonObject());
    }

    private static synchronized void acceptToken(JsonObject token) {
        accessToken = string(token, "access_token", "");
        if (token.has("refresh_token") && !token.get("refresh_token").isJsonNull()) {
            refreshToken = token.get("refresh_token").getAsString();
        }
        long expiresIn = number(token, "expires_in", 3600L);
        expiresAt = System.currentTimeMillis() + Math.max(60L, expiresIn) * 1000L;
        save();
    }

    private static String validAccessToken() throws Exception {
        if (accessToken.isBlank() || System.currentTimeMillis() >= expiresAt - 60_000L) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private static HttpResponse<String> spotifyRequest(String method, String endpoint)
            throws Exception {
        String token = validAccessToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + endpoint))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            refreshAccessToken();
            request = HttpRequest.newBuilder(URI.create(API_BASE + endpoint))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    private static void pollPlayback() throws Exception {
        HttpResponse<String> response = spotifyRequest("GET", "/me/player/currently-playing");
        if (response.statusCode() == 204) {
            playing = false;
            trackName = "Nothing playing";
            artistName = "";
            albumName = "";
            trackId = "";
            progressMs = 0L;
            durationMs = 0L;
            progressUpdatedNanos = 0L;
            lyrics = "Start playing a song in Spotify to load its lyrics.";
            status = "Connected · no active playback";
            return;
        }
        if (response.statusCode() != 200) {
            status = apiError("Spotify playback status", response.statusCode());
            return;
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        playing = root.has("is_playing") && root.get("is_playing").getAsBoolean();
        progressMs = number(root, "progress_ms", 0L);
        progressUpdatedNanos = System.nanoTime();
        JsonObject item = root.has("item") && root.get("item").isJsonObject()
                ? root.getAsJsonObject("item") : null;
        if (item == null) {
            trackName = "Nothing playing";
            artistName = "";
            albumName = "";
            trackId = "";
            lyrics = "Start playing a song in Spotify to load its lyrics.";
            status = "Connected · no active playback";
            return;
        }

        String newId = string(item, "id", "");
        String newTrack = string(item, "name", "Unknown title");
        String newArtist = artists(item);
        String newAlbum = item.has("album") && item.get("album").isJsonObject()
                ? string(item.getAsJsonObject("album"), "name", "") : "";
        durationMs = number(item, "duration_ms", 0L);
        trackName = newTrack;
        artistName = newArtist;
        albumName = newAlbum;
        status = playing ? "Connected · playing" : "Connected · paused";

        if (!newId.isBlank() && !newId.equals(trackId)) {
            trackId = newId;
            fetchLyrics(newTrack, newArtist, durationMs);
        }
    }

    private static String artists(JsonObject item) {
        if (!item.has("artists") || !item.get("artists").isJsonArray()) return "";
        JsonArray artists = item.getAsJsonArray("artists");
        List<String> names = new ArrayList<>();
        for (JsonElement element : artists) {
            if (element.isJsonObject()) {
                String name = string(element.getAsJsonObject(), "name", "");
                if (!name.isBlank()) names.add(name);
            }
        }
        return String.join(", ", names);
    }

    private static void fetchLyrics(String title, String artist, long durationMs) {
        lyrics = "Searching lyrics…";
        try {
            String query = LYRICS_BASE
                    + "?track_name=" + encode(title)
                    + "&artist_name=" + encode(artist);
            if (durationMs > 0L) query += "&duration=" + Math.max(1L, durationMs / 1000L);

            HttpRequest request = HttpRequest.newBuilder(URI.create(query))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "RuffyVisuals/2.1.0 (https://github.com/u7649810409-beep/Visuals)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                lyrics = "No lyrics found for this song on LRCLIB.";
                return;
            }
            if (response.statusCode() != 200) {
                lyrics = "Lyrics service returned HTTP " + response.statusCode() + ".";
                return;
            }

            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            String plain = string(result, "plainLyrics", "");
            if (plain.isBlank()) {
                plain = removeLrcTimestamps(string(result, "syncedLyrics", ""));
            }
            lyrics = plain.isBlank() ? "No readable lyrics are available for this song." : plain.trim();
        } catch (Exception exception) {
            lyrics = "Could not load lyrics: " + safeMessage(exception);
        }
    }

    private static String removeLrcTimestamps(String value) {
        if (value == null || value.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\R")) {
            String clean = line.replaceFirst("^\\[\\d{2}:\\d{2}(?:\\.\\d+)?\\]\\s*", "").trim();
            if (!clean.isBlank()) {
                if (!result.isEmpty()) result.append('\n');
                result.append(clean);
            }
        }
        return result.toString();
    }

    public static void togglePlayback() {
        runPlaybackCommand(() -> {
            HttpResponse<String> response = spotifyRequest("PUT",
                    playing ? "/me/player/pause" : "/me/player/play");
            playbackCommandResult(response, playing ? "Paused" : "Playback resumed");
        });
    }

    public static void nextTrack() {
        runPlaybackCommand(() -> playbackCommandResult(
                spotifyRequest("POST", "/me/player/next"), "Skipped to next track"));
    }

    public static void previousTrack() {
        runPlaybackCommand(() -> playbackCommandResult(
                spotifyRequest("POST", "/me/player/previous"), "Returned to previous track"));
    }

    public static void refreshNow() {
        CompletableFuture.runAsync(() -> {
            try {
                pollPlayback();
            } catch (Exception exception) {
                status = "Spotify update failed: " + safeMessage(exception);
            }
        });
    }

    private static void runPlaybackCommand(ThrowingRunnable command) {
        if (!isConnected()) {
            status = "Connect Spotify first";
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                command.run();
                pollPlayback();
            } catch (Exception exception) {
                status = "Spotify command failed: " + safeMessage(exception);
            }
        });
    }

    private static void playbackCommandResult(HttpResponse<String> response, String success) {
        if (response.statusCode() == 204) {
            status = success;
        } else {
            status = apiError("Spotify playback control", response.statusCode());
        }
    }

    private static String apiError(String action, int code) {
        if (code == 401) return "Spotify session expired — reconnect";
        if (code == 403) return "Playback control may require Spotify Premium and an active device";
        if (code == 429) return "Spotify is rate limiting requests — retry shortly";
        return action + " returned HTTP " + code;
    }

    public static synchronized void disconnect() {
        accessToken = "";
        refreshToken = "";
        expiresAt = 0L;
        trackId = "";
        trackName = "Nothing playing";
        artistName = "";
        albumName = "";
        lyrics = "Connect Spotify to show the current song's lyrics.";
        playing = false;
        progressMs = 0L;
        durationMs = 0L;
        progressUpdatedNanos = 0L;
        status = "Disconnected";
        authorizationUrl = "";
        stopCallbackServer();
        save();
    }

    private static synchronized void load() {
        STORE.clear();
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                STORE.load(reader);
            } catch (IOException exception) {
                status = "Could not read Spotify settings: " + safeMessage(exception);
            }
        }
        clientId = STORE.getProperty("client_id", "");
        accessToken = STORE.getProperty("access_token", "");
        refreshToken = STORE.getProperty("refresh_token", "");
        try {
            expiresAt = Long.parseLong(STORE.getProperty("expires_at", "0"));
        } catch (NumberFormatException ignored) {
            expiresAt = 0L;
        }
    }

    private static synchronized void save() {
        STORE.setProperty("client_id", clientId);
        STORE.setProperty("access_token", accessToken);
        STORE.setProperty("refresh_token", refreshToken);
        STORE.setProperty("expires_at", Long.toString(expiresAt));
        try {
            Files.createDirectories(FILE.getParent());
            try (var writer = Files.newBufferedWriter(FILE)) {
                STORE.store(writer, "Ruffy Visuals Spotify settings — keep this file private");
            }
        } catch (IOException exception) {
            status = "Could not save Spotify settings: " + safeMessage(exception);
        }
    }

    private static void stopCallbackServer() {
        HttpServer server = callbackServer;
        callbackServer = null;
        if (server != null) server.stop(0);
    }

    private static String randomUrlSafe(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String form(Map<String, String> values) {
        List<String> pairs = new ArrayList<>();
        values.forEach((key, value) -> pairs.add(encode(key) + "=" + encode(value)));
        return String.join("&", pairs);
    }

    private static Map<String, String> parseQuery(String raw) {
        java.util.HashMap<String, String> values = new java.util.HashMap<>();
        if (raw == null || raw.isBlank()) return values;
        for (String pair : raw.split("&")) {
            int split = pair.indexOf('=');
            String key = split < 0 ? pair : pair.substring(0, split);
            String value = split < 0 ? "" : pair.substring(split + 1);
            values.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long number(JsonObject object, String key, long fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() > 110 ? message.substring(0, 107) + "..." : message;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
