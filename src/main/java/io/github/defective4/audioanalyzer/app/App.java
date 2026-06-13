package io.github.defective4.audioanalyzer.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.defective4.audioanalyzer.app.option.EnvironmentVariable;
import io.github.defective4.audioanalyzer.app.option.ProgramOptions;
import io.github.defective4.audioanalyzer.exception.MissingModelsException;
import io.github.defective4.audioanalyzer.exception.SubsonicException;
import io.github.defective4.audioanalyzer.expr.NumericExpression;
import io.github.defective4.audioanalyzer.format.MarkdownTableWriter;
import io.github.defective4.audioanalyzer.format.PrintFormat;
import io.github.defective4.audioanalyzer.ml.AnalysisState;
import io.github.defective4.audioanalyzer.ml.ModelLoader;
import io.github.defective4.audioanalyzer.ml.Repository;
import io.github.defective4.audioanalyzer.ml.TensorflowAnalyzer;
import io.github.defective4.audioanalyzer.ml.model.AnalysisResponse;
import io.github.defective4.audioanalyzer.ml.model.ModelMetadata;
import io.github.defective4.audioanalyzer.ml.model.Track;
import io.github.defective4.audioanalyzer.subsonic.SubsonicAPI;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;
import io.github.defective4.audioanalyzer.subsonic.model.Playlist;

public class App {

    public static final String COMMAND_ENV = "A_COMMAND";
    private static final String ERROR_EMPTY = "The song list is empty.";
    private static final String ERROR_UNANALYZED = "The song database is not analyzed. You have to use \"analyze\" command before doing any operations on the database";

    private final String analyzerURL;

    private final SubsonicAPI api;
    private final Repository db;
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(CLI.class);
    private final ModelLoader modelLoader = new ModelLoader(Path.of("./models"), logger);

    private final Random random = new Random();

    public App(String dbFile, String username, char[] password, String url, String analyzerURL)
            throws SQLException, IOException {
        this.analyzerURL = analyzerURL;
        db = new Repository("jdbc:sqlite:" + dbFile);
        api = username != null ? new SubsonicAPI(username, password, url) : null;
    }

    public void analyze(boolean onlyNew, String filterArtist, String filterAlbumArtist) throws Exception {
        try {
            TensorflowAnalyzer analyzer = getTensorflow();
            logger.info("Analyzer server OK");
            modelLoader.loadModels();
            Map<String, ModelMetadata> models = modelLoader.getLoadedModels();
            checkAPI();
            logger.info("Downloading track lists...");
            List<Entity> songs = api.getAllMusic(logger, filterArtist, filterAlbumArtist);
            logger.info("Downloaded information about %s songs".formatted(songs.size()));
            logger.info("Starting analysis...");

            List<String> ignore = onlyNew ? db.getAllSongIDs() : List.of();

            Path tmpDir = Files.createTempDirectory("e-analysis-");
            tmpDir.toFile().deleteOnExit();

            int errors = 0;

            db.setAnalysisState(AnalysisState.ABORTED);
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
                    List<String> missing = new ArrayList<>();
                    for (String model : ModelLoader.REQUIRED_MODELS)
                        if (!response.scoreMap().containsKey(model)) missing.add(model);
                    if (!missing.isEmpty()) {
                        throw new MissingModelsException("The analyzer service is missing the following models: %s"
                                .formatted(String.join(", ", missing.toArray(String[]::new))), missing);
                    }
                    float bpm = response.bpm();
                    logger.info("Storing results in database...");
                    logger.info("Results for %s:".formatted(song.title()));
                    String moodName = models.get("moods").classes()[response.mood()];
                    String instrumentName = models.get("instruments").classes()[response.instrument()];
                    String genreName = models.get("genres").classes()[response.genre()];

                    logger.info(" Mood: %s".formatted(moodName));
                    logger.info(" Instrument: %s".formatted(instrumentName));
                    logger.info(" Genre: %s".formatted(genreName));
                    logger.info(" BPM: %s".formatted(bpm));
                    db.insertData(song, response.scoreMap(), moodName, instrumentName, genreName, bpm, null);
                } catch (IOException e) {
                    e.printStackTrace();
                    errors++;
                    db.insertData(song, Map.of("failedPlaceholder", 1f), "null", "null", "null", 0, e);
                    continue;
                } finally {
                    target.toFile().delete();
                }
            }
            db.setAnalysisState(AnalysisState.ANALYZED);
            logger.info("All done!");
            logger.info("Analyzed {} songs with {} errors", songs.size(), errors);
        } catch (Exception e) {
            logger.error("An error occured: " + e.getMessage());
            throw e;
        }
    }

    public void checkModels(boolean update, String baseURL) throws IOException {
        if (update) {
            for (Entry<String, String> entry : ModelLoader.REQUIRED_MODEL_FILES.entrySet()) {
                URL url = URI.create(baseURL + entry.getValue()).toURL();
                try (InputStream in = url.openStream()) {
                    logger.info("Downloading {}...", url);
                    File target = new File(modelLoader.getModelsPath().toFile(), entry.getKey());
                    if (target.getParentFile() != null) target.getParentFile().mkdirs();
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        int missing = 0;
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println("List of models:");
            for (String key : ModelLoader.REQUIRED_MODEL_FILES.keySet()) {
                File f = new File(modelLoader.getModelsPath().toFile(), key);
                pw.print("- [%s] ".formatted(f.isFile() ? "+" : "-"));
                pw.println(key);
                if (!f.isFile()) missing++;
            }
        }
        System.out.println(writer.toString());
        if (missing > 0)
            logger.warn("{} models are missing. Run --update to download missing models", missing);
        else
            modelLoader.loadModels();
    }

    public void groupTracks(String baseSong, String moodFilter, String instrumentFilter, String genreFilter,
            String playlistName, String replacePlaylist, int limit, boolean newPublic, boolean sameGenre,
            boolean sameMood, boolean sameInstrument, boolean includeTempo, NumericExpression bpmExpr,
            NumericExpression vocalExpr, boolean sameArtist, String filterArtist, boolean shuffleSimilar,
            boolean printJSON) throws SQLException, IOException, InterruptedException {
        checkAPI();
        if (db.getAnalysisState() == AnalysisState.UNANALYZED) {
            logger.error(ERROR_UNANALYZED);
            return;
        }

        if (db.hasFailedTracks()) {
            logger.warn("Some songs are marked as failed. Consider reanalyzing the song database.");
            Thread.sleep(1000);
        }

        if (db.getAnalysisState() == AnalysisState.ABORTED) {
            printAbortedWarning();
        }

        logger.info("Getting tracks from the database...");
        List<Track> tracks = new ArrayList<>(db.getAllTracks(true));
        if (tracks.isEmpty()) {
            logger.error(ERROR_EMPTY);
            return;
        }
        logger.info("Loaded {} tracks", tracks.size());
        Collections.shuffle(tracks, random);
        Stream<Track> stream = tracks.stream();
        if (filterArtist != null) stream = stream.filter(t -> t.artist().equalsIgnoreCase(filterArtist));
        if (moodFilter != null && sameMood) {
            Set<String> set = tracks.stream().map(Track::mood).collect(Collectors.toUnmodifiableSet());
            if (moodFilter.equalsIgnoreCase("?list")) {
                logger.info("Available moods:");
                set.forEach(s -> logger.info(" - " + s));
                return;
            } else if (!set.contains(moodFilter)) {
                logger.error("Mood {} does not exist", moodFilter);
                return;
            }
            stream = stream.filter(t -> t.mood().equalsIgnoreCase(moodFilter));
        }
        if (instrumentFilter != null && !sameInstrument) {
            Set<String> set = tracks.stream().map(Track::instrument).collect(Collectors.toUnmodifiableSet());
            if (instrumentFilter.equalsIgnoreCase("?list")) {
                logger.info("Available instruments:");
                set.forEach(s -> logger.info(" - " + s));
                return;
            } else if (!set.contains(instrumentFilter)) {
                logger.error("Instrument {} does not exist", instrumentFilter);
                return;
            }
            stream = stream.filter(t -> t.instrument().equalsIgnoreCase(instrumentFilter));
        }
        if (genreFilter != null && !sameGenre) {
            Set<String> set = tracks.stream().map(Track::genre).collect(Collectors.toUnmodifiableSet());
            if (genreFilter.equalsIgnoreCase("?list")) {
                logger.info("Available genres:");
                set.forEach(s -> logger.info(" - " + s));
                return;
            } else if (!set.stream().anyMatch(e -> Pattern.matches(genreFilter.toLowerCase(), e.toLowerCase()))) {
                logger.error("Genre {} does not exist", genreFilter);
                return;
            }
            stream = stream.filter(t -> Pattern.matches(genreFilter.toLowerCase(), t.genre().toLowerCase()));
        }

        Optional<Track> baseOp = Optional.empty();
        if (baseSong != null) {
            try {
                baseOp = db.getTrackByIdOrName(baseSong);
            } catch (IllegalStateException e) {
                logger.error(e.getMessage());
                return;
            }
            if (!baseOp.isPresent()) {
                logger.error("Track with id or name %s does not exist.".formatted(baseSong));
                return;
            }
        }

        logger.info("Finding matching tracks...");
        if (baseOp.isPresent()) {
            Track base = baseOp.get();
            stream = stream.sorted((t1, t2) -> {
                double diff = base.calculateSimilarity(t1, includeTempo) - base.calculateSimilarity(t2, includeTempo);
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            });
            if (sameGenre) stream = stream.filter(track -> track.genre().equals(base.genre()));
            if (sameInstrument) stream = stream.filter(track -> track.instrument().equals(base.instrument()));
            if (sameMood) stream = stream.filter(track -> track.mood().equals(base.mood()));

            if (sameArtist) stream = stream.filter(t -> t.artist().equals(base.artist()));

        }

        if (bpmExpr != null) {
            stream = stream.filter(track -> {
                int val = bpmExpr.intValue();
                return switch (bpmExpr.getType()) {
                    case LESS_THAN -> track.bpm() < val;
                    case MORE_THAN -> track.bpm() > val;
                    default -> track.bpm() == val;
                };
            });
        }

        if (vocalExpr != null) {
            stream = stream.filter(track -> {
                float val = vocalExpr.floatValue();
                return switch (vocalExpr.getType()) {
                    case LESS_THAN -> track.getVocality() < val;
                    case MORE_THAN -> track.getVocality() > val;
                    default -> track.getVocality() == val;
                };
            });
        }

        List<Track> similar = new ArrayList<>(stream.limit(limit).toList());
        if (shuffleSimilar) Collections.shuffle(similar);

        logger.info("Updating playlist...");
        Playlist playlist;
        boolean pub;
        if (replacePlaylist != null) {
            try {
                playlist = api.getPlaylist(replacePlaylist);
            } catch (SubsonicException e) {
                if (e.getError().code() == 70) {
                    logger.error("Playlist with id %s was not found".formatted(replacePlaylist));
                    return;
                }
                throw e;
            }
            pub = playlist.isPublic();
            int songs = playlist.entry().length;
            for (int i = 0; i < songs; i++) api.updatePlaylist(playlist.id(), null, songs - i - 1, pub);
        } else {
            playlist = api.createPlaylist(playlistName);
            pub = newPublic;
            api.updatePlaylist(playlist.id(), null, -1, pub);
        }
        logger.info("Adding songs to the playlist...");
        for (Track t : similar) api.updatePlaylist(playlist.id(), t.id(), -1, pub);
        logger.info("Added {} songs to playlist {}!", similar.size(), playlist.name());

        if (printJSON) System.out.println(gson.toJson(playlist));
    }

    public void managePlaylist(String createNamed, String remove) throws IOException {
        if (createNamed != null) {
            logger.info("Creating a new playlist named \"{}\"...", createNamed);
            Playlist pls = api.createPlaylist(createNamed);
            logger.info("Playlit created!");
            System.out.println(gson.toJson(pls));
        }
        if (remove != null) {
            Playlist toRemove;
            try {
                toRemove = api.getPlaylist(remove);
            } catch (SubsonicException e) {
                if (e.getError().code() == 70) {
                    logger.info("Getting a list of playlists...");
                    toRemove = api.getPlaylists().stream().filter(pls -> pls.name().equals(remove)).findAny()
                            .orElse(null);
                } else {
                    throw e;
                }
            }
            if (toRemove != null) {
                logger.info("Deleting playlist \"{}\"", toRemove.name());
                api.deletePlaylist(toRemove.id());
                logger.info("Playlist deleted!");
            } else {
                logger.error("Playlist {} not found", remove);
            }
        }
    }

    public void printEnvironment(boolean uncensor) {
        try {
            String cmd = System.getenv(COMMAND_ENV);
            cmd = cmd == null ? "<unset>" : cmd;
            System.out.println(COMMAND_ENV + "=" + cmd);
            for (Field field : ProgramOptions.class.getFields())
                if (field.getType() == Option.class && field.isAnnotationPresent(EnvironmentVariable.class)) {
                    String key = ((Option) field.get(null)).getLongOpt();
                    EnvironmentVariable var = field.getAnnotation(EnvironmentVariable.class);
                    Object val = ProgramOptions.getEnvironmentVariable(key);
                    String valString = val == null ? "<unset>" : String.valueOf(val);
                    if (!uncensor && var.sensitive() && val != null) valString = valString.replaceAll(".", "*");
                    System.out.println("%s=%s".formatted(var.value(), valString));
                }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void printSongs(PrintFormat printFormat, String song, String output)
            throws SQLException, IOException, InterruptedException {
        if (db.getAnalysisState() == AnalysisState.UNANALYZED) {
            logger.error(ERROR_UNANALYZED);
            return;
        }
        if (db.getAnalysisState() == AnalysisState.ABORTED) {
            printAbortedWarning();
        }
        logger.info("Retrieving song statistics...");
        List<Track> tracks;
        if (song == null) {
            tracks = db.getAllTracks(false);
        } else {
            Optional<Track> track = db.getTrackByIdOrName(song);
            if (!track.isPresent()) {
                logger.error("Song {} does not exist", song);
                return;
            }
            tracks = Collections.singletonList(track.get());
        }
        if (tracks.isEmpty()) {
            logger.error(ERROR_EMPTY);
            return;
        }
        try (Writer writer = new OutputStreamWriter(
                output.equals("-") ? System.out : Files.newOutputStream(Path.of(output)))) {
            switch (printFormat) {
                case JSON -> { new Gson().toJson(tracks, writer); }
                case JSON_PRETTY -> { new GsonBuilder().setPrettyPrinting().create().toJson(tracks, writer); }
                case MARKDOWN -> {
                    try (MarkdownTableWriter md = new MarkdownTableWriter(writer, new String[] { "Id", "Name", "Artist",
                            "Mood", "Instrument", "Genre", "BPM", "Failed", "Failed reason" })) {
                        md.writeLines(tracks.stream()
                                .map(track -> new String[] { track.id(), track.name(), track.artist(), track.mood(),
                                        track.instrument(), track.genre(), Integer.toString(track.bpm()),
                                        Boolean.toString(track.failed()),
                                        track.failedReason() == null ? "" : track.failedReason() })
                                .toArray(String[][]::new));
                    }
                }
                default -> throw new IllegalArgumentException("Unknown print format");
            }
        }
        System.out.println();
        logger.info("Database analysis state: " + db.getAnalysisState());
        logger.info("Done!");
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

    private void printAbortedWarning() throws InterruptedException {
        logger.warn("Last analysis was aborted. The database might be incomplete, consider reanalyzing.");
        Thread.sleep(1000);
    }
}
