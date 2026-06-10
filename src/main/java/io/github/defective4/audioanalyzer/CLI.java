package io.github.defective4.audioanalyzer;

import static io.github.defective4.audioanalyzer.ProgramOptions.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLI {

    private interface CLIConsumer {
        void consume(CommandLine cli, App prog) throws Exception;

        String desc();

        Options ops();
    }

    private static final Options ANALYSIS_OPTIONS;

    private static final Map<String, CLIConsumer> COMMANDS = Map.of("analyze", new CLIConsumer() {

        @Override
        public void consume(CommandLine cli, App prog) throws Exception {
            prog.index(!cli.hasOption(AN_ALL));
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
        public void consume(CommandLine cli, App prog) throws Exception {
            String song = cli.getOptionValue(PLS_SIMILAR_SONG_OPTION, () -> null);
            String mood = cli.getOptionValue(PLS_MOOD_FILTER_OPTION, () -> null);
            String instrument = cli.getOptionValue(PLS_INSTRUMENT_FILTER_OPTION, () -> null);
            String genre = cli.getOptionValue(PLS_GENRE_FILTER_OPTION, () -> null);
            String playlistName = cli.getOptionValue(PLS_NAME_OPTION);
            String replacePlaylist = cli.getOptionValue(PLS_REPLACE_OPTION, () -> null);
            int limit = cli.getParsedOptionValue(PLS_LIMIT_OPTION, DEFAULT_LIMIT);
            boolean newPublic = cli.getParsedOptionValue(PLS_PUBLIC_OPTION, true);
            prog.groupTracks(song, mood, instrument, genre, playlistName, replacePlaylist, limit, newPublic);
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
                .addOption(Option.builder("j").desc("JDBC URL for the database (default " + ProgramOptions.DEFAULT_JDBC + ")")
                        .longOpt("jdbc").numberOfArgs(1).argName("url").build())
                .addOption(Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                        .argName("username").required().build())
                .addOption(Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                        .argName("pass").required().build())
                .addOption(Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url").numberOfArgs(1)
                        .argName("url").required().build());
        ANALYSIS_OPTIONS = new Options().addOptions(COMMON_OPTIONS)
                .addOption(AN_ALL)
                .addOption(AN_TENSORFLOW);
        PLAYLIST_OPTIONS = new Options().addOptions(COMMON_OPTIONS).addOption(PLS_NAME_OPTION)
                .addOption(PLS_GENRE_FILTER_OPTION).addOption(PLS_INSTRUMENT_FILTER_OPTION).addOption(PLS_LIMIT_OPTION)
                .addOption(PLS_MOOD_FILTER_OPTION).addOption(PLS_PUBLIC_OPTION).addOption(PLS_REPLACE_OPTION)
                .addOption(PLS_SIMILAR_SONG_OPTION);
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
                String jdbc = cli.getOptionValue('j', DEFAULT_JDBC);
                String user = cli.getOptionValue('u');
                String password = cli.getOptionValue('p');
                String subsonicURL = cli.getOptionValue('s');
                if (!subsonicURL.endsWith("/")) subsonicURL = subsonicURL + "/";
                String essentiaURL = cli.getOptionValue('t', DEFAULT_ESSENTIA);
                if (!essentiaURL.endsWith("/")) essentiaURL = essentiaURL + "/";

                App prog = new App(jdbc, user, password.toCharArray(), subsonicURL, essentiaURL);
                COMMANDS.get(args[0]).consume(cli, prog);
                return;
            }
        } catch (MissingArgumentException | MissingOptionException e) {
            System.err.println(e.getMessage());
        }

        new HelpFormatter().printHelp("%s %s [options]".formatted(
                Path.of(CLI.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getFileName(),
                args[0]), options);
        System.exit(2);
    }
}
