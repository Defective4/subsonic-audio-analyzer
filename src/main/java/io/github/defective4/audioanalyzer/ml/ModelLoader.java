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
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.Gson;

import io.github.defective4.audioanalyzer.app.CLI;
import io.github.defective4.audioanalyzer.ml.model.ModelMetadata;

public class ModelLoader {

    public static final Map<String, String> REQUIRED_MODEL_FILES = CLI.map("discogs-effnet-bs64-1.pb",
            "feature-extractors/discogs-effnet-bs64-1.pb", "other/genre_discogs400-discogs-effnet-1.json",
            "classification-heads/genre_discogs400/genre_discogs400-discogs-effnet-1.json",
            "other/mtg_jamendo_moodtheme-discogs-effnet-1.json",
            "classification-heads/mtg_jamendo_moodtheme/mtg_jamendo_moodtheme-discogs-effnet-1.json",
            "other/mtg_jamendo_instrument-discogs-effnet-1.pb",
            "classification-heads/mtg_jamendo_instrument/mtg_jamendo_instrument-discogs-effnet-1.pb",
            "other/mtg_jamendo_instrument-discogs-effnet-1.json",
            "classification-heads/mtg_jamendo_instrument/mtg_jamendo_instrument-discogs-effnet-1.json",
            "other/genre_discogs400-discogs-effnet-1.pb",
            "classification-heads/genre_discogs400/genre_discogs400-discogs-effnet-1.pb",
            "other/mtg_jamendo_moodtheme-discogs-effnet-1.pb",
            "classification-heads/mtg_jamendo_moodtheme/mtg_jamendo_moodtheme-discogs-effnet-1.pb",
            "mood/mood_relaxed-discogs-effnet-1.pb",
            "classification-heads/mood_relaxed/mood_relaxed-discogs-effnet-1.pb",
            "mood/mood_aggressive-discogs-effnet-1.pb",
            "classification-heads/mood_aggressive/mood_aggressive-discogs-effnet-1.pb",
            "mood/mood_party-discogs-effnet-1.pb", "classification-heads/mood_party/mood_party-discogs-effnet-1.pb",
            "mood/mood_sad-discogs-effnet-1.pb", "classification-heads/mood_sad/mood_sad-discogs-effnet-1.pb",
            "mood/mood_happy-discogs-effnet-1.pb", "classification-heads/mood_happy/mood_happy-discogs-effnet-1.pb",
            "mood/voice_instrumental-discogs-effnet-1.pb",
            "classification-heads/voice_instrumental/voice_instrumental-discogs-effnet-1.pb",
            "mood/danceability-discogs-effnet-1.pb",
            "classification-heads/danceability/danceability-discogs-effnet-1.pb");

    public static final Set<String> REQUIRED_MODELS = Set.of("danceability_discogs_effnet_1",
            "mood_aggressive_discogs_effnet_1", "mood_happy_discogs_effnet_1", "mood_party_discogs_effnet_1",
            "mood_relaxed_discogs_effnet_1", "mood_sad_discogs_effnet_1", "voice_instrumental_discogs_effnet_1");

    private static final Map<String, String> AVAILABLE_MODELS = Map.of("genres",
            "genre_discogs400-discogs-effnet-1.json", "instruments", "mtg_jamendo_instrument-discogs-effnet-1.json",
            "moods", "mtg_jamendo_moodtheme-discogs-effnet-1.json");
    private final Gson gson = new Gson();
    private final Map<String, ModelMetadata> loadedModels = new HashMap<>();
    private final Logger logger;
    private final Path modelsPath;

    public ModelLoader(Path modelsPath, Logger logger) {
        this.modelsPath = modelsPath;
        this.logger = logger;
    }

    public Map<String, ModelMetadata> getLoadedModels() {
        return Collections.unmodifiableMap(loadedModels);
    }

    public Path getModelsPath() {
        return modelsPath;
    }

    public void loadModels() throws IOException {
        for (Entry<String, String> entry : AVAILABLE_MODELS.entrySet()) {
            try (Reader reader = new InputStreamReader(
                    Files.newInputStream(Path.of(modelsPath.toString() + "/other", entry.getValue())),
                    StandardCharsets.UTF_8)) {
                loadedModels.put(entry.getKey(), gson.fromJson(reader, ModelMetadata.class));
            }
        }
        logger.info("Loaded %s models with %s classes".formatted(getLoadedModels().size(),
                getLoadedModels().values().stream().mapToInt(data -> data.classes().length).sum()));
    }

}
