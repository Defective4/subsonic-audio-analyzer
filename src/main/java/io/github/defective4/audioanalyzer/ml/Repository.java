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

public class Repository {
    private static final String ANALYSIS_STATUS_PROPERTY = "analysis_status";
    private final Connection con;

    public Repository(String jdbcURL) throws SQLException {
        con = DriverManager.getConnection(jdbcURL);
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS "moods" (
                    	"trackId"	      TEXT NOT NULL,
                    	"trackName"       TEXT NOT NULL,
                    	"mood"            TEXT NOT NULL,
                    	"instrument"      TEXT NOT NULL,
                    	"genre"           TEXT NOT NULL,
                    	"bpm"             INTEGER NOT NULL,
                    	"failed"          INTEGER NOT NULL DEFAULT false,
                    	"failedReason"    TEXT DEFAULT NULL,
                    	"artist"          TEXT NOT NULL,
                    	PRIMARY KEY("trackId")
                    )""");
            st.execute("CREATE INDEX IF NOT EXISTS mood_1_IDX ON moods (mood)");
            st.execute("CREATE INDEX IF NOT EXISTS instrument_1_IDX ON moods (instrument)");
            st.execute("CREATE INDEX IF NOT EXISTS genre_1_IDX ON moods (genre)");
            st.execute("CREATE INDEX IF NOT EXISTS bpm_1_IDX ON moods (bpm)");
            st.execute("CREATE INDEX IF NOT EXISTS artist_1_IDX ON moods (artist)");
            st.execute("CREATE INDEX IF NOT EXISTS failed_1_IDX ON moods (failed)");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS "properties" (
                    	property TEXT NOT NULL,
                    	value TEXT NOT NULL,
                    	CONSTRAINT NewTable_PK PRIMARY KEY (property)
                    ); """);
            st.execute("insert or ignore into `properties` (property, value) values (\"%s\", \"%s\")"
                    .formatted(ANALYSIS_STATUS_PROPERTY, AnalysisState.UNANALYZED.name()));
        }
    }

    public List<String> getAllSongIDs() throws SQLException {
        List<String> songs = new ArrayList<>();
        try (Statement st = con.createStatement();
                ResultSet set = st.executeQuery("select trackId from `moods` where failed = false")) {
            while (set.next()) songs.add(set.getString(1));
        }
        return Collections.unmodifiableList(songs);
    }

    public List<Track> getAllTracks(boolean ignoreFailed) throws SQLException {
        List<Track> tracks = new ArrayList<>();
        List<String> columns = getColumns();
        try (Statement st = con.createStatement();
                ResultSet set = st.executeQuery(
                        ignoreFailed ? "select * from `moods` where failed = false" : "select * from `moods`")) {
            while (set.next()) {
                tracks.add(trackFromResultSet(set, columns));
            }
        }
        return Collections.unmodifiableList(tracks);
    }

    public AnalysisState getAnalysisState() throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet set = st.executeQuery(
                        "select value from `properties` where property = \"%s\"".formatted(ANALYSIS_STATUS_PROPERTY))) {
            set.next();
            return AnalysisState.valueOf(set.getString(1));
        }
    }

    public Optional<Track> getTrackByIdOrName(String idOrName) throws SQLException {
        List<String> cols = getColumns();
        try (PreparedStatement st = con.prepareStatement(
                "select * from `moods` where (`trackId` = ? or `trackName` = ?) and `failed` = false")) {
            st.setString(1, idOrName);
            st.setString(2, idOrName);
            try (ResultSet set = st.executeQuery()) {
                if (set.next()) {
                    Optional<Track> of = Optional.of(trackFromResultSet(set, cols));
                    if (set.next())
                        throw new IllegalStateException("There is more than one track with name " + idOrName);
                    return of;
                }
                return Optional.empty();
            }
        }
    }

    public void insertData(Entity song, Map<String, Float> values, String moodName, String instrumentName,
            String genreName, float bpm, Exception failed) throws SQLException {
        List<String> columns = getColumns();
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!columns.contains(key)) {
                insertColumn(key);
            }
        }

        List<Map.Entry<String, Float>> valList = new ArrayList<>(values.entrySet());

        try (PreparedStatement st = con.prepareStatement(
                "insert or replace into `moods` (trackId, trackName, mood, instrument, genre, bpm, failed, failedReason, artist, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, %s)"
                        .formatted(String.join(", ", valList.stream().map(e -> e.getKey()).toArray(String[]::new)),
                                String.join(", ", valList.stream().map(e -> String.valueOf(e.getValue()))
                                        .toArray(String[]::new))))) {
            int i = 1;
            st.setString(i++, song.id());
            st.setString(i++, song.title());
            st.setString(i++, moodName);
            st.setString(i++, instrumentName);
            st.setString(i++, genreName);
            st.setInt(i++, (int) bpm);
            st.setBoolean(i++, failed != null);
            st.setString(i++, failed == null ? null : failed.getMessage());
            st.setString(i++, song.artist());
            st.executeUpdate();
        }
    }

    public void setAnalysisState(AnalysisState state) throws SQLException {
        try (PreparedStatement st = con.prepareStatement("update `properties` set `value` = ? where `property` = ?")) {
            st.setString(1, state.name());
            st.setString(2, ANALYSIS_STATUS_PROPERTY);
            st.execute();
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
        int bpm = set.getInt(++i);
        boolean failed = set.getBoolean(++i);
        String failedReason = set.getString(++i);
        String artist = set.getString(++i);
        Map<String, Float> scores = new HashMap<>();
        for (String column : cols) scores.put(column, set.getFloat(column));
        return new Track(id, name, mood, instrument, genre, Collections.unmodifiableMap(scores), bpm, failed,
                failedReason, artist);
    }
}
