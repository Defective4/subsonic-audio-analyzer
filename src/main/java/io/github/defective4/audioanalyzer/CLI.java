package io.github.defective4.audioanalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

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
import io.github.defective4.audioanalyzer.ml.ModelLoader;
import io.github.defective4.audioanalyzer.ml.TensorflowAnalyzer;
import io.github.defective4.audioanalyzer.ml.model.AnalysisResponse;
import io.github.defective4.audioanalyzer.ml.model.ModelMetadata;
import io.github.defective4.audioanalyzer.ml.model.Track;
import io.github.defective4.audioanalyzer.subsonic.SubsonicAPI;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;

public class CLI {

    private static final String DEFAULT_ESSENTIA = "http://127.0.0.1:8000/";
    private static final String DEFAULT_JDBC = "jdbc:sqlite:./mood.sqlite";
    private static final int DEFAULT_LIMIT = 30;

    private static final Options options = new Options()
            .addOption(Option.builder("h").desc("Display this help section").longOpt("help").build())
            .addOption(Option.builder("a").desc("(re)analyze music database").longOpt("analyze").build())
            .addOption(Option.builder("n").desc("Analyze all tracks, even if they are present in the database")
                    .longOpt("only-new").build())
            .addOption(Option.builder("j").desc("JDBC URL for the database (default " + DEFAULT_JDBC + ")")
                    .longOpt("jdbc").numberOfArgs(1).argName("url").build())
            .addOption(Option.builder("u").desc("Subsonic username (Required)").longOpt("user").numberOfArgs(1)
                    .argName("username").required().build())
            .addOption(Option.builder("p").desc("Subsonic password (Required)").longOpt("password").numberOfArgs(1)
                    .argName("pass").required().build())
            .addOption(Option.builder("s").desc("Subsonic instance URL (Required)").longOpt("url").numberOfArgs(1)
                    .argName("url").required().build())
            .addOption(Option.builder("t").desc("Essentia analyzer URL (Default " + DEFAULT_ESSENTIA + ")")
                    .numberOfArgs(1).argName("url").build())

            .addOption(Option.builder("g").desc("Generate a playlist").longOpt("playlist-generate").build());
//            .addOption(Option.builder().longOpt("playlist-name").numberOfArgs(1).argName("name")
//                    .desc("A name of playlist to generate").build())
//            .addOption(Option.builder().longOpt("playlist-limit").numberOfArgs(1).argName("limit")
//                    .desc("Limit of songs to be added to the playlist. (Default " + DEFAULT_LIMIT + ")").build())
//            .addOption(Option.builder().longOpt("playlist-similar").numberOfArgs(1).argName("track ID or name")
//                    .desc("Group tracks based on similarity to the provided track").build());

    private final String analyzerURL;
    private final SubsonicAPI api;
    private final Database db;
    private final Logger logger = LoggerFactory.getLogger(CLI.class);

    public CLI(String jdbcURL, String username, char[] password, String url, String analyzerURL)
            throws SQLException, IOException {
        this.analyzerURL = analyzerURL;
        db = new Database(jdbcURL);
        api = new SubsonicAPI(username, password, url);
    }

    private void checkAPI() throws IOException {
        logger.info("Checking credentials...");
        api.ping();
        logger.info("Logged in successfully!");
    }

    private TensorflowAnalyzer getTensorflow() throws MalformedURLException, IOException {
        logger.info("Pinging analyzer server...");
        TensorflowAnalyzer analyzer = new TensorflowAnalyzer(analyzerURL);
        analyzer.ping();
        return analyzer;
    }

    private void groupTracks() throws SQLException {
//        checkAPI();
        int limit = 30;
        String id = "エキストラ·ヒーロー";
        Optional<Track> baseOp = Optional.empty();
        baseOp = db.getTrackById(id);
        if (!baseOp.isPresent()) {
            logger.error("Track with id or name %s does not exist.");
            return;
        }

        Track base = baseOp.get();

        List<Track> similar = db.getAllTracks().stream().sorted((t1, t2) -> {
            double diff = calculateSimilarity(t1, base) - calculateSimilarity(t2, base);
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }).filter(track -> !track.id().equals(base.id())).limit(limit).toList();

        System.out.println(similar.stream().map(Track::name).toList());
    }

    private void index(boolean onlyNew) throws Exception {
        try {
            TensorflowAnalyzer analyzer = getTensorflow();
            logger.info("Analyzer server OK");
            ModelLoader modelLoader = new ModelLoader(Path.of("./models/other"), logger);
            Map<String, ModelMetadata> models = modelLoader.getLoadedModels();
            checkAPI();
            logger.info("Downloading track lists...");
            List<Entity> songs = api.getAllMusic(logger);
            logger.info("Downloaded information about %s songs".formatted(songs.size()));
            logger.info("Starting analysis...");

            List<String> ignore = onlyNew ? db.getAllSongIDs() : List.of();

            Path tmpDir = Files.createTempDirectory("e-analysis-");
            tmpDir.toFile().deleteOnExit();

            for (int i = 0; i < songs.size(); i++) {
                Entity song = songs.get(i);
                if (ignore.contains(song.id())) {
                    logger.info("%s is already in database, skipped.".formatted(song.title()));
                    continue;
                }
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
                    AnalysisResponse response = analyzer.requestAnalysis(target.toString());
                    logger.info("Storing results in database...");
                    logger.info("Results for %s:".formatted(song.title()));
                    String moodName = models.get("moods").classes()[response.mood()];
                    String instrumentName = models.get("instruments").classes()[response.instrument()];
                    String genreName = models.get("genres").classes()[response.genre()];

                    logger.info(" Mood: %s".formatted(moodName));
                    logger.info(" Instrument: %s".formatted(instrumentName));
                    logger.info(" Genre: %s".formatted(genreName));
                    System.err.println();
                    db.insertData(song, response.scoreMap(), moodName, instrumentName, genreName);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
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
            if (!essentiaURL.endsWith("/")) essentiaURL = essentiaURL + "/";

            CLI prog = new CLI(jdbc, user, password.toCharArray(), subsonicURL, essentiaURL);
            if (cli.hasOption('a')) {
                prog.index(!cli.hasOption('n'));
                return;
            } else if (cli.hasOption('g')) {
                prog.groupTracks();
                return;
            }
            System.err.println("One of --analyze, or --playlist-generate is required");
        } catch (MissingArgumentException | MissingOptionException e) {
            System.err.println(e.getMessage());
        }

        new HelpFormatter().printHelp(
                Path.of(CLI.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getFileName()
                        + " [options]",
                options);
    }

    private static double calculateSimilarity(Track track1, Track track2) {
        double sum = 0;
        for (Entry<String, Float> entry : track1.scores().entrySet()) {
            float diff = entry.getValue() - track2.scores().get(entry.getKey());
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
