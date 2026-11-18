package comp3011;

import java.nio.file.Path;

public record AudioChunk(
        Path audioPath,
        double startSeconds) {
}