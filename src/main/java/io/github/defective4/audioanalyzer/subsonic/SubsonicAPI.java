package io.github.defective4.audioanalyzer.subsonic;

import java.io.IOException;
import java.io.InputStream;
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

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.github.defective4.audioanalyzer.subsonic.exception.SubsonicException;
import io.github.defective4.audioanalyzer.subsonic.model.AlbumList;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;
import io.github.defective4.audioanalyzer.subsonic.model.Playlist;
import io.github.defective4.audioanalyzer.subsonic.model.SongList;
import io.github.defective4.audioanalyzer.subsonic.model.SubsonicError;
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
        this.baseURL = baseURL + "rest/";
    }

    public Playlist createPlaylist(String name) throws IOException {
        return request("createPlaylist", Map.of("name", name)).playlist();
    }

    public InputStream download(Entity entity) throws IOException {
        return URI.create(baseURL + "download" + constructQueryString(Map.of("id", entity.id()))).toURL().openStream();
    }

    public AlbumList getAlbumList(int limit, int offset) throws IOException {
        return request("getAlbumList", Map.of("type", "newest", "size", limit, "offset", offset)).albumList();
    }

    public List<Entity> getAllAlbums(Logger logger) throws IOException {
        List<Entity> albums = new ArrayList<>();
        int offset = 0;
        int i = 0;
        while (true) {
            Entity[] as = getAlbumList(500, offset).album();
            logger.info("Downloaded chunk %s".formatted(++i));
            Collections.addAll(albums, as);
            if (as.length == 500)
                offset += 500;
            else
                break;
        }
        return Collections.unmodifiableList(albums);
    }

    public List<Entity> getAllMusic(Logger logger) throws IOException {
        List<Entity> songs = new ArrayList<>();
        List<Entity> albums = getAllAlbums(logger);
        int i = 0;
        for (Entity album : albums) {
            if (++i % 100 == 0) logger.info("Downloaded metadata for %s out of %s albums".formatted(i, albums.size()));
            Collections.addAll(songs, getMusicDirectory(album.id()).child());
        }
        return Collections.unmodifiableList(songs);
    }

    public SongList getMusicDirectory(String id) throws IOException {
        return request("getMusicDirectory", Map.of("id", id)).directory();
    }

    public Playlist getPlaylist(String id) throws IOException {
        return request("getPlaylist", Map.of("id", id)).playlist();
    }

    public SubsonicResponse ping() throws IOException {
        return request("ping", Map.of());
    }

    public void updatePlaylist(String id, String songToAdd, int songToRemove, boolean isPublic) throws IOException {
        Map<String, Object> map = new HashMap<>();
        if (songToAdd != null) map.put("songIdToAdd", songToAdd);
        if (songToRemove >= 0) map.put("songIndexToRemove", songToRemove);
        map.put("playlistId", id);
        map.put("public", isPublic);
        request("updatePlaylist", map);
    }

    private String computeToken(String salt) {
        return hash(new String(password) + salt);
    }

    private String constructQueryString(Map<String, Object> queryParameters) {
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
        params.forEach((k, v) -> queryBuilder
                .append(String.format("%s=%s&", k, URLEncoder.encode(v.toString(), StandardCharsets.UTF_8))));
        String queryString = queryBuilder.toString();
        return queryString.substring(0, queryBuilder.length() - 1);
    }

    private String hash(String data) {
        md5.reset();
        return hex.formatHex(md5.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private SubsonicResponse request(String path, Map<String, Object> queryParameters)
            throws IOException, SubsonicException {
        HttpURLConnection con = null;
        try {
            String queryString = constructQueryString(queryParameters);
            con = (HttpURLConnection) URI.create(baseURL + path + queryString).toURL().openConnection();
            try (Reader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader).getAsJsonObject().get("subsonic-response");
                SubsonicResponse response = gson.fromJson(json, SubsonicResponse.class);
                SubsonicError error = response.error();
                if (error != null)
                    throw new SubsonicException("Error %s: %s".formatted(error.code(), error.message()), error);
                return response;
            }
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String generateSalt() {
        return Long.toHexString(System.currentTimeMillis());
    }
}
