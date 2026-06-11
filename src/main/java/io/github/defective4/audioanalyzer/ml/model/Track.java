package io.github.defective4.audioanalyzer.ml.model;

import java.util.Map;

public record Track(String id, String name, String mood, String instrument, String genre, Map<String, Float> scores,
        int bpm) {

    public int getVocality() {
        return (int) (scores.getOrDefault("voice_instrumental_discogs_effnet_1", 0f) * 100f);
    }
}
