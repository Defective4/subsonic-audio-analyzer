package io.github.defective4.audioanalyzer;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.defective4.audioanalyzer.ml.Database;
import io.github.defective4.audioanalyzer.ml.TensorflowAnalyzer;
import io.github.defective4.audioanalyzer.subsonic.SubsonicAPI;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;

public class CLI {

    private static final String DEFAULT_ESSENTIA = "http://127.0.0.1:8000/analyze";
    private static final String DEFAULT_JDBC = "jdbc:sqlite:./mood.sqlite";

    private static final Options options = new Options()
            .addOption(Option.builder("h").desc("Display this help section").longOpt("help").build())
            .addOption(Option.builder("a").desc("(re)analyze music database").longOpt("analyze").build())
            .addOption(Option.builder("j").desc("JDBC URL for the database (default " + DEFAULT_JDBC + ")")
                    .longOpt("jdbc").numberOfArgs(1).argName("url").build())
            .addOption(Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                    .argName("username").required().build())
            .addOption(Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                    .argName("pass").required().build())
            .addOption(Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url").numberOfArgs(1)
                    .argName("url").required().build())
            .addOption(Option.builder("t").desc("Essentia analyzer endpoint (Default " + DEFAULT_ESSENTIA + ")")
                    .numberOfArgs(1).argName("endpoint url").build());

    private final TensorflowAnalyzer analyzer;
    private final SubsonicAPI api;
    private final Database db;
    private final Logger logger = LoggerFactory.getLogger(CLI.class);

    public CLI(String jdbcURL, String username, char[] password, String url, String analyzerURL)
            throws SQLException, MalformedURLException {
        db = new Database(jdbcURL);
        api = new SubsonicAPI(username, password, url);
        analyzer = new TensorflowAnalyzer(analyzerURL);
    }

    private void index() throws Exception {
        try {
            logger.info("Checking credentials...");
            api.ping();
            logger.info("Logged in successfully!");
            logger.info("Downloading track lists...");
            List<Entity> songs = api.getAllMusic(logger);
            logger.info("Downloaded information about %s songs".formatted(songs.size()));
            logger.info("Starting analysis...");

            Path tmpDir = Files.createTempDirectory("e-analysis-");
            tmpDir.toFile().deleteOnExit();

            for (int i = 0; i < songs.size(); i++) {
                Entity song = songs.get(i);
                System.err.println();
                logger.info("Starting analysis of %s (%s) [%s/%s]...".formatted(song.title(), song.id(), i + 1,
                        songs.size()));
                logger.info("Downloading %s...".formatted(song.id()));
                Path target = Path.of(tmpDir.toString(), Path.of(song.path()).getFileName().toString());
                target.toFile().deleteOnExit();
                try {
                    try (InputStream in = api.download(song)) {
                        Files.copy(in, target);
                    }
                    logger.info("Analyzing %s...".formatted(song.id()));
                    Map<String, Float> response = analyzer.requestAnalysis(target.toString());
                    logger.info("Storing results in database...");
                    db.insertData(song, response);
                } finally {
                    target.toFile().delete();
                }
            }
        } catch (Exception e) {
            logger.error("An error occured: " + e.getMessage());
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        CommandLine cli;
        try {
            cli = DefaultParser.builder().build().parse(options, args);

            String jdbc = cli.hasOption('j') ? cli.getOptionValue('j') : DEFAULT_JDBC;
            String user = cli.getOptionValue('u');
            String password = cli.getOptionValue('p');
            String subsonicURL = cli.getOptionValue('s');
            if (!subsonicURL.endsWith("/")) subsonicURL = subsonicURL + "/";
            String essentiaURL = cli.hasOption('t') ? cli.getOptionValue('t') : DEFAULT_ESSENTIA;

            CLI prog = new CLI(jdbc, user, password.toCharArray(), subsonicURL, essentiaURL);
            if (cli.hasOption('a')) {
                prog.index();
                return;
            }
            System.err.println("One of --analyze is required");
        } catch (MissingArgumentException | MissingOptionException e) {
            System.err.println(e.getMessage());
        }

        new HelpFormatter().printHelp(
                Path.of(CLI.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getFileName()
                        + " [options]",
                options);
    }
}
