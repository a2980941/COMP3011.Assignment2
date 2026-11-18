package comp3011;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class AudioExtractor {

    @FunctionalInterface
    public interface ChunkHandler {
        void handle(AudioChunk chunk) throws Exception;
    }

    public void extractChunks(File videoFile, ChunkHandler handler)
            throws Exception {

        try (FFmpegFrameGrabber audioGrabber =
                new FFmpegFrameGrabber(videoFile)) {

            audioGrabber.start();

            int channels = audioGrabber.getAudioChannels();

            if (channels == 0) {
                throw new IOException("This video has no audio track.");
            }

            double chunkSeconds = Math.min(
                    60.0, 8_000_000.0 / (16000 * 2 * channels));

            long chunkDurationUs = (long) (chunkSeconds * 1_000_000);
            Frame audioFrame = audioGrabber.grabSamples();

            while (audioFrame != null) {
                checkInterrupted();

                long chunkStartUs = audioFrame.timestamp;
                var audioPath = Files.createTempFile(
                        "subtitle-chunk-", ".wav");

                try {
                    try (FFmpegFrameRecorder recorder =
                            new FFmpegFrameRecorder(
                                    audioPath.toFile(), channels)) {

                        recorder.setFormat("wav");
                        recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
                        recorder.setSampleRate(16000);
                        recorder.start();

                        do {
                            checkInterrupted();

                            recorder.recordSamples(
                                    audioFrame.sampleRate,
                                    audioFrame.audioChannels,
                                    audioFrame.samples);

                            audioFrame = audioGrabber.grabSamples();

                        } while (audioFrame != null
                                && audioFrame.timestamp - chunkStartUs
                                < chunkDurationUs);

                        recorder.stop();
                    }

                    checkInterrupted();

                    handler.handle(new AudioChunk(
                            audioPath, chunkStartUs / 1_000_000.0));

                } finally {
                    Files.deleteIfExists(audioPath);
                }
            }
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Audio extraction cancelled.");
        }
    }
}