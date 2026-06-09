package io.github.defective4.audioanalyzer.ml.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

public record AnalysisResponse(JsonObject scores, float[] instruments, float[] genres, float[] moods) {

    public Map<String, Float> scoreMap() {
        Map<String, Float> scores = new HashMap<>();
        this.scores.asMap().forEach((k, v) -> {
            String key = k.replace("-", "_").replace(".pb", "");
            scores.put(key, v.getAsFloat());
        });
        return Collections.unmodifiableMap(scores);
    }

    public int instrument() {
        return max(instruments);
    }

    public int genre() {
        return max(genres);
    }

    public int mood() {
        return max(moods);
    }

    private static int max(float[] vals) {
        float prev = -1;
        int idx = 0;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] > prev) {
                prev = vals[i];
                idx = i;
            }
        }
        return idx;
    }
}
