package io.github.vitaliyprogrammer.banking.common.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Spring MVC alone, through real requests: the resolver test cannot see what the handler does before it. */
@SpringJUnitWebConfig(WebConfigurationTests.WebMvc.class)
class WebConfigurationTests {

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(WebConfiguration.class)
    static class WebMvc {
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void theBanksAddressLeadsToTheWebClient() throws Exception {

        mvc.perform(get("/")).andExpect(redirectedUrl("/app/"));
        mvc.perform(get("/app")).andExpect(redirectedUrl("/app/"));
    }

    @Test
    void theWebClientsRootOpensTheApplication() throws Exception {
        mvc.perform(get("/app/")).andExpect(status().isOk()).andExpect(forwardedUrl("/app/index.html"));
    }

    @Test
    void aClientRouteOpensTheApplicationWithoutBeingCached() throws Exception {

        mvc.perform(get("/app/cards"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void aMissingScriptStaysMissing() throws Exception {
        mvc.perform(get("/app/assets/deleted-in-the-last-deploy.js")).andExpect(status().isNotFound());
    }
}
