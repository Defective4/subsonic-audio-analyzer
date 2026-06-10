package io.github.defective4.audioanalyzer.subsonic.exception;

import java.io.IOException;

import io.github.defective4.audioanalyzer.subsonic.model.SubsonicError;

public class SubsonicException extends IOException {

    private final SubsonicError error;

    public SubsonicException(String message, SubsonicError error) {
        super(message);
        this.error = error;
    }

    public SubsonicError getError() {
        return error;
    }
}
