package io.github.vitaliyprogrammer.banking;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests on a real PostgreSQL. All subclasses share ONE Spring context: Spring caches
 * a context by its configuration, so anything a subclass adds (its own properties, MockMvc, its own WireMock port,
 * its own mail server) would start another whole application. That is why MockMvc and the fake outside world
 * (WireMock for HTTP, {@link FakeMailServer} for email) live here, for everyone.
 * After every test the ledger must reconcile.
 * The bill payment worker is off: tests that need it call {@code BillPaymentProcessor} themselves, at the moment
 * they choose, instead of racing a background timer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "banking.bill-payments.worker-enabled=false",
        "banking.providers.mock-enabled=false",
        // Short enough to keep the timeout tests fast, long enough for the first (cold) request to WireMock.
        "banking.providers.read-timeout=1500ms"
})
public abstract class IntegrationTest {

    /** Where the fake NBU answers on {@link #wireMock}. */
    protected static final String NBU_PATH = "/NBUStatService/v1/statdirectory/exchange";

    /**
     * The outside world (bill providers, the NBU) as one HTTP server for the whole test run. Started once and never
     * restarted, so its port, and with it the context's properties, is the same for every test class.
     * Stubs are cleared before each test.
     */
    protected static final WireMockServer wireMock = startWireMock();

    /** The bank's SMTP server for the whole run. It accepts every email unless a test says otherwise. */
    protected static final FakeMailServer mailServer = new FakeMailServer().start();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void outsideWorld(DynamicPropertyRegistry registry) {
        registry.add("banking.providers.base-url", wireMock::baseUrl);
        registry.add("banking.exchange-rates.nbu-url", () -> wireMock.baseUrl() + NBU_PATH + "?json");
        registry.add("spring.mail.host", mailServer::host);
        registry.add("spring.mail.port", mailServer::smtpPort);
        // A busy Docker can make the first SMTP answer slow.
        registry.add("spring.mail.properties.mail.smtp.timeout", () -> "20000");
    }

    @BeforeEach
    void forgetOtherTestsSetup() {
        wireMock.resetAll();
        mailServer.acceptEverything();
    }

    private static WireMockServer startWireMock() {
        WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        return server;
    }

    /**
     * Invariants that must hold after any sequence of operations:
     * the cached balance equals the sum of the account's entries, and money is never created or lost.
     */
    @AfterEach
    void ledgerReconciles() {
        Integer mismatchedAccounts = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM accounts a
                WHERE a.balance <> COALESCE((
                    SELECT sum(CASE e.direction WHEN 'CREDIT' THEN e.amount ELSE -e.amount END)
                    FROM ledger_entries e WHERE e.account_id = a.id), 0)
                """, Integer.class);
        assertThat(mismatchedAccounts).as("accounts whose balance differs from their ledger entries").isZero();

        List<BigDecimal> totalsByCurrency = jdbcTemplate.queryForList(
                "SELECT sum(balance) FROM accounts GROUP BY currency", BigDecimal.class);
        assertThat(totalsByCurrency).allSatisfy(total -> assertThat(total).isEqualByComparingTo("0"));
    }

    protected UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, created_at)
                VALUES (?, ?, 'not-used', 'Test', 'Client', 'CLIENT', 'ACTIVE', now())
                """, id, "client-" + id + "@example.com");
        return id;
    }

    protected BigDecimal balanceOf(UUID accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    /** Starts all tasks at the same instant (a start gate), so they really compete for the same rows. */
    protected static <T> List<T> runConcurrently(int count, IntFunction<Callable<T>> taskFactory) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                Callable<T> task = taskFactory.apply(i);
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }
}
