package io.github.defective4.audioanalyzer.ml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.defective4.audioanalyzer.ml.model.Track;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;

public class Database {
    private final Connection con;

    public Database(String jdbcURL) throws SQLException {
        con = DriverManager.getConnection(jdbcURL);
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS "moods" (
                    	"trackId"	      TEXT NOT NULL,
                    	"trackName"       TEXT NOT NULL,
                    	"mood"        TEXT NOT NULL,
                    	"instrument"      TEXT NOT NULL,
                    	"genre"       TEXT NOT NULL,
                    	PRIMARY KEY("trackId")
                    )""");
            st.execute("CREATE INDEX IF NOT EXISTS mood_1_IDX ON moods (mood)");
            st.execute("CREATE INDEX IF NOT EXISTS instrument_1_IDX ON moods (instrument)");
            st.execute("CREATE INDEX IF NOT EXISTS genre_1_IDX ON moods (genre)");
        }
    }

    public List<String> getAllSongIDs() throws SQLException {
        List<String> songs = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet set = st.executeQuery("select trackId from `moods`")) {
            while (set.next()) songs.add(set.getString(1));
        }
        return Collections.unmodifiableList(songs);
    }

    public List<Track> getAllTracks() throws SQLException {
        List<Track> tracks = new ArrayList<>();
        List<String> columns = getColumns();
        try (Statement st = con.createStatement(); ResultSet set = st.executeQuery("select * from `moods`")) {
            while (set.next()) {
                tracks.add(trackFromResultSet(set, columns));
            }
        }
        return Collections.unmodifiableList(tracks);
    }

    public Optional<Track> getTrackById(String id) throws SQLException {
        List<String> cols = getColumns();
        try (PreparedStatement st = con
                .prepareStatement("select * from `moods` where `trackId` = ? or `trackName` = ?")) {
            st.setString(1, id);
            st.setString(2, id);
            try (ResultSet set = st.executeQuery()) {
                if (set.next()) {
                    return Optional.of(trackFromResultSet(set, cols));
                }
                return Optional.empty();
            }
        }
    }

    public void insertData(Entity track, Map<String, Float> values, String moodName, String instrumentName,
            String genreName) throws SQLException {
        List<String> columns = getColumns();
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!columns.contains(key)) {
                insertColumn(key);
            }
        }

        List<Map.Entry<String, Float>> valList = new ArrayList<>(values.entrySet());

        try (PreparedStatement st = con.prepareStatement(
                "insert or replace into `moods` (trackId, trackName, mood, instrument, genre, %s) values (?, ?, ?, ?, ?, %s)"
                        .formatted(String.join(", ", valList.stream().map(e -> e.getKey()).toArray(String[]::new)),
                                String.join(", ", valList.stream().map(e -> String.valueOf(e.getValue()))
                                        .toArray(String[]::new))))) {
            int i = 1;
            st.setString(i++, track.id());
            st.setString(i++, track.title());
            st.setString(i++, moodName);
            st.setString(i++, instrumentName);
            st.setString(i++, genreName);
            st.executeUpdate();
        }
    }

    private List<String> getColumns() throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement st = con.createStatement();
                ResultSet set = st.executeQuery("select name from pragma_table_info(\"moods\")")) {
            while (set.next()) {
                columns.add(set.getString(1));
            }
        }
        return Collections.unmodifiableList(columns);
    }

    private void insertColumn(String columnName) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("ALTER TABLE moods ADD %s REAL DEFAULT (0) NOT NULL".formatted(columnName));
            st.execute("CREATE INDEX %s_1_IDX ON moods (%s)".formatted(columnName, columnName));
        }
    }

    private static Track trackFromResultSet(ResultSet set, List<String> cols) throws SQLException {
        int i = 0;
        String id = set.getString(++i);
        String name = set.getString(++i);
        String mood = set.getString(++i);
        String instrument = set.getString(++i);
        String genre = set.getString(++i);
        Map<String, Float> scores = new HashMap<>();
        for (String column : cols) scores.put(column, set.getFloat(column));
        return new Track(id, name, mood, instrument, genre, Collections.unmodifiableMap(scores));
    }
}
