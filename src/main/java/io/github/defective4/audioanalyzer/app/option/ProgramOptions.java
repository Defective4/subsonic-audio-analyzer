package io.github.defective4.audioanalyzer.app.option;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.audioanalyzer.expr.EnumConverter;
import io.github.defective4.audioanalyzer.expr.IntegerExpressionConverter;
import io.github.defective4.audioanalyzer.format.PrintFormat;

public class ProgramOptions {

    @EnvironmentVariable("ANALYZE_ALL")
    public static final Option AN_ALL_OPTION;
    @EnvironmentVariable("AN_FILTER_ALBUM_ARTIST")
    public static final Option AN_FILTER_ALBUM_ARTIST_OPTION;
    @EnvironmentVariable(value = "TENSORFLOW_URL", sensitive = true)
    public static final Option AN_TENSORFLOW_OPTION;

    public static final Options ANALYSIS_OPTIONS;
    public static final Options COMMON_OPTIONS;
    @EnvironmentVariable("PLAYLIST_CREATE_NAME")
    public static final Option CREATE_PLAYLIST_OPTION;
    @EnvironmentVariable(value = "DB_FILE", sensitive = true)
    public static final Option DB_LOCATION_OPTION;
    public static final String DEFAULT_DB = "./mood.sqlite";
    public static final String DEFAULT_ESSENTIA = "http://127.0.0.1:8000/";
    public static final int DEFAULT_LIMIT = 30;
    @EnvironmentVariable("PLAYLIST_DELETE")
    public static final Option DELETE_PLAYLIST_OPTION;
    public static final Options ENV_OPTIONS;
    @EnvironmentVariable("ENV_UNCENSOR")
    public static final Option ENV_UNCENSOR_OPTION;
    @EnvironmentVariable("FILTER_ARTIST")
    public static final Option FILTER_ARTIST_OPTION;
    @EnvironmentVariable("GEN_BPM_FILTER")
    public static final Option GEN_BPM_FILTER_OPTION;
    @EnvironmentVariable("GEN_GENRE_FILTER")
    public static final Option GEN_GENRE_FILTER_OPTION;
    @EnvironmentVariable("GEN_INSTRUMENT_FILTER")
    public static final Option GEN_INSTRUMENT_FILTER_OPTION;
    @EnvironmentVariable("GEN_LIMIT")
    public static final Option GEN_LIMIT_OPTION;
    @EnvironmentVariable("GEN_MOOD_FILTER")
    public static final Option GEN_MOOD_FILTER_OPTION;
    public static final Option GEN_NAME_OPTION;
    @EnvironmentVariable("GEN_PRINT_JSON")
    public static final Option GEN_PRINT_JSON_OPTION;
    @EnvironmentVariable("GEN_PUBLIC")
    public static final Option GEN_PUBLIC_OPTION;
    @EnvironmentVariable("GEN_REPLACE_PLAYLIST")
    public static final Option GEN_REPLACE_OPTION;
    @EnvironmentVariable("GEN_SAME_ARTIST")
    public static final Option GEN_SAME_ARTIST_OPTION;
    @EnvironmentVariable("GEN_SAME_GENRE")
    public static final Option GEN_SAME_GENRE_OPTION;
    @EnvironmentVariable("GEN_SAME_INSTRUMENT")
    public static final Option GEN_SAME_INSTRUMENT_OPTION;
    @EnvironmentVariable("GEN_SAME_MOOD")
    public static final Option GEN_SAME_MOOD_OPTION;
    @EnvironmentVariable("GEN_SUFFLE_SIMILAR")
    public static final Option GEN_SHUFFLE_SIMILAR_OPTION;
    @EnvironmentVariable("GEN_SAME_INCLUDE_BPM")
    public static final Option GEN_SIMILAR_INCLUDE_BPM;
    @EnvironmentVariable("GEN_SIMILAR_SONG")
    public static final Option GEN_SIMILAR_SONG_OPTION;
    @EnvironmentVariable("GEN_VOCAL_FILTER")
    public static final Option GEN_VOCALITY_FILTER_OPTION;
    public static final Options GENERATOR_OPTIONS;

    public static final Option HELP_OPTION;
    @EnvironmentVariable(value = "SUBSONIC_PASSWORD", sensitive = true)
    public static final Option PASSWORD_OPTION;

    public static final Options PLAYLIST_OPTIONS;

    @EnvironmentVariable("STATS_OUTPUT_FILE")
    public static final Option ST_OUTPUT_OPTION;
    @EnvironmentVariable("STATS_FORMAT")
    public static final Option ST_PRINT_FORMAT_OPTION;

    @EnvironmentVariable("STATS_SONG")
    public static final Option ST_SONG_OPTION;

    public static final Options STATS_OPTIONS;

    @EnvironmentVariable(value = "SUBSONIC_URL", sensitive = true)
    public static final Option SUBSONIC_URL_OPTION;

    @EnvironmentVariable(value = "SUBSONIC_USER", sensitive = true)
    public static final Option USER_OPTION;

    private static final Map<String, Object> ENV_VARIABLES;

    static {
        // Define options
        GEN_PRINT_JSON_OPTION = Option.builder("j").desc("Print data about resultling playlist in JSON format").build();
        DELETE_PLAYLIST_OPTION = Option.builder("r").argName("playlist").numberOfArgs(1).longOpt("delete-playlist")
                .desc("Delete a playlist by its name or ID.").build();
        CREATE_PLAYLIST_OPTION = Option.builder("c").argName("name").numberOfArgs(1).longOpt("create-playlist")
                .desc("Create a new playlist with the given name and print info about it to standard output.").build();
        GEN_SHUFFLE_SIMILAR_OPTION = Option.builder().longOpt("shuffle-similar")
                .desc("If enabled, similar songs will be added to the playlist in a random order.").build();
        GEN_SAME_ARTIST_OPTION = Option.builder().longOpt("same-artist")
                .desc("Filter songs with the same artist as in --similar-song").build();
        FILTER_ARTIST_OPTION = Option.builder().longOpt("artist-filter").numberOfArgs(1).argName("artist")
                .desc("Filter tracks based on artist name. This option is case-insensitive.").build();
        AN_FILTER_ALBUM_ARTIST_OPTION = Option.builder().longOpt("album-artist-filter").numberOfArgs(1)
                .argName("artist")
                .desc("Filter analyzed tracks based on album artist name. This option is case-insensitive.").build();
        ENV_UNCENSOR_OPTION = Option.builder().longOpt("uncensor")
                .desc("Do not censor sensitive environment variables.").build();
        HELP_OPTION = Option.builder("h").desc("Display this help section").longOpt("help").build();
        DB_LOCATION_OPTION = Option.builder("d")
                .desc("SQLite database location (default " + ProgramOptions.DEFAULT_DB + ")").longOpt("db")
                .numberOfArgs(1).argName("file").build();
        GEN_VOCALITY_FILTER_OPTION = Option.builder().longOpt("vocality-filter").numberOfArgs(1).argName("expression")
                .desc("Filter tracks by their vocality." + "Less than 50% is instrumental, more than 50% is vocal."
                        + "(Example expression: \">50\"." + "This filters tracks with more than 50% vocal score.")
                .converter(new IntegerExpressionConverter()).build();

        ST_SONG_OPTION = Option.builder("s").longOpt("song").argName("song").numberOfArgs(1)
                .desc("Get statistics for a particular song. Both song ID and name is supported.").build();
        ST_OUTPUT_OPTION = Option.builder("o").argName("output").numberOfArgs(1)
                .desc("Redirects command output to a file. Use \"-\" for standard output (default).").longOpt("output")
                .build();
        ST_PRINT_FORMAT_OPTION = Option.builder("p")
                .desc("(Required) Statistics print format. Available values are: " + String.join(", ",
                        Arrays.stream(PrintFormat.values()).map(e -> e.name().toLowerCase()).toArray(String[]::new)
                                + "."))
                .longOpt("print-format").argName("format").numberOfArgs(1).required()
                .converter(new EnumConverter<>(PrintFormat.class)).build();
        PASSWORD_OPTION = Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                .argName("pass").required().build();
        USER_OPTION = Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                .argName("username").required().build();
        GEN_BPM_FILTER_OPTION = Option.builder().longOpt("bpm-filter").argName("filter expr.").numberOfArgs(1)
                .desc("Filter songs by their BPM. You can use > and < (Example: >70 for songs with more than 70 BPM)")
                .converter(new IntegerExpressionConverter()).build();
        GEN_SIMILAR_INCLUDE_BPM = Option.builder().longOpt("include-bpm")
                .desc("Include tempo calculations in similar songs analysis.").build();
        GEN_SAME_GENRE_OPTION = Option.builder().longOpt("same-genre").desc(
                "If enabled, and --similar-song is used, only songs with the same genre as the base will be matched.")
                .build();
        GEN_SAME_INSTRUMENT_OPTION = Option.builder().longOpt("same-instrument").desc(
                "If enabled, and --similar-song is used, only songs with the same instrument as the base will be matched.")
                .build();
        GEN_SAME_MOOD_OPTION = Option.builder().longOpt("same-mood").desc(
                "If enabled, and --similar-song is used, only songs with the same mood as the base will be matched.")
                .build();

        AN_TENSORFLOW_OPTION = Option.builder("t").longOpt("tensorflow-url")
                .desc("Essentia analyzer URL (Default " + DEFAULT_ESSENTIA + ")").numberOfArgs(1).argName("url")
                .build();
        AN_ALL_OPTION = Option.builder("a").desc("Analyze all tracks, even if they are present in the database.")
                .longOpt("all").build();

        GEN_GENRE_FILTER_OPTION = Option.builder().longOpt("genre-filter").numberOfArgs(1).argName("genre")
                .desc("Filter songs based on their genre. Pass ?list to list available genres. Regex IS supported.")
                .build();
        GEN_INSTRUMENT_FILTER_OPTION = Option.builder().longOpt("instrument-filter").numberOfArgs(1)
                .argName("instrument")
                .desc("Filter songs based on their primary instrument. Pass ?list to list available instruments.")
                .build();
        GEN_LIMIT_OPTION = Option.builder().argName("n").numberOfArgs(1).longOpt("limit")
                .desc("Limit the playlists to n tracks. (Default " + DEFAULT_LIMIT + ")").converter(Integer::parseInt)
                .build();
        GEN_MOOD_FILTER_OPTION = Option.builder().longOpt("mood-filter").numberOfArgs(1).argName("mood")
                .desc("Filter songs based on their mood. Pass ?list to list available moods.").build();
        GEN_NAME_OPTION = Option.builder().longOpt("playlist-name").numberOfArgs(1).argName("name")
                .desc("Human readable name of the new playlist. Required if --replace-playlist is not used.").build();
        GEN_PUBLIC_OPTION = Option.builder().longOpt("public").desc("If present, the NEW playlist will be public.")
                .build();
        GEN_REPLACE_OPTION = Option.builder().longOpt("replace-playlist").numberOfArgs(1).argName("id").desc(
                "Replace an existing playlist instead of creating a new one. This will remove all songs from an existing playlist in favor of new ones.")
                .build();
        GEN_SIMILAR_SONG_OPTION = Option.builder().longOpt("similar-song").numberOfArgs(1).argName("song")
                .desc("ID or name of a base song to find similar songs to it.").build();
        SUBSONIC_URL_OPTION = Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url")
                .numberOfArgs(1).argName("url").required().build();

        // Define option groups

        COMMON_OPTIONS = new Options().addOption(HELP_OPTION).addOption(DB_LOCATION_OPTION);
        ANALYSIS_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(AN_ALL_OPTION)
                .addOption(AN_TENSORFLOW_OPTION).addOption(ProgramOptions.USER_OPTION)
                .addOption(ProgramOptions.PASSWORD_OPTION).addOption(SUBSONIC_URL_OPTION)
                .addOption(FILTER_ARTIST_OPTION).addOption(AN_FILTER_ALBUM_ARTIST_OPTION);
        GENERATOR_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(GEN_NAME_OPTION)
                .addOption(ProgramOptions.USER_OPTION).addOption(ProgramOptions.PASSWORD_OPTION)
                .addOption(GEN_GENRE_FILTER_OPTION).addOption(GEN_INSTRUMENT_FILTER_OPTION).addOption(GEN_LIMIT_OPTION)
                .addOption(GEN_MOOD_FILTER_OPTION).addOption(GEN_PUBLIC_OPTION).addOption(GEN_REPLACE_OPTION)
                .addOption(GEN_SIMILAR_SONG_OPTION).addOption(GEN_SAME_GENRE_OPTION).addOption(GEN_SAME_MOOD_OPTION)
                .addOption(GEN_SAME_INSTRUMENT_OPTION).addOption(GEN_SIMILAR_INCLUDE_BPM)
                .addOption(GEN_BPM_FILTER_OPTION).addOption(SUBSONIC_URL_OPTION).addOption(GEN_VOCALITY_FILTER_OPTION)
                .addOption(GEN_SAME_ARTIST_OPTION).addOption(FILTER_ARTIST_OPTION)
                .addOption(GEN_SHUFFLE_SIMILAR_OPTION)
                .addOption(GEN_PRINT_JSON_OPTION);
        STATS_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(ST_PRINT_FORMAT_OPTION)
                .addOption(ST_SONG_OPTION).addOption(ST_OUTPUT_OPTION);
        ENV_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(ENV_UNCENSOR_OPTION);
        PLAYLIST_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(ProgramOptions.USER_OPTION)
                .addOption(ProgramOptions.PASSWORD_OPTION).addOption(SUBSONIC_URL_OPTION)
                .addOption(CREATE_PLAYLIST_OPTION).addOption(DELETE_PLAYLIST_OPTION);

        ENV_VARIABLES = getEnvironmentVariables();
    }

    private ProgramOptions() {}

    public static Object getEnvironmentVariable(String key) {
        return ENV_VARIABLES.get(key);
    }

    public static String getOptionValue(CommandLine cli, Option option) {
        return getOptionValue(cli, option, null);
    }

    public static String getOptionValue(CommandLine cli, Option option, String def) {
        return cli.hasOption(option) ? cli.getOptionValue(option, def)
                : getEnvValue(option, def) == null ? null : getEnvValue(option, def).toString();
    }

    public static <T> T getParsedOptionValue(CommandLine cli, Option option, T def) throws ParseException {
        if (cli.hasOption(option)) return cli.getParsedOptionValue(option, def);
        return (T) getEnvValue(option, def);
    }

    public static boolean hasOption(CommandLine cli, Option option) {
        return cli.hasOption(option)
                || Boolean.TRUE.toString().equalsIgnoreCase(getEnvValue(option, Boolean.FALSE.toString()).toString());
    }

    private static Map<String, Object> getEnvironmentVariables() {
        Map<String, Object> vars = new HashMap<>();
        try {
            for (Field field : ProgramOptions.class.getFields())
                if (field.getType() == Option.class && field.isAnnotationPresent(EnvironmentVariable.class)) {
                    Option opt = (Option) field.get(null);
                    String varName = field.getAnnotation(EnvironmentVariable.class).value();
                    String val = System.getenv(varName);
                    if (val == null)
                        vars.put(opt.getLongOpt(), null);
                    else {
                        Object converted;
                        if (opt.getConverter() != null) {
                            try {
                                converted = opt.getConverter().apply(val);
                            } catch (Throwable e) {
                                System.err.println("Invalid environment variable: " + e.getMessage());
                                System.exit(4);
                                return null;
                            }
                        } else {
                            converted = val;
                        }
                        vars.put(opt.getLongOpt(), converted);
                    }
                }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return Collections.unmodifiableMap(vars);
    }

    private static Object getEnvValue(Option opt) {
        return getEnvValue(opt);
    }

    private static Object getEnvValue(Option opt, Object def) {
        Object val = ENV_VARIABLES.get(opt.getLongOpt());
        return val == null ? def : val;

    }
}
