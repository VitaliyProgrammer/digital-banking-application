package io.github.vitaliyprogrammer.banking.common.configuration;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The web client lives under /app, so its routes never collide with the API (/cards is data, /app/cards is a page).
 * Whoever opens the bank's address in a browser lands on the web client, not on a 401.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    private static final String WEB_CLIENT_LOCATION = "classpath:/static/app/";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addRedirectViewController("/", "/app/");
        registry.addRedirectViewController("/app", "/app/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Built files carry a content hash in their names: a new version is a new name, so the old one never changes.
        registry.addResourceHandler("/app/assets/**")
                .addResourceLocations(WEB_CLIENT_LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // index.html is never cached, so a deploy reaches every browser on its next page load.
        registry.addResourceHandler("/app/**")
                .addResourceLocations(WEB_CLIENT_LOCATION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SinglePageAppResourceResolver());
    }
}
