package io.github.defective4.audioanalyzer.ml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import io.github.defective4.audioanalyzer.ml.model.AnalysisResponse;

public class TensorflowAnalyzer {
    private final String analyzerEndpoint;
    private final Gson gson = new Gson();

    public TensorflowAnalyzer(String analyzerEndpoint) throws MalformedURLException {
        URI.create(analyzerEndpoint).toURL();
        this.analyzerEndpoint = analyzerEndpoint;
    }

    public void ping() throws IOException {
        try (Reader reader = new InputStreamReader(URI.create(analyzerEndpoint + "ping").toURL().openStream())) {
            if (!JsonParser.parseReader(reader).getAsJsonObject().get("status").getAsString().equalsIgnoreCase("ok"))
                throw new IOException("Invalid response from the server");
        }
    }

    public AnalysisResponse requestAnalysis(String filePath) throws IOException {
        try (Reader reader = new InputStreamReader(URI
                .create(analyzerEndpoint + "analyze?audioPath=" + URLEncoder.encode(filePath, StandardCharsets.UTF_8))
                .toURL().openStream())) {
            return gson.fromJson(reader, AnalysisResponse.class);
        }
    }
}
