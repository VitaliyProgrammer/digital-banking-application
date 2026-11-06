package io.github.vitaliyprogrammer.banking.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The Ukrainian dictionary keeps up with the code. A new error without a translation would reach a Ukrainian
 * client in English without anyone noticing; a typo in a pattern would show "{0}" instead of the value.
 */
class ErrorTranslationsTests {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    /** A code is kebab-case: "insufficient-funds", "card-is-pin-locked". Sentences have spaces and capitals. */
    private static final Pattern CODE = Pattern.compile("\"([a-z]+(?:-[a-z0-9]+)+)\"");

    /** Outside the exception classes, a code is passed where the exception is built. */
    private static final Pattern THROW_SITE = Pattern.compile("Exception\\(|requireStatus\\(");

    @Test
    void everyErrorCodeHasAUkrainianText() throws IOException {
        Properties ukrainian = load("messages_uk.properties");

        Set<String> missing = new TreeSet<>();
        for (String code : codesInTheCode()) {
            if (!ukrainian.containsKey("error." + code)) {
                missing.add(code);
            }
        }

        assertThat(missing).as("codes without error.<code> in messages_uk.properties").isEmpty();
    }

    @Test
    void noTranslationIsLeftForACodeThatIsGone() throws IOException {
        Set<String> codes = codesInTheCode();

        Set<String> orphans = new TreeSet<>();
        for (String key : load("messages_uk.properties").stringPropertyNames()) {
            if (key.startsWith("error.") && !key.equals("error.unexpected") && !codes.contains(key.substring(6))) {
                orphans.add(key);
            }
        }

        assertThat(orphans).as("translations nobody throws any more").isEmpty();
    }

    @Test
    void everyPatternShowsItsValues() throws IOException {
        Properties ukrainian = load("messages_uk.properties");

        for (String key : ukrainian.stringPropertyNames()) {
            String text = ukrainian.getProperty(key);
            if (!text.contains("{")) {
                continue;
            }
            // An ASCII apostrophe starts a quoted part in MessageFormat: "{0}" after it would be printed as is.
            String formatted = new MessageFormat(text, Locale.forLanguageTag("uk"))
                    .format(new Object[] {1234.5, "VALUE", 3});

            assertThat(formatted).as(key).doesNotContain("{").doesNotContain("}");
        }
    }

    private static Set<String> codesInTheCode() throws IOException {
        Set<String> codes = new TreeSet<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                boolean exceptionClass = source.contains("extends ApplicationException");
                for (String line : source.lines().toList()) {
                    if (exceptionClass || THROW_SITE.matcher(line).find()) {
                        Matcher code = CODE.matcher(line);
                        while (code.find()) {
                            codes.add(code.group(1));
                        }
                    }
                }
            }
        }
        assertThat(codes).as("the scan finds the codes at all").hasSizeGreaterThan(60);
        return codes;
    }

    private static Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                ErrorTranslationsTests.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
