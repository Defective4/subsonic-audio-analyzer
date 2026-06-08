package io.github.defective4.audioanalyzer.ml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.defective4.audioanalyzer.subsonic.model.Entity;

public class Database {
    private final Connection con;

    public Database(String jdbcURL) throws SQLException {
        con = DriverManager.getConnection(jdbcURL);
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS "moods" (
                    	"trackId"	TEXT NOT NULL,
                    	"trackName" TEXT NOT NULL,
                    	PRIMARY KEY("trackId")
                    )""");
        }
    }

    public void insertData(Entity track, Map<String, Float> values) throws SQLException {
        List<String> columns = getColumns();
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!columns.contains(key)) {
                insertColumn(key);
            }
        }

        List<Map.Entry<String, Float>> valList = new ArrayList<>(values.entrySet());

        try (PreparedStatement st = con
                .prepareStatement("insert or replace into `moods` (trackId, trackName, %s) values (?, ?, %s)".formatted(
                        String.join(", ", valList.stream().map(e -> e.getKey()).toArray(String[]::new)),
                        String.join(", ",
                                valList.stream().map(e -> String.valueOf(e.getValue())).toArray(String[]::new))))) {
            st.setString(1, track.id());
            st.setString(2, track.title());
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
            st.execute("ALTER TABLE moods ADD %s REAL DEFAULT (0) NOT NULL;".formatted(columnName));
        }
    }
}
