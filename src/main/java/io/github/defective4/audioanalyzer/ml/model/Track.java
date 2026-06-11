package io.github.defective4.audioanalyzer.ml.model;

import java.util.Map;
import java.util.Map.Entry;

public record Track(String id, String name, String mood, String instrument, String genre, Map<String, Float> scores,
        int bpm) {

    public int getVocality() {
        return (int) (scores.getOrDefault("voice_instrumental_discogs_effnet_1", 0f) * 100f);
    }

    public double calculateSimilarity(Track track2, boolean tempo) {
        double sum = 0;
        for (Entry<String, Float> entry : scores().entrySet()) {
            float diff = entry.getValue() - track2.scores().get(entry.getKey());
            sum += diff * diff;
        }
        if (tempo) sum += (bpm() - track2.bpm()) / 200f;
        return Math.sqrt(sum);
    }
}
