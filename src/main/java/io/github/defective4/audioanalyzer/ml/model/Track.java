package io.github.defective4.audioanalyzer.ml.model;

import java.util.Map;

public record Track(String id, String name, String mood, String instrument, String genre, Map<String, Float> scores) {
}
