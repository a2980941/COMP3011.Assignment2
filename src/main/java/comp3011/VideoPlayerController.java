/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. Name: Ishan Soni  Student ID: A2980941
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;
import java.util.List;

import java.io.File;
import java.util.function.BiConsumer;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.nio.file.Files;
/**
 * The controller part of the video player model-view-controller architecture.
 * 
 * <p>
 * Wires up events from the {@link VideoPlayerView} to the {@link VideoPlayerModel}.
 * This includes button actions, keyboard shortcuts and display resizing.
 * </p>
 */

/**
 * Transfers the selected frame processor sequence from the application into the model while preserving the MVC ownership boundaries
 * developed with ai guidance. This is my own work
 */
public class VideoPlayerController {
    private final VideoPlayerModel model;
    private final VideoPlayerView view;
    private boolean subtitlesEnabled;
    private Thread subtitleTask;
    private final BiConsumer<Integer, Integer> videoSizeChangedHandler;
    
    private void prepareSubtitles(File videoFile) {
        if (subtitleTask != null) {
            subtitleTask.interrupt();
        }

        subtitleTask = Thread.ofVirtual().start(() -> {
            try {
                var audioPath = Files.createTempFile("subtitle-audio-", ".wav");

                try {
                	System.out.println("Starting audio extraction: " + videoFile.getAbsolutePath());
                    new AudioExtractor().extract(videoFile, audioPath.toFile());
                    System.out.println(
                            "Audio extraction complete: "
                            + Files.size(audioPath) + " bytes");
                    System.out.println("Requesting subtitles...");
                    String subtitleJson = new SubtitleClient().requestSubtitles(audioPath);
                    System.out.println("Subtitles received: " + subtitleJson);
                } finally {
                    Files.deleteIfExists(audioPath);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                System.err.println(
                        "Subtitle preparation failed: " + exception.getMessage());
            }
        });
    }
    
    public VideoPlayerController(
            boolean audioEnabled,
            List<FrameProcessor> frameProcessors,
            BiConsumer<Integer, Integer> videoSizeChangedHandler) {
        this.view = new VideoPlayerView();
        this.videoSizeChangedHandler = videoSizeChangedHandler;
        model = new VideoPlayerModel(
                audioEnabled,
                frameProcessors,
                this::onVideoSizeChanged,
                this::onFrameReady,
                this::onStatusChanged,
                this::onPlaybackStateChanged,
                this::onAudioOutputStateChanged);

        view.getStartOverButton().setOnAction(_ -> startOver());
        view.getBackFiveButton().setOnAction(_ -> backFiveSeconds());
        view.getPausePlayButton().setOnAction(_ -> togglePause());
        view.getForwardFiveButton().setOnAction(_ -> forwardFiveSeconds());
        view.getStopButton().setOnAction(_ -> stopAndExit());
        view.getAudioButton().setOnAction(_ -> toggleAudioOutput());
        view.showAudioEnabled(model.isAudioOutputEnabled());
        subtitlesEnabled = frameProcessors.stream()
                .anyMatch(processor -> processor instanceof FrameSubtitler);
        view.showSubtitlesEnabled(subtitlesEnabled);
        view.getSubtitleButton().setOnAction(_ -> toggleSubtitles());
    }

    public void installKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stopAndExit();
                event.consume();
            } else if (event.getCode() == KeyCode.SPACE) {
                togglePause();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                backFiveSeconds();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                forwardFiveSeconds();
                event.consume();
            } else if (event.getCode() == KeyCode.HOME) {
                startOver();
                event.consume();
            } else if (event.getCode() == KeyCode.A) {
                toggleAudioOutput();
                event.consume();
            }
            else if (event.getCode() == KeyCode.S) {
            toggleSubtitles();
            event.consume();
            }
        });
    }

    public VideoPlayerView getView() {
        return view;
    }

    public void play(File file) {
        view.setPlaybackControlsDisabled(false);
        model.play(file);
        prepareSubtitles(file);
    }

    public void shutdown() {
        if (subtitleTask != null) {
            subtitleTask.interrupt();
        }
        model.shutdown();
    }

    private void startOver() {
        model.startOver();
    }

    private void backFiveSeconds() {
        model.backFiveSeconds();
    }

    private void togglePause() {
        model.togglePause();
    }

    private void forwardFiveSeconds() {
        model.forwardFiveSeconds();
    }

    private void toggleAudioOutput() {
        model.toggleAudioOutput();
    }
    private void toggleSubtitles() {
        subtitlesEnabled = !subtitlesEnabled;
        view.showSubtitlesEnabled(subtitlesEnabled);
    }

    private void stopAndExit() {
        model.stopPlayback();
        javafx.application.Platform.exit();
    }

    private void onVideoSizeChanged(int width, int height) {
        videoSizeChangedHandler.accept(width, height);
    }

    private void onFrameReady(Image image) {
        view.setImage(image);
    }

    private void onStatusChanged(String status) {
        view.setStatus(status);
    }

    private void onPlaybackStateChanged(boolean playing, boolean paused) {
        if (playing && !paused) {
            view.showPauseAction();
        } else {
            view.showPlayAction();
        }
    }

    private void onAudioOutputStateChanged(boolean audioOutputEnabled) {
        view.showAudioEnabled(audioOutputEnabled);
    }
}
