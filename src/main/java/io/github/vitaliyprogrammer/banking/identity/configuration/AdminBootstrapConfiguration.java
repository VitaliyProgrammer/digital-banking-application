package io.github.vitaliyprogrammer.banking.identity.configuration;

import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the first admin on startup. Later admins would be appointed by this one; nobody can register as admin.
 * Runs on every start but only acts once: an existing account is never changed, so a restart cannot reset
 * the admin's password, and an account a client registered with that email is never promoted.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrapConfiguration {

    @Bean
    public ApplicationRunner adminBootstrap(AdminProperties properties, UserService userService) {
        return _ -> {
            if (!properties.isConfigured()) {
                log.info("ADMIN_EMAIL / ADMIN_PASSWORD are not set: no admin account is created");
                return;
            }
            userService.createAdminIfAbsent(properties.email(), properties.password());
        };
    }
}
