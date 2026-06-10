package io.github.defective4.audioanalyzer.subsonic.model;

import com.google.gson.annotations.SerializedName;

public record Playlist(String id, String name, Entity[] entry, @SerializedName("public") boolean isPublic) {
}
