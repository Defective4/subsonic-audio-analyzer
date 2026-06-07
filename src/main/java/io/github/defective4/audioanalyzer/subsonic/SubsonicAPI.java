package io.github.defective4.audioanalyzer.subsonic;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.github.defective4.audioanalyzer.subsonic.model.Entity;
import io.github.defective4.audioanalyzer.subsonic.model.SubsonicResponse;

public class SubsonicAPI {
    private static final String CLIENT_ID = "audio-analyzer";
    private static final String VERSION = "1.16.1";
    private final String baseURL;
    private final Gson gson = new GsonBuilder().create();
    private final HexFormat hex = HexFormat.of();
    private final MessageDigest md5;
    private final char[] password;
    private final String username;

    public SubsonicAPI(String username, char[] password, String baseURL) throws MalformedURLException {
        this.username = username;
        this.password = password;
        try {
            md5 = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        URI.create(baseURL).toURL();
        this.baseURL = baseURL + "/rest/";
    }

    public SubsonicResponse getAlbumList(int limit, int offset) throws IOException {
        return request("getAlbumList", Map.of("type", "newest", "size", limit, "offset", offset));
    }

    public List<Entity> getAllAlbums() throws IOException {
        List<Entity> albums = new ArrayList<>();
        int offset = 0;
        while (true) {
            Entity[] as = getAlbumList(500, offset).albumList().album();
            Collections.addAll(albums, as);
            if (as.length == 500)
                offset += 500;
            else
                break;
        }
        return Collections.unmodifiableList(albums);
    }

    public List<Entity> getAllMusic() throws IOException {
        List<Entity> songs = new ArrayList<>();
        for (Entity album : getAllAlbums())
            Collections.addAll(songs, getMusicDirectory(album.id()).directory().child());
        return Collections.unmodifiableList(songs);
    }

    public SubsonicResponse getMusicDirectory(String id) throws IOException {
        return request("getMusicDirectory", Map.of("id", id));
    }

    public void ping() throws IOException {
        request("ping", Map.of());
    }

    private String computeToken(String salt) {
        return hash(new String(password) + salt);
    }

    private String hash(String data) {
        md5.reset();
        return hex.formatHex(md5.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private SubsonicResponse request(String path, Map<String, Object> queryParameters) throws IOException {
        HttpURLConnection con = null;
        try {
            Map<String, Object> params = new HashMap<>();
            String salt = generateSalt();
            params.put("u", username);
            params.put("t", computeToken(salt));
            params.put("s", salt);
            params.put("v", VERSION);
            params.put("c", CLIENT_ID);
            params.put("f", "json");
            params.putAll(queryParameters);
            StringBuilder queryBuilder = new StringBuilder("?");
            params.forEach((k, v) -> {
                queryBuilder
                        .append(String.format("%s=%s&", k, URLEncoder.encode(v.toString(), StandardCharsets.UTF_8)));
            });
            String queryString = queryBuilder.toString();
            con = (HttpURLConnection) URI.create(baseURL + path + queryString.substring(0, queryString.length() - 1))
                    .toURL().openConnection();
            try (Reader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader).getAsJsonObject().get("subsonic-response");
                return gson.fromJson(json, SubsonicResponse.class);
            }
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String generateSalt() {
        return Long.toHexString(System.currentTimeMillis());
    }
}
