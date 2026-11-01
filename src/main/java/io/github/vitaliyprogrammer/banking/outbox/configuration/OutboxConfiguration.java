package io.github.vitaliyprogrammer.banking.outbox.configuration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    /**
     * Durable: it survives a broker restart. Spring declares it on the first connection, so a fresh broker needs
     * no manual setup. Queues are not declared here: each consumer declares and binds its own.
     */
    @Bean
    TopicExchange bankingEventsExchange(OutboxProperties properties) {

        return new TopicExchange(properties.exchange(), true, false);
    }
}
