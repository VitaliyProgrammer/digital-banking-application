package io.github.vitaliyprogrammer.banking.common.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Whoever opens the bank's address in a browser lands on the demo client, not on a 401. */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addRedirectViewController("/", "/demo/index.html");
        registry.addRedirectViewController("/demo", "/demo/index.html");
        registry.addRedirectViewController("/demo/", "/demo/index.html");
    }
}
