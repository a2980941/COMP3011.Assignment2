package comp3011;

public record SubtitleCue(
        double startSeconds,
        double endSeconds,
        String text) {

    public boolean isActiveAt(double playbackSeconds) {
        return playbackSeconds >= startSeconds
                && playbackSeconds < endSeconds;
    }
}