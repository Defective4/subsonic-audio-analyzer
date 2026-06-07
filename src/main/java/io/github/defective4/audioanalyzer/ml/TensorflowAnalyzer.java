package io.github.defective4.audioanalyzer.ml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TensorflowAnalyzer {
    private final String analyzerEndpoint;

    public TensorflowAnalyzer(String analyzerEndpoint) throws MalformedURLException {
        URI.create(analyzerEndpoint).toURL();
        this.analyzerEndpoint = analyzerEndpoint;
    }

    public Map<String, Float> requestAnalysis(String filePath) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) URI
                    .create(analyzerEndpoint + "?audioPath=" + URLEncoder.encode(filePath, StandardCharsets.UTF_8))
                    .toURL().openConnection();
            Map<String, Float> map = new HashMap<>();
            try (Reader reader = new InputStreamReader(con.getInputStream())) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                obj.asMap().forEach((k, v) -> map.put(k, v.getAsFloat()));
            }
            return Collections.unmodifiableMap(map);
        } finally {
            if (con != null) con.disconnect();
        }
    }
}
