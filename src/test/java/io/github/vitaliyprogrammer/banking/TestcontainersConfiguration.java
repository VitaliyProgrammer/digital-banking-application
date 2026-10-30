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
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * The default 60 s is not always enough when the whole suite starts several contexts and Docker is busy:
     * the first PostgreSQL then times out although nothing is wrong. A slow start is waited for; a broken one still fails.
     */
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
                .withExposedPorts(6379)
                .withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-alpine"))
                .withStartupTimeout(STARTUP_TIMEOUT);
    }
}
