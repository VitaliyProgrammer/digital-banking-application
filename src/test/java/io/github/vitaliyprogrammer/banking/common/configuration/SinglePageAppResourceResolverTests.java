package io.github.vitaliyprogrammer.banking.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class SinglePageAppResourceResolverTests {

    private final Resource location = new ClassPathResource("static/app/");
    private final SinglePageAppResourceResolver resolver = new SinglePageAppResourceResolver();

    @Test
    void anExistingFileIsServedAsItIs() throws IOException {
        assertThat(resolver.getResource("assets/app.js", location).getFilename()).isEqualTo("app.js");
    }

    @Test
    void aClientRouteOpensTheApplicationEvenAfterAReload() throws IOException {
        assertThat(resolver.getResource("cards/0b7c/details", location).getFilename()).isEqualTo("index.html");
        assertThat(resolver.getResource("", location).getFilename()).isEqualTo("index.html");
    }

    @Test
    void aMissingFileStaysMissingInsteadOfBecomingThePage() throws IOException {
        assertThat(resolver.getResource("assets/deleted-in-the-last-deploy.js", location)).isNull();
    }
}
