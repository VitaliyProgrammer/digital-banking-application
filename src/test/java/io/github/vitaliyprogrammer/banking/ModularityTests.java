package io.github.vitaliyprogrammer.banking;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build when a module reaches into another module's internals
 * or when modules depend on each other in a cycle.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(DigitalBankingApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }
}
