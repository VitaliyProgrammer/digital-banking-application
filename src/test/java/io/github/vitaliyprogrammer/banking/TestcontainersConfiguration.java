package io.github.vitaliyprogrammer.banking;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL, Redis and RabbitMQ for integration tests. {@code @ServiceConnection} wires each container's
 * address and credentials into Spring, so no test properties are needed.
 * <p>
 * The containers are static: ONE set for the whole test run, shared by every Spring context. Spring caches each
 * distinct test context (plain, MockMvc, receipts with Mailpit, rates with WireMock...) until the run ends; with a
 * set of containers per context that was six or seven sets alive at once, which a laptop's Docker cannot start in
 * time. Sharing is safe because every test creates its own users and accounts, and Flyway on an already migrated
 * database does nothing. Ryuk removes the containers when the JVM exits.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * The default 60 s is not always enough when Docker is busy: the first PostgreSQL then times out although
     * nothing is wrong. A slow start is waited for; a broken one still fails.
     */
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withStartupTimeout(STARTUP_TIMEOUT);

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
                    .withExposedPorts(6379)
                    .withStartupTimeout(STARTUP_TIMEOUT);

    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-alpine"))
                    .withStartupTimeout(STARTUP_TIMEOUT);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return REDIS;
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return RABBIT;
    }
}
