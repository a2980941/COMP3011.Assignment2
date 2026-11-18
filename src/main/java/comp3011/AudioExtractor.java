package comp3011;

import java.io.File;
import java.io.IOException;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class AudioExtractor {

    public void extract(File videoFile, File audioFile) throws Exception {
        try (FFmpegFrameGrabber audioGrabber =
                new FFmpegFrameGrabber(videoFile)) {

            audioGrabber.start();

            if (audioGrabber.getAudioChannels() == 0) {
                throw new IOException("This video has no audio track.");
            }

            try (FFmpegFrameRecorder recorder =
            		new FFmpegFrameRecorder(audioFile, audioGrabber.getAudioChannels())) {

                recorder.setFormat("wav");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
                recorder.setSampleRate(16000);
                recorder.start();

                Frame audioFrame;
                while ((audioFrame = audioGrabber.grabSamples()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Audio extraction cancelled.");
                    }

                    recorder.recordSamples(
                            audioFrame.sampleRate,
                            audioFrame.audioChannels,
                            audioFrame.samples);
                }

                recorder.stop();
            }
        }
    }
}