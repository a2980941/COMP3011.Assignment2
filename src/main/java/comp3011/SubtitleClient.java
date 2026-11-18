package comp3011;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.nio.file.Files;

public class SubtitleClient {
	
	public String requestSubtitlesWithRetry(Path audioPath)
	        throws IOException, InterruptedException {

	    long retryDelayMillis = 5000;

	    while (true) {
	        if (Thread.currentThread().isInterrupted()) {
	            throw new InterruptedException("Subtitle request cancelled.");
	        }

	        try {
	            return requestSubtitles(audioPath);
	        } catch (IOException exception) {
	            if (!Files.isReadable(audioPath)) {
	                throw exception;
	            }

	            System.err.println(
	                    "Subtitle request failed: " + exception
	                    + ". Retrying in "
	                    + retryDelayMillis / 1000 + " seconds.");

	            Thread.sleep(retryDelayMillis);
	            retryDelayMillis = Math.min(retryDelayMillis * 2, 30000);
	        }
	    }
	}

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

            int status = response.statusCode();

            if (status != 200) {
                String message = "Subtitle server returned HTTP "
                        + status + ": " + response.body();

                if (status == 408 || status == 429 || status >= 500) {
                    throw new IOException(message);
                }

                throw new IllegalStateException(message);
            }

            return response.body();
        }
    }
}