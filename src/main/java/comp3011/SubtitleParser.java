package comp3011;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class SubtitleParser {

    public List<SubtitleCue> parse(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode segments = root.get("segments");
        JsonNode words = root.get("words");

        if (segments == null || !segments.isArray()
                || words == null || !words.isArray()) {
            throw new IllegalArgumentException(
                    "Subtitle response must contain segments and words.");
        }

        List<SubtitleCue> cues = new ArrayList<>();
        Set<Integer> usedWordIndexes = new HashSet<>();

        for (JsonNode segment : segments) {
            double segmentStart = segment.get("start").asDouble();
            double segmentEnd = segment.get("end").asDouble();

            String caption = "";
            double captionStart = 0.0;
            double captionEnd = 0.0;

            for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
                JsonNode word = words.get(wordIndex);

                double wordStart = word.get("start").asDouble();
                double wordEnd = word.get("end").asDouble();
                double wordMiddle = (wordStart + wordEnd) / 2.0;
                String wordText = word.get("word").asText().trim();

                boolean atSegmentEnd = wordStart == wordEnd
                        && wordStart == segmentEnd;

                if (usedWordIndexes.contains(wordIndex)
                        || wordMiddle < segmentStart
                        || (wordMiddle >= segmentEnd && !atSegmentEnd)
                        || wordText.isBlank()
                        || wordEnd < wordStart) {
                    continue;
                }

                usedWordIndexes.add(wordIndex);

                if (wrapText(wordText) == null) {
                    throw new IllegalArgumentException(
                            "A transcription word exceeds the caption capacity.");
                }

                String candidate = caption.isEmpty()
                        ? wordText : caption + " " + wordText;

                boolean tooLong = wordEnd - captionStart > 7.0;
                boolean tooMuchText = wrapText(candidate) == null;
                boolean naturalPause = wordStart - captionEnd >= 0.6;
                boolean sentenceEnded = caption.matches(".*[.!?]$")
                        && captionEnd - captionStart >= 1.0;

                if (!caption.isEmpty()
                        && (tooLong || tooMuchText
                        || naturalPause || sentenceEnded)) {
                    addCue(cues, captionStart, captionEnd, caption);
                    caption = "";
                }

                if (caption.isEmpty()) {
                    captionStart = wordStart;
                    caption = wordText;
                } else {
                    caption += " " + wordText;
                }

                captionEnd = wordEnd;
            }

            if (!caption.isEmpty()) {
                addCue(cues, captionStart, captionEnd, caption);
            }
        }

        List<SubtitleCue> adjustedCues = new ArrayList<>();

        for (int index = 0; index < cues.size(); index++) {
            SubtitleCue cue = cues.get(index);

            double end = Math.max(
                    cue.endSeconds(), cue.startSeconds() + 1.0);

            end = Math.min(end, cue.startSeconds() + 7.0);

            if (index + 1 < cues.size()) {
                end = Math.min(end, cues.get(index + 1).startSeconds());
            }

            if (end > cue.startSeconds()) {
                adjustedCues.add(new SubtitleCue(
                        cue.startSeconds(), end, cue.text()));
            }
        }

        return List.copyOf(adjustedCues);
    }

    private void addCue(
            List<SubtitleCue> cues,
            double start,
            double end,
            String text) {

        cues.add(new SubtitleCue(
                start, Math.min(end, start + 7.0), wrapText(text)));
    }

    private String wrapText(String text) {
        if (text.length() <= 42) {
            return text;
        }

        int preferredBreak = -1;
        int fallbackBreak = -1;

        for (int index = 1; index <= 42 && index < text.length(); index++) {
            if (text.charAt(index) != ' ') {
                continue;
            }

            String remaining = text.substring(index + 1).trim();

            if (remaining.length() <= 42) {
                fallbackBreak = index;

                char previous = text.charAt(index - 1);
                if (".,!?;:".indexOf(previous) >= 0) {
                    preferredBreak = index;
                }
            }
        }

        int splitAt = preferredBreak >= 0
                ? preferredBreak : fallbackBreak;

        if (splitAt >= 0) {
            return text.substring(0, splitAt).trim()
                    + "\n"
                    + text.substring(splitAt + 1).trim();
        }

        if (!text.contains(" ") && text.length() <= 84) {
            return text.substring(0, 42)
                    + "\n"
                    + text.substring(42);
        }

        return null;
    }
}