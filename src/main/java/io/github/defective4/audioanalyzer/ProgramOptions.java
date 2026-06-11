package io.github.defective4.audioanalyzer;

import org.apache.commons.cli.Option;

import io.github.defective4.audioanalyzer.expr.IntegerExpressionConverter;

public class ProgramOptions {

    public static final Option AN_ALL;
    public static final Option AN_TENSORFLOW;
    public static final String DEFAULT_DB = "./mood.sqlite";

    public static final String DEFAULT_ESSENTIA = "http://127.0.0.1:8000/";
    public static final int DEFAULT_LIMIT = 30;
    public static final Option PASSWORD_OPTION;
    public static final Option PLS_BPM_FILTER;
    public static final Option PLS_GENRE_FILTER_OPTION;
    public static final Option PLS_INSTRUMENT_FILTER_OPTION;
    public static final Option PLS_LIMIT_OPTION;
    public static final Option PLS_MOOD_FILTER_OPTION;
    public static final Option PLS_NAME_OPTION;
    public static final Option PLS_PUBLIC_OPTION;
    public static final Option PLS_REPLACE_OPTION;
    public static final Option PLS_SIMILAR_GENRE_OPTION;
    public static final Option PLS_SIMILAR_INCLUDE_BPM;
    public static final Option PLS_SIMILAR_INSTRUMENT_OPTION;
    public static final Option PLS_SIMILAR_MOOD_OPTION;
    public static final Option PLS_SIMILAR_SONG_OPTION;
    public static final Option SUBSONIC_URL;
    public static final Option USER_OPTION;

    static {
        PASSWORD_OPTION = Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                .argName("pass").required().build();
        USER_OPTION = Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                .argName("username").required().build();
        PLS_BPM_FILTER = Option.builder().longOpt("bpm-filter").argName("filter expr.").numberOfArgs(1)
                .desc("Filter songs by their BPM. You can use > and < (Example: >70 for songs with more than 70 BPM)")
                .converter(new IntegerExpressionConverter()).build();
        PLS_SIMILAR_INCLUDE_BPM = Option.builder().longOpt("include-bpm")
                .desc("Include tempo calculations in similar songs analysis.").build();
        PLS_SIMILAR_GENRE_OPTION = Option.builder().longOpt("same-genre").desc(
                "If enabled, and --similar-song is used, only songs with the same genre as the base will be matched.")
                .build();
        PLS_SIMILAR_INSTRUMENT_OPTION = Option.builder().longOpt("same-instrument").desc(
                "If enabled, and --similar-song is used, only songs with the same instrument as the base will be matched.")
                .build();
        PLS_SIMILAR_MOOD_OPTION = Option.builder().longOpt("same-mood").desc(
                "If enabled, and --similar-song is used, only songs with the same mood as the base will be matched.")
                .build();

        AN_TENSORFLOW = Option.builder("t").desc("Essentia analyzer URL (Default " + DEFAULT_ESSENTIA + ")")
                .numberOfArgs(1).argName("url").build();
        AN_ALL = Option.builder("a").desc("Analyze all tracks, even if they are present in the database.")
                .longOpt("all").build();

        PLS_GENRE_FILTER_OPTION = Option.builder().longOpt("genre-filter").numberOfArgs(1).argName("genre")
                .desc("Filter songs based on their genre. Pass ?list to list available genres. Regex IS supported.")
                .build();
        PLS_INSTRUMENT_FILTER_OPTION = Option.builder().longOpt("instrument-filter").numberOfArgs(1)
                .argName("instrument")
                .desc("Filter songs based on their primary instrument. Pass ?list to list available instruments.")
                .build();
        PLS_LIMIT_OPTION = Option.builder().argName("n").numberOfArgs(1).longOpt("limit")
                .desc("Limit the playlists to n tracks. (Default " + DEFAULT_LIMIT + ")").converter(Integer::parseInt)
                .build();
        PLS_MOOD_FILTER_OPTION = Option.builder().longOpt("mood-filter").numberOfArgs(1).argName("mood")
                .desc("Filter songs based on their mood. Pass ?list to list available moods.").build();
        PLS_NAME_OPTION = Option.builder().longOpt("playlist-name").numberOfArgs(1).argName("name")
                .desc("Human readable name of the new playlist. Required if --replace-playlist is not used.").build();
        PLS_PUBLIC_OPTION = Option.builder().longOpt("public").desc("If present, the NEW playlist will be public.")
                .build();
        PLS_REPLACE_OPTION = Option.builder().longOpt("replace-playlist").numberOfArgs(1).argName("id").desc(
                "Replace an existing playlist instead of creating a new one. This will remove all songs from an existing playlist in favor of new ones.")
                .build();
        PLS_SIMILAR_SONG_OPTION = Option.builder().longOpt("similar-song").numberOfArgs(1).argName("song")
                .desc("ID or name of a base song to find similar songs to it.").build();
        SUBSONIC_URL = Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url").numberOfArgs(1)
                .argName("url").required().build();
    }

    private ProgramOptions() {}
}
