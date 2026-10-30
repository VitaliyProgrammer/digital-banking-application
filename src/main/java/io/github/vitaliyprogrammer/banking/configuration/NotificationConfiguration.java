package io.github.vitaliyprogrammer.banking.configuration;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The notification consumer's own queue. The outbox knows nothing about it: the queue decides what it listens to,
 * so a new consumer is a new queue and a new binding, with no change on the publishing side.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

    /** A rejected message goes, via the default exchange, straight to the dead-letter queue by its name. */
    @Bean
    Queue notificationsQueue(NotificationProperties properties) {

        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange("")
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue notificationsDeadLetterQueue(NotificationProperties properties) {

        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    /** Every payment event, everything about credit and support answers. */
    @Bean
    Declarables notificationsBindings(Queue notificationsQueue, TopicExchange bankingEventsExchange) {

        return new Declarables(
                BindingBuilder.bind(notificationsQueue).to(bankingEventsExchange).with("payment.*"),
                BindingBuilder.bind(notificationsQueue).to(bankingEventsExchange).with("credit.#"),
                BindingBuilder.bind(notificationsQueue).to(bankingEventsExchange).with("support.#"));
    }
}
