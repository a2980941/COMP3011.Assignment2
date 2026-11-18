package comp3011;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

public class SubtitleClient {

    public String requestSubtitles(Path audioPath) throws IOException,
            InterruptedException {

        String boundary = "SubtitleBoundary" + UUID.randomUUID();

        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"audio\"; "
                + "filename=\"audio.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n";

        String footer = "\r\n--" + boundary + "--\r\n";

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(header),
                HttpRequest.BodyPublishers.ofFile(audioPath),
                HttpRequest.BodyPublishers.ofString(footer));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/subtitles"))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type",
                        "multipart/form-data; boundary=" + boundary)
                .POST(body)
                .build();

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Subtitle server returned HTTP "
                        + response.statusCode() + ": " + response.body());
            }

            return response.body();
        }
    }
}