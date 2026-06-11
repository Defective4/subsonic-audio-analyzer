package io.github.defective4.audioanalyzer;

import static io.github.defective4.audioanalyzer.ProgramOptions.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.audioanalyzer.expr.IntegerExpression;

public class CLI {

    private interface CLIConsumer {
        boolean consume(CommandLine cli, App prog) throws Exception;

        String desc();

        Options ops();
    }

    private static final Options ANALYSIS_OPTIONS;

    private static final Map<String, CLIConsumer> COMMANDS = Map.of("analyze", new CLIConsumer() {

        @Override
        public boolean consume(CommandLine cli, App prog) throws Exception {
            prog.index(!cli.hasOption(AN_ALL));
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
            String song = cli.getOptionValue(PLS_SIMILAR_SONG_OPTION, () -> null);
            String mood = cli.getOptionValue(PLS_MOOD_FILTER_OPTION, () -> null);
            String instrument = cli.getOptionValue(PLS_INSTRUMENT_FILTER_OPTION, () -> null);
            String genre = cli.getOptionValue(PLS_GENRE_FILTER_OPTION, () -> null);
            String playlistName = cli.getOptionValue(PLS_NAME_OPTION);
            String replacePlaylist = cli.getOptionValue(PLS_REPLACE_OPTION, () -> null);
            int limit = cli.getParsedOptionValue(PLS_LIMIT_OPTION, DEFAULT_LIMIT);
            boolean newPublic = cli.hasOption(PLS_PUBLIC_OPTION);
            IntegerExpression bpmExpr = cli.getParsedOptionValue(PLS_BPM_FILTER, null);

            if (replacePlaylist == null && playlistName == null) {
                System.err.println("Missing playlist name");
                return false;
            }
            boolean similarGenre = cli.hasOption(PLS_SIMILAR_GENRE_OPTION);
            boolean similarMood = cli.hasOption(PLS_SIMILAR_MOOD_OPTION);
            boolean similarInstrument = cli.hasOption(PLS_SIMILAR_INSTRUMENT_OPTION);
            boolean tempo = cli.hasOption(PLS_SIMILAR_INCLUDE_BPM);
            prog.groupTracks(song, mood, instrument, genre, playlistName, replacePlaylist, limit, newPublic,
                    similarGenre, similarMood, similarInstrument, tempo, bpmExpr);
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

    });
    private static final Options COMMON_OPTIONS;
    private static final Options PLAYLIST_OPTIONS;
    static {

        COMMON_OPTIONS = new Options()
                .addOption(Option.builder("h").desc("Display this help section").longOpt("help").build())
                .addOption(
                        Option.builder("d").desc("SQLite database location (default " + ProgramOptions.DEFAULT_DB + ")")
                                .longOpt("db").numberOfArgs(1).argName("file").build())
                .addOption(Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                        .argName("username").required().build())
                .addOption(Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                        .argName("pass").required().build())
                .addOption(Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url").numberOfArgs(1)
                        .argName("url").required().build());
        ANALYSIS_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(AN_ALL).addOption(AN_TENSORFLOW);
        PLAYLIST_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(PLS_NAME_OPTION)
                .addOption(PLS_GENRE_FILTER_OPTION).addOption(PLS_INSTRUMENT_FILTER_OPTION).addOption(PLS_LIMIT_OPTION)
                .addOption(PLS_MOOD_FILTER_OPTION).addOption(PLS_PUBLIC_OPTION).addOption(PLS_REPLACE_OPTION)
                .addOption(PLS_SIMILAR_SONG_OPTION).addOption(PLS_SIMILAR_GENRE_OPTION)
                .addOption(PLS_SIMILAR_MOOD_OPTION).addOption(PLS_SIMILAR_INSTRUMENT_OPTION)
                .addOption(PLS_SIMILAR_INCLUDE_BPM)
                .addOption(PLS_BPM_FILTER);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !COMMANDS.containsKey(args[0])) {
            System.err
                    .println(!COMMANDS.containsKey(args[0]) ? "Invalid command " + args[0] : "Usage: <command> [args]");
            System.err.println("Valid commands are:");
            COMMANDS.forEach((k, v) -> { System.err.println(" - %s: %s".formatted(k, v.desc())); });
            System.exit(1);
            return;
        }
        Options options = COMMANDS.get(args[0]).ops();
        CommandLine cli;
        try {
            cli = DefaultParser.builder().build().parse(options, Arrays.copyOfRange(args, 1, args.length));
            if (!cli.hasOption('h')) {
                String db = cli.getOptionValue('d', DEFAULT_DB);
                String user = cli.getOptionValue('u');
                String password = cli.getOptionValue('p');
                String subsonicURL = cli.getOptionValue('s');
                if (!subsonicURL.endsWith("/")) subsonicURL = subsonicURL + "/";
                String essentiaURL = cli.getOptionValue('t', DEFAULT_ESSENTIA);
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
