package io.github.vitaliyprogrammer.banking.business.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BusinessProperties.class)
public class BusinessConfiguration {
}
