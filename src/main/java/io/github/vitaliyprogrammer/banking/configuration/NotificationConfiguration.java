package io.github.vitaliyprogrammer.banking.configuration;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
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

    /**
     * Messages that failed every attempt. Anything above zero needs a person: a broken event, or a bug.
     * While the broker is down the count cannot be read, and NaN (a gap on the graph) says exactly that.
     */
    @Bean
    MeterBinder notificationsDeadLetterGauge(AmqpAdmin amqpAdmin, NotificationProperties properties) {

        return registry -> Gauge.builder("bank.notifications.dead.letters", amqpAdmin, admin -> {
                    try {
                        QueueInformation queue = admin.getQueueInfo(properties.deadLetterQueue());
                        return queue != null ? queue.getMessageCount() : Double.NaN;
                    } catch (AmqpException e) {
                        return Double.NaN;
                    }
                })
                .description("Messages in the notifications dead-letter queue")
                .register(registry);
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
