package io.github.defective4.audioanalyzer.app;

import static io.github.defective4.audioanalyzer.app.option.ProgramOptions.*;

import java.nio.file.Path;
import java.util.Arrays;
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

    private static final Map<String, CLIConsumer> COMMANDS = Map.of("analyze", new CLIConsumer() {

        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            prog.analyze(!cli.hasOption(AN_ALL));
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
            String song = getOptionValue(cli, PLS_SIMILAR_SONG_OPTION, null);
            String mood = getOptionValue(cli, PLS_MOOD_FILTER_OPTION, null);
            String instrument = getOptionValue(cli, PLS_INSTRUMENT_FILTER_OPTION, null);
            String genre = getOptionValue(cli, PLS_GENRE_FILTER_OPTION, null);
            String playlistName = getOptionValue(cli, PLS_NAME_OPTION);
            String replacePlaylist = getOptionValue(cli, PLS_REPLACE_OPTION, null);
            int limit = getParsedOptionValue(cli, PLS_LIMIT_OPTION, DEFAULT_LIMIT);
            boolean newPublic = hasOption(cli, PLS_PUBLIC_OPTION);
            NumericExpression bpmExpr = getParsedOptionValue(cli, PLS_BPM_FILTER, null);
            NumericExpression vocalExpr = getParsedOptionValue(cli, PLS_VOCALITY_FILTER_OPTION, null);

            if (replacePlaylist == null && playlistName == null) {
                System.err.println("Missing playlist name");
                return false;
            }
            boolean similarGenre = hasOption(cli, PLS_SIMILAR_GENRE_OPTION);
            boolean similarMood = hasOption(cli, PLS_SIMILAR_MOOD_OPTION);
            boolean similarInstrument = hasOption(cli, PLS_SIMILAR_INSTRUMENT_OPTION);
            boolean tempo = hasOption(cli, PLS_SIMILAR_INCLUDE_BPM);
            prog.groupTracks(song, mood, instrument, genre, playlistName, replacePlaylist, limit, newPublic,
                    similarGenre, similarMood, similarInstrument, tempo, bpmExpr, vocalExpr);
            return true;
        }

        @Override
        public String desc() {
            return "Group tracks into playlists by mood, similarity, etc.";
        }

        @Override
        public Options ops() {
            return PLAYLIST_OPTIONS;
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
    });

    public static void main(String[] args) throws Exception {
        String envCmd = System.getenv("A_COMMAND");
        String cmd = envCmd != null || args.length > 0 ? envCmd != null ? envCmd : args[0] : null;
        if (cmd == null || !COMMANDS.containsKey(cmd)) {
            System.err.println(!COMMANDS.containsKey(cmd) ? "Invalid command " + cmd : "Usage: <command> [args]");
            System.err.println("Valid commands are:");
            COMMANDS.forEach((k, v) -> { System.err.println(" - %s: %s".formatted(k, v.desc())); });
            System.exit(1);
            return;
        }
        Options options = COMMANDS.get(cmd).ops();
        CommandLine cli;

        try {
            cli = DefaultParser.builder().build().parse(options, Arrays.copyOfRange(args, 1, args.length));
            if (!cli.hasOption('h')) {
                String db = getOptionValue(cli, 'd', DEFAULT_DB);
                String user = getOptionValue(cli, 'u');
                String password = getOptionValue(cli, 'p');
                String subsonicURL = getOptionValue(cli, 's');
                if (subsonicURL != null && !subsonicURL.endsWith("/")) subsonicURL = subsonicURL + "/";
                String essentiaURL = getOptionValue(cli, 't', DEFAULT_ESSENTIA);
                if (!essentiaURL.endsWith("/")) essentiaURL = essentiaURL + "/";

                App prog = new App(db, user, password.toCharArray(), subsonicURL, essentiaURL);
                if (COMMANDS.get(args[0]).consume(cli, prog)) return;
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
}
