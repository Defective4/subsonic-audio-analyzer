package io.github.defective4.audioanalyzer;

import org.apache.commons.cli.Option;

public class ProgramOptions {

    public static final Option PLS_GENRE_FILTER_OPTION = Option.builder().longOpt("genre-filter").numberOfArgs(1)
            .argName("genre").desc("Filter songs based on their genre. Pass ?list to list available genres").build();
    public static final Option PLS_INSTRUMENT_FILTER_OPTION = Option.builder().longOpt("instrument-filter")
            .numberOfArgs(1).argName("instrument")
            .desc("Filter songs based on their primary instrument. Pass ?list to list available instruments").build();
    public static final Option PLS_LIMIT_OPTION = Option.builder().argName("n").numberOfArgs(1).longOpt("limit")
            .desc("Limit the playlists to n tracks").converter(Integer::parseInt).build();
    public static final Option PLS_MOOD_FILTER_OPTION = Option.builder().longOpt("mood-filter").numberOfArgs(1)
            .argName("mood").desc("Filter songs based on their mood. Pass ?list to list available moods").build();
    public static final Option PLS_NAME_OPTION = Option.builder().longOpt("playlist-name").numberOfArgs(1)
            .argName("name").desc("Human readable name of the new playlist. Required if --replace-playlist is not used")
            .build();
    public static final Option PLS_PUBLIC_OPTION = Option.builder().argName("true|false").numberOfArgs(1)
            .longOpt("public").desc("If true, the NEW playlist will be public").converter(Boolean::parseBoolean)
            .build();
    public static final Option PLS_REPLACE_OPTION = Option.builder().longOpt("replace-playlist").numberOfArgs(1)
            .argName("id")
            .desc("Replace an existing playlist instead of creating a new one. This will remove all songs from an existing playlist in favor of new ones.")
            .build();
    public static final Option PLS_SIMILAR_SONG_OPTION = Option.builder().longOpt("similar-song").numberOfArgs(1)
            .argName("song").desc("ID or name of a base song to find similar songs to it").build();

    private ProgramOptions() {}
}
