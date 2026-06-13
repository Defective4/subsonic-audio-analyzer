package io.github.defective4.audioanalyzer.app;

import static io.github.defective4.audioanalyzer.app.option.ProgramOptions.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.audioanalyzer.expr.NumericExpression;
import io.github.defective4.audioanalyzer.format.PrintFormat;

public class CLI {

    private interface CLIConsumer {
        boolean consume(CommandLine cli, App prog) throws Exception;

        String desc();

        Options ops();
    }

    private static final Map<String, CLIConsumer> COMMANDS = map("analyze", new CLIConsumer() {

        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            boolean notAnalyzeAll = cli.hasOption(AN_ALL_OPTION);
            String filterArtist = cli.getOptionValue(FILTER_ARTIST_OPTION);
            String filterAlbumArtist = cli.getOptionValue(AN_FILTER_ALBUM_ARTIST_OPTION);
            prog.analyze(!notAnalyzeAll, filterArtist, filterAlbumArtist);
            return true;
        }

        @Override
        public String desc() {
            return "(Re)analyze tracks from a remote Subsonic instance";
        }

        @Override
        public Options ops() {
            return ANALYSIS_OPTIONS;
        }
    }, "generate-playlist", new CLIConsumer() {
        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            String song = getOptionValue(cli, GEN_SIMILAR_SONG_OPTION, null);
            String mood = getOptionValue(cli, GEN_MOOD_FILTER_OPTION, null);
            String instrument = getOptionValue(cli, GEN_INSTRUMENT_FILTER_OPTION, null);
            String genre = getOptionValue(cli, GEN_GENRE_FILTER_OPTION, null);
            String playlistName = getOptionValue(cli, GEN_NAME_OPTION);
            String replacePlaylist = getOptionValue(cli, GEN_REPLACE_OPTION, null);
            int limit = getParsedOptionValue(cli, GEN_LIMIT_OPTION, DEFAULT_LIMIT);
            boolean newPublic = hasOption(cli, GEN_PUBLIC_OPTION);
            NumericExpression bpmExpr = getParsedOptionValue(cli, GEN_BPM_FILTER_OPTION, null);
            NumericExpression vocalExpr = getParsedOptionValue(cli, GEN_VOCALITY_FILTER_OPTION, null);

            if (replacePlaylist == null && playlistName == null) {
                System.err.println("Missing playlist name");
                return false;
            }
            boolean similarGenre = hasOption(cli, GEN_SAME_GENRE_OPTION);
            boolean similarMood = hasOption(cli, GEN_SAME_MOOD_OPTION);
            boolean similarInstrument = hasOption(cli, GEN_SAME_INSTRUMENT_OPTION);
            boolean tempo = hasOption(cli, GEN_SIMILAR_INCLUDE_BPM);
            boolean sameArtist = hasOption(cli, GEN_SAME_ARTIST_OPTION);
            String filterPlaylist = getOptionValue(cli, FILTER_ARTIST_OPTION);
            boolean shuffleSimilar = hasOption(cli, GEN_SHUFFLE_SIMILAR_OPTION);
            boolean printJSON = hasOption(cli, GEN_PRINT_JSON_OPTION);
            prog.groupTracks(song, mood, instrument, genre, playlistName, replacePlaylist, limit, newPublic,
                    similarGenre, similarMood, similarInstrument, tempo, bpmExpr, vocalExpr, sameArtist, filterPlaylist,
                    shuffleSimilar, printJSON);
            return true;
        }

        @Override
        public String desc() {
            return "Group tracks into playlists by mood, similarity, etc.";
        }

        @Override
        public Options ops() {
            return GENERATOR_OPTIONS;
        }

    }, "stats", new CLIConsumer() {
        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            prog.printSongs(cli.getParsedOptionValue(ST_PRINT_FORMAT_OPTION, PrintFormat.JSON),
                    cli.getOptionValue(ST_SONG_OPTION), cli.getOptionValue(ST_OUTPUT_OPTION, "-"));
            return true;
        }

        @Override
        public String desc() {
            return "Print statistics about songs in the database";
        }

        @Override
        public Options ops() {
            return STATS_OPTIONS;
        }
    }, "playlist-tools", new CLIConsumer() {

        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            String createNamed = cli.getOptionValue(CREATE_PLAYLIST_OPTION);
            String remove = cli.getOptionValue(DELETE_PLAYLIST_OPTION);
            if (createNamed == null && remove == null) {
                System.err.println("Either --%s or --%s is required".formatted(CREATE_PLAYLIST_OPTION.getLongOpt(),
                        DELETE_PLAYLIST_OPTION.getLongOpt()));
                return false;
            }
            prog.managePlaylist(createNamed, remove);
            return true;
        }

        @Override
        public String desc() {
            return "A set of tools to manage your playlist";
        }

        @Override
        public Options ops() {
            return PLAYLIST_OPTIONS;
        }
    }, "env", new CLIConsumer() {

        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            prog.printEnvironment(hasOption(cli, ENV_UNCENSOR_OPTION));
            return true;
        }

        @Override
        public String desc() {
            return "Print information about available environment variables";
        }

        @Override
        public Options ops() {
            return ENV_OPTIONS;
        }
    });

    public static void main(String[] args) throws Exception {
        String envCmd = System.getenv(App.COMMAND_ENV);
        String cmd = envCmd != null || args.length > 0 ? envCmd != null ? envCmd : args[0] : null;
        if (cmd == null || !COMMANDS.containsKey(cmd)) {
            System.err.println(
                    cmd != null && !COMMANDS.containsKey(cmd) ? "Invalid command " + cmd : "Usage: <command> [args]");
            System.err.println("Valid commands are:");
            COMMANDS.forEach((k, v) -> { System.err.println(" - %s: %s".formatted(k, v.desc())); });
            System.exit(1);
            return;
        }
        Options options = COMMANDS.get(cmd).ops();
        CommandLine cli;

        try {
            cli = DefaultParser.builder().build().parse(options,
                    envCmd == null ? Arrays.copyOfRange(args, 1, args.length) : args);
            if (!cli.hasOption(HELP_OPTION)) {
                String db = getOptionValue(cli, DB_LOCATION_OPTION, DEFAULT_DB);
                String user = getOptionValue(cli, USER_OPTION);
                String password = getOptionValue(cli, PASSWORD_OPTION);
                String subsonicURL = getOptionValue(cli, SUBSONIC_URL_OPTION);
                if (subsonicURL != null && !subsonicURL.endsWith("/")) subsonicURL = subsonicURL + "/";
                String essentiaURL = getOptionValue(cli, AN_TENSORFLOW_OPTION, DEFAULT_ESSENTIA);
                if (!essentiaURL.endsWith("/")) essentiaURL = essentiaURL + "/";

                App prog = new App(db, user, password == null ? new char[0] : password.toCharArray(), subsonicURL,
                        essentiaURL);
                if (COMMANDS.get(cmd).consume(cli, prog)) return;
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }

        HelpFormatter hf = new HelpFormatter();
        hf.setWidth((int) (HelpFormatter.DEFAULT_WIDTH * 1.5));
        hf.printHelp("%s %s [options]".formatted(
                Path.of(CLI.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getFileName(),
                args[0]), options);
        System.exit(2);
    }

    private static <K, V> Map<K, V> map(Object... kv) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((K) kv[i], (V) kv[i + 1]);
        return Collections.unmodifiableMap(map);
    }
}
