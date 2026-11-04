package io.github.vitaliyprogrammer.banking.common.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Lets work that must not hold up a request run on its own thread (@Async), e.g. sending a password reset email:
 * its SMTP time must not show in the response, or the response time would tell which emails have an account.
 * With virtual threads on, Spring Boot's task executor gives every such task a virtual thread.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {
}
