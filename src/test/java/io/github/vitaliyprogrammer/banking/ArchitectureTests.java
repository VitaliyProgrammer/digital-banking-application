package io.github.vitaliyprogrammer.banking;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerEntryRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerTransactionRepository;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.account.service.impl.AccountServiceImpl;
import io.github.vitaliyprogrammer.banking.account.service.impl.LedgerServiceImpl;
import io.github.vitaliyprogrammer.banking.audit.repository.AuditEntryRepository;
import io.github.vitaliyprogrammer.banking.audit.service.impl.AuditServiceImpl;
import io.github.vitaliyprogrammer.banking.card.repository.CardRepository;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.card.service.impl.CardServiceImpl;
import io.github.vitaliyprogrammer.banking.cashback.repository.CashbackAccrualRepository;
import io.github.vitaliyprogrammer.banking.cashback.repository.CashbackPayoutRepository;
import io.github.vitaliyprogrammer.banking.cashback.service.impl.CashbackServiceImpl;
import io.github.vitaliyprogrammer.banking.credit.repository.CreditApplicationRepository;
import io.github.vitaliyprogrammer.banking.credit.repository.CreditStatementRepository;
import io.github.vitaliyprogrammer.banking.credit.service.impl.CreditApplicationServiceImpl;
import io.github.vitaliyprogrammer.banking.credit.service.impl.CreditLineServiceImpl;
import io.github.vitaliyprogrammer.banking.exchange.repository.BankRateRepository;
import io.github.vitaliyprogrammer.banking.exchange.repository.ExchangeQuoteRepository;
import io.github.vitaliyprogrammer.banking.exchange.repository.OfficialRateRepository;
import io.github.vitaliyprogrammer.banking.exchange.service.impl.ExchangeQuoteServiceImpl;
import io.github.vitaliyprogrammer.banking.exchange.service.impl.ExchangeRateServiceImpl;
import io.github.vitaliyprogrammer.banking.exchange.service.impl.NbuRateClient;
import io.github.vitaliyprogrammer.banking.identity.repository.RefreshTokenRepository;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.impl.AuthenticationServiceImpl;
import io.github.vitaliyprogrammer.banking.identity.service.impl.RefreshTokenServiceImpl;
import io.github.vitaliyprogrammer.banking.identity.service.impl.UserServiceImpl;
import io.github.vitaliyprogrammer.banking.notification.repository.NotificationRepository;
import io.github.vitaliyprogrammer.banking.notification.service.impl.NotificationServiceImpl;
import io.github.vitaliyprogrammer.banking.outbox.repository.OutboxEventRepository;
import io.github.vitaliyprogrammer.banking.outbox.service.impl.OutboxRelay;
import io.github.vitaliyprogrammer.banking.outbox.service.impl.OutboxServiceImpl;
import io.github.vitaliyprogrammer.banking.payment.repository.PaymentRepository;
import io.github.vitaliyprogrammer.banking.payment.repository.ProviderRepository;
import io.github.vitaliyprogrammer.banking.payment.service.impl.BillPaymentProcessor;
import io.github.vitaliyprogrammer.banking.payment.service.impl.HttpProviderClient;
import io.github.vitaliyprogrammer.banking.payment.service.impl.PaymentServiceImpl;
import io.github.vitaliyprogrammer.banking.receipt.repository.ReceiptRepository;
import io.github.vitaliyprogrammer.banking.receipt.service.impl.ReceiptServiceImpl;
import io.github.vitaliyprogrammer.banking.savings.repository.InterestAccrualRepository;
import io.github.vitaliyprogrammer.banking.savings.repository.InterestPayoutRepository;
import io.github.vitaliyprogrammer.banking.savings.service.impl.SavingsServiceImpl;
import io.github.vitaliyprogrammer.banking.support.repository.SupportTicketRepository;
import io.github.vitaliyprogrammer.banking.support.repository.TicketMessageRepository;
import io.github.vitaliyprogrammer.banking.support.service.impl.SupportTicketServiceImpl;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestClient;

/**
 * Architecture rules checked on every build. The code is packaged by domain (account, payment, credit...), and each
 * domain is layered inside (controller → service → repository). On top of that each domain owns its tables, and
 * domains talk to each other only through service interfaces, never through another domain's implementation.
 * A rule that is broken fails the build with the exact class and line.
 */
@AnalyzeClasses(packages = "io.github.vitaliyprogrammer.banking", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String ROOT = "io.github.vitaliyprogrammer.banking";

    /** Controller → Service → Repository, inside every domain: never backwards and never skipping the service layer. */
    @ArchTest
    static final ArchRule layersDependOnlyDownwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(ROOT + ".*.controller..")
            .layer("Service").definedBy(ROOT + ".*.service..")
            .layer("Repository").definedBy(ROOT + ".*.repository..")
            .layer("Configuration").definedBy(ROOT + ".*.configuration..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Configuration")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controllersNeverExposeEntities = noClasses()
            .that().resideInAPackage(ROOT + ".*.controller..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .because("controllers return DTOs; an entity in a response would leak internals like a password hash");

    @ArchTest
    static final ArchRule servicePackageHoldsOnlyInterfaces = classes()
            .that().resideInAPackage(ROOT + ".*.service")
            .should().beInterfaces()
            .because("implementations live in service.impl");

    /**
     * Dependencies between domains go one way only: common ← account ← card / payment ← savings / credit / cashback /
     * receipt ← notification. A cycle (payment needs credit, credit needs payment) would make the two one module in
     * disguise: neither could be understood, tested, changed or split out without the other.
     */
    @ArchTest
    static final ArchRule domainsDependOnEachOtherWithoutCycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    /**
     * Domains meet only at their service interfaces and DTOs. If credit could reach into payment's implementation,
     * the two would quietly grow into one, and no module could be changed, tested or split out on its own.
     */
    @ArchTest
    static final ArchRule domainsDoNotReachIntoEachOthersImplementation = slices()
            .matching(ROOT + ".(*).service.impl..")
            .should().notDependOnEachOther();

    /** The money rule: a balance changes only together with a ledger entry, under the ledger's row lock. */
    @ArchTest
    static final ArchRule onlyTheLedgerChangesBalances = noClasses()
            .that().doNotBelongToAnyOf(LedgerServiceImpl.class)
            .should().callMethod(Account.class, "apply", TransactionType.class, EntryDirection.class, BigDecimal.class)
            .because("money moves only through LedgerService.post(), which locks the accounts and writes the journal");

    // Each domain owns its repositories. Other domains use its service interface, e.g. payments call
    // LedgerService.post() instead of touching AccountRepository, so the ledger's rules cannot be bypassed.
    // "Dependent classes", not "accessed by": most repository methods (findById, save) are inherited from
    // Spring Data, and an access check counts calls against the declaring interface, so it would miss them.

    @ArchTest
    static final ArchRule accountAndLedgerTablesBelongToTheAccountDomain = classes()
            .that().belongToAnyOf(AccountRepository.class, LedgerEntryRepository.class, LedgerTransactionRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(AccountServiceImpl.class, LedgerServiceImpl.class);

    @ArchTest
    static final ArchRule cardTableBelongsToTheCardDomain = classes()
            .that().belongToAnyOf(CardRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(CardServiceImpl.class);

    @ArchTest
    static final ArchRule paymentTablesBelongToThePaymentDomain = classes()
            .that().belongToAnyOf(PaymentRepository.class, ProviderRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(PaymentServiceImpl.class, BillPaymentProcessor.class);

    @ArchTest
    static final ArchRule rateTablesBelongToTheExchangeDomain = classes()
            .that().belongToAnyOf(OfficialRateRepository.class, BankRateRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(ExchangeRateServiceImpl.class);

    @ArchTest
    static final ArchRule quoteTableBelongsToTheExchangeDomain = classes()
            .that().belongToAnyOf(ExchangeQuoteRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(ExchangeQuoteServiceImpl.class);

    @ArchTest
    static final ArchRule interestTablesBelongToTheSavingsDomain = classes()
            .that().belongToAnyOf(InterestAccrualRepository.class, InterestPayoutRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(SavingsServiceImpl.class);

    @ArchTest
    static final ArchRule creditApplicationTableBelongsToTheCreditDomain = classes()
            .that().belongToAnyOf(CreditApplicationRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(CreditApplicationServiceImpl.class);

    @ArchTest
    static final ArchRule cashbackTablesBelongToTheCashbackDomain = classes()
            .that().belongToAnyOf(CashbackAccrualRepository.class, CashbackPayoutRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(CashbackServiceImpl.class);

    @ArchTest
    static final ArchRule creditStatementTableBelongsToTheCreditDomain = classes()
            .that().belongToAnyOf(CreditStatementRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(CreditLineServiceImpl.class);

    /** A credit line is lent money: it exists only because an application was approved, never on a plain request. */
    @ArchTest
    static final ArchRule onlyTheCreditModuleOpensCreditLines = noClasses()
            .that().doNotBelongToAnyOf(CreditLineServiceImpl.class)
            .should().callMethod(AccountService.class, "openCreditAccount", UUID.class, BigDecimal.class)
            .orShould().callMethod(CardService.class, "issueCreditCard", UUID.class);

    /** Payments announce what happened; they never call the cashback module directly. */
    @ArchTest
    static final ArchRule paymentsDoNotKnowAboutCashback = noClasses()
            .that().belongToAnyOf(PaymentServiceImpl.class, BillPaymentProcessor.class)
            .should().dependOnClassesThat().belongToAnyOf(CashbackServiceImpl.class);

    @ArchTest
    static final ArchRule receiptTableBelongsToTheReceiptDomain = classes()
            .that().belongToAnyOf(ReceiptRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(ReceiptServiceImpl.class);

    @ArchTest
    static final ArchRule outboxTableBelongsToTheOutbox = classes()
            .that().belongToAnyOf(OutboxEventRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(OutboxServiceImpl.class, OutboxRelay.class);

    /** Only the audit service writes the log, and only through record(): nobody else can add or shape entries. */
    @ArchTest
    static final ArchRule auditTableBelongsToTheAudit = classes()
            .that().belongToAnyOf(AuditEntryRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(AuditServiceImpl.class);

    @ArchTest
    static final ArchRule ticketTablesBelongToSupport = classes()
            .that().belongToAnyOf(SupportTicketRepository.class, TicketMessageRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(SupportTicketServiceImpl.class);

    @ArchTest
    static final ArchRule notificationTableBelongsToNotifications = classes()
            .that().belongToAnyOf(NotificationRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(NotificationServiceImpl.class);

    /**
     * Nobody publishes to the broker directly: a direct send next to a database change is exactly the
     * "message sent, transaction rolled back" bug. Everything goes through OutboxService.record().
     */
    @ArchTest
    static final ArchRule onlyTheOutboxRelayPublishesToTheBroker = noClasses()
            .that().doNotBelongToAnyOf(OutboxRelay.class)
            .should().dependOnClassesThat().belongToAnyOf(RabbitTemplate.class, AmqpTemplate.class);

    /** Only the clients know how to talk HTTP to the outside world; the rest of the code sees business outcomes. */
    @ArchTest
    static final ArchRule onlyTheHttpClientsMakeHttpCalls = noClasses()
            .that().doNotBelongToAnyOf(HttpProviderClient.class, NbuRateClient.class)
            .should().dependOnClassesThat().belongToAnyOf(RestClient.class);

    @ArchTest
    static final ArchRule userTablesBelongToTheIdentityDomain = classes()
            .that().belongToAnyOf(UserRepository.class, RefreshTokenRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(
                    AuthenticationServiceImpl.class, UserServiceImpl.class, RefreshTokenServiceImpl.class);
}
