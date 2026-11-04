package io.github.vitaliyprogrammer.banking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A real SMTP server (Mailpit) for the whole test run. It accepts every email and shows it through its HTTP API,
 * or, when a test asks, refuses every recipient the way a mail server in trouble does. One server that can do
 * both keeps one Spring context: two tests that need "mail works" and "mail is down" do not need two addresses.
 */
public final class FakeMailServer {

    private static final int SMTP_PORT = 1025;

    private static final int API_PORT = 8025;

    /** 451: "try again later". A temporary failure, so the bank must retry, not give up. */
    private static final String REFUSE_EVERY_RECIPIENT = """
            {"Recipient": {"ErrorCode": 451, "Probability": 100}}""";

    private static final String ACCEPT_EVERYTHING = """
            {"Authentication": {"ErrorCode": 0, "Probability": 0},
             "Recipient": {"ErrorCode": 0, "Probability": 0},
             "Sender": {"ErrorCode": 0, "Probability": 0}}""";

    private final GenericContainer<?> container =
            new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.6"))
                    .withExposedPorts(SMTP_PORT, API_PORT)
                    .withEnv("MP_ENABLE_CHAOS", "true")
                    .withStartupTimeout(Duration.ofMinutes(3));

    private final HttpClient http = HttpClient.newHttpClient();

    private final JsonMapper json = JsonMapper.builder().build();

    FakeMailServer start() {
        container.start();
        return this;
    }

    public String host() {
        return container.getHost();
    }

    public int smtpPort() {
        return container.getMappedPort(SMTP_PORT);
    }

    /** Every email received so far, as Mailpit's JSON. */
    public String messages() {
        return call(HttpRequest.newBuilder(api("/api/v1/messages")).GET());
    }

    /** How many emails this address has received so far. */
    public int countTo(String email) {
        return search("to:\"" + email + "\"").path("messages_count").asInt();
    }

    /** How many emails with this subject this address has received so far. */
    public int countTo(String email, String subject) {
        return search(byAddressAndSubject(email, subject)).path("messages_count").asInt();
    }

    /** The plain text of the newest email with this subject to this address; empty if there is none. */
    public String latestTextTo(String email, String subject) {
        JsonNode messages = search(byAddressAndSubject(email, subject)).path("messages");
        if (messages.isEmpty()) {
            return "";
        }
        String id = messages.get(0).path("ID").asString();
        return json.readTree(call(HttpRequest.newBuilder(api("/api/v1/message/" + id)).GET())).path("Text").asString();
    }

    private static String byAddressAndSubject(String email, String subject) {
        return "to:\"" + email + "\" subject:\"" + subject + "\"";
    }

    private JsonNode search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return json.readTree(call(HttpRequest.newBuilder(api("/api/v1/search?query=" + encoded)).GET()));
    }

    /** From now on every email is refused with a temporary error, until {@link #acceptEverything()}. */
    public void refuseEverything() {
        setChaos(REFUSE_EVERY_RECIPIENT);
    }

    public void acceptEverything() {
        setChaos(ACCEPT_EVERYTHING);
    }

    private void setChaos(String triggers) {
        call(HttpRequest.newBuilder(api("/api/v1/chaos"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(triggers)));
    }

    private String call(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Mailpit answered " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private URI api(String path) {
        return URI.create("http://" + host() + ":" + container.getMappedPort(API_PORT) + path);
    }
}
