package io.github.defective4.audioanalyzer.ml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;

import io.github.defective4.audioanalyzer.ml.model.ModelMetadata;

public class ModelLoader {
    private static final Map<String, String> AVAILABLE_MODELS = Map.of("genres",
            "genre_discogs400-discogs-effnet-1.json", "instruments", "mtg_jamendo_instrument-discogs-effnet-1.json",
            "moods", "mtg_jamendo_moodtheme-discogs-effnet-1.json");
    private final Gson gson = new Gson();
    private final Map<String, ModelMetadata> loadedModels = new HashMap<>();

    public ModelLoader(Path modelsPath) throws IOException {
        for (Entry<String, String> entry : AVAILABLE_MODELS.entrySet()) {
            try (Reader reader = new InputStreamReader(
                    Files.newInputStream(Path.of(modelsPath.toString(), entry.getValue())),
                    StandardCharsets.UTF_8)) {
                loadedModels.put(entry.getKey(), gson.fromJson(reader, ModelMetadata.class));
            }
        }
    }

    public Map<String, ModelMetadata> getLoadedModels() {
        return Collections.unmodifiableMap(loadedModels);
    }

}
