package comp3011;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class FrameSubtitler extends FrameProcessor {

    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private volatile List<SubtitleCue> cues = List.of();
    private volatile boolean enabled;

    public void setCues(List<SubtitleCue> cues) {
        this.cues = List.copyOf(cues);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void process(Frame frame, InfoFrame info) {
        if (!enabled || frame.image == null) {
            return;
        }

        double playbackSeconds = info.timestampUs() / 1_000_000.0;
        SubtitleCue activeCue = null;

        for (SubtitleCue cue : cues) {
            if (cue.isActiveAt(playbackSeconds)) {
                activeCue = cue;
                break;
            }
        }

        if (activeCue == null) {
            return;
        }

        BufferedImage image = converter.getBufferedImage(frame);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String[] lines = activeCue.text().split("\n");
            int fontSize = Math.max(12, image.getHeight() / 22);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            FontMetrics metrics = graphics.getFontMetrics(font);

            int widestLine = 0;
            for (String line : lines) {
                widestLine = Math.max(widestLine, metrics.stringWidth(line));
            }

            double availableWidth = image.getWidth() * 0.9;

            if (widestLine > availableWidth) {
                float fittedSize = (float) (
                        fontSize * availableWidth / widestLine);
                font = font.deriveFont(Math.max(1.0f, fittedSize));
            }

            graphics.setFont(font);
            metrics = graphics.getFontMetrics();

            int margin = Math.max(4, image.getHeight() / 20);
            int baseline = image.getHeight() - margin - metrics.getDescent()
                    - (lines.length - 1) * metrics.getHeight();

            graphics.setStroke(new BasicStroke(
                    Math.max(1.0f, font.getSize2D() / 12.0f),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            for (String line : lines) {
                float left = (image.getWidth() - metrics.stringWidth(line)) / 2.0f;

                GlyphVector glyphs = font.createGlyphVector(
                        graphics.getFontRenderContext(), line);
                Shape outline = glyphs.getOutline(left, baseline);

                graphics.setColor(Color.BLACK);
                graphics.draw(outline);
                graphics.setColor(Color.WHITE);
                graphics.fill(outline);

                baseline += metrics.getHeight();
            }
        } finally {
            graphics.dispose();
        }

        Java2DFrameConverter.copy(image, frame);
    }
}