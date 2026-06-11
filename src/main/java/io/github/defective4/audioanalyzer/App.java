package io.github.defective4.audioanalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.defective4.audioanalyzer.ml.Database;
import io.github.defective4.audioanalyzer.ml.ModelLoader;
import io.github.defective4.audioanalyzer.ml.TensorflowAnalyzer;
import io.github.defective4.audioanalyzer.ml.model.AnalysisResponse;
import io.github.defective4.audioanalyzer.ml.model.ModelMetadata;
import io.github.defective4.audioanalyzer.ml.model.Track;
import io.github.defective4.audioanalyzer.subsonic.SubsonicAPI;
import io.github.defective4.audioanalyzer.subsonic.exception.SubsonicException;
import io.github.defective4.audioanalyzer.subsonic.model.Entity;
import io.github.defective4.audioanalyzer.subsonic.model.Playlist;

public class App {

    private final String analyzerURL;

    private final SubsonicAPI api;
    private final Database db;
    private final Logger logger = LoggerFactory.getLogger(CLI.class);
    private final Random random = new Random();

    public App(String jdbcURL, String username, char[] password, String url, String analyzerURL)
            throws SQLException, IOException {
        this.analyzerURL = analyzerURL;
        db = new Database(jdbcURL);
        api = new SubsonicAPI(username, password, url);
    }

    public void groupTracks(String baseSong, String moodFilter, String instrumentFilter, String genreFilter,
            String playlistName, String replacePlaylist, int limit, boolean newPublic, boolean similarGenre,
            boolean similarMood, boolean similarInstrument) throws SQLException, IOException {
        checkAPI();

        logger.info("Getting tracks from the database...");
        List<Track> tracks = new ArrayList<>(db.getAllTracks());
        logger.info("Loaded {} tracks", tracks.size());
        Collections.shuffle(tracks, random);
        Stream<Track> stream = tracks.stream();
        if (moodFilter != null && similarMood) {
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
        if (instrumentFilter != null && !similarInstrument) {
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
        if (genreFilter != null && !similarGenre) {
            Set<String> set = tracks.stream().map(Track::genre).collect(Collectors.toUnmodifiableSet());
            if (genreFilter.equalsIgnoreCase("?list")) {
                logger.info("Available genres:");
                set.forEach(s -> logger.info(" - " + s));
                return;
            } else if (!set.contains(genreFilter)) {
                logger.error("Genre {} does not exist", genreFilter);
                return;
            }
            stream = stream.filter(t -> t.genre().equalsIgnoreCase(genreFilter));
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
                double diff = calculateSimilarity(t1, base) - calculateSimilarity(t2, base);
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            });
            if (similarGenre) stream = stream.filter(track -> track.genre().equals(base.genre()));
            if (similarInstrument) stream = stream.filter(track -> track.instrument().equals(base.instrument()));
            if (similarMood) stream = stream.filter(track -> track.mood().equals(base.mood()));
//            stream = stream.filter(track -> !track.id().equals(base.id()));
        }

        List<Track> similar = new ArrayList<>(stream.limit(limit).toList());
        Collections.shuffle(similar);

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
    }

    public void index(boolean onlyNew) throws Exception {
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

            int errors = 0;

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
                    System.err.println();
                    db.insertData(song, response.scoreMap(), moodName, instrumentName, genreName, bpm);
                } catch (IOException e) {
                    e.printStackTrace();
                    errors++;
                    continue;
                } finally {
                    target.toFile().delete();
                }
            }
            logger.info("All done!");
            logger.info("Analyzed {} songs with {} errors", songs.size(), errors);
        } catch (Exception e) {
            logger.error("An error occured: " + e.getMessage());
            throw e;
        }
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

    private static double calculateSimilarity(Track track1, Track track2) {
        double sum = 0;
        for (Entry<String, Float> entry : track1.scores().entrySet()) {
            float diff = entry.getValue() - track2.scores().get(entry.getKey());
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

}
