package io.github.vitaliyprogrammer.banking;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.vitaliyprogrammer.banking.entity.Account;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.repository.BankRateRepository;
import io.github.vitaliyprogrammer.banking.repository.CardRepository;
import io.github.vitaliyprogrammer.banking.repository.CashbackAccrualRepository;
import io.github.vitaliyprogrammer.banking.repository.CashbackPayoutRepository;
import io.github.vitaliyprogrammer.banking.repository.CreditApplicationRepository;
import io.github.vitaliyprogrammer.banking.repository.ExchangeQuoteRepository;
import io.github.vitaliyprogrammer.banking.repository.InterestAccrualRepository;
import io.github.vitaliyprogrammer.banking.repository.InterestPayoutRepository;
import io.github.vitaliyprogrammer.banking.repository.LedgerEntryRepository;
import io.github.vitaliyprogrammer.banking.repository.LedgerTransactionRepository;
import io.github.vitaliyprogrammer.banking.repository.OfficialRateRepository;
import io.github.vitaliyprogrammer.banking.repository.PaymentRepository;
import io.github.vitaliyprogrammer.banking.repository.ProviderRepository;
import io.github.vitaliyprogrammer.banking.repository.ReceiptRepository;
import io.github.vitaliyprogrammer.banking.repository.RefreshTokenRepository;
import io.github.vitaliyprogrammer.banking.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.service.impl.AccountServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.AuthenticationServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.BillPaymentProcessor;
import io.github.vitaliyprogrammer.banking.service.impl.CardServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.CashbackServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.CreditApplicationServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.ExchangeQuoteServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.ExchangeRateServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.HttpProviderClient;
import io.github.vitaliyprogrammer.banking.service.impl.LedgerServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.NbuRateClient;
import io.github.vitaliyprogrammer.banking.service.impl.PaymentServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.ReceiptServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.RefreshTokenServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.SavingsServiceImpl;
import io.github.vitaliyprogrammer.banking.service.impl.UserServiceImpl;
import java.math.BigDecimal;
import org.springframework.web.client.RestClient;

/**
 * Architecture rules checked on every build. The packages are layered (controller → service → repository),
 * and on top of that each domain owns its tables: another domain must go through that domain's service interface.
 * A rule that is broken fails the build with the exact class and line.
 */
@AnalyzeClasses(packages = "io.github.vitaliyprogrammer.banking", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String ROOT = "io.github.vitaliyprogrammer.banking";

    /** Controller → Service → Repository: never backwards and never skipping the service layer. */
    @ArchTest
    static final ArchRule layersDependOnlyDownwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(ROOT + ".controller..")
            .layer("Service").definedBy(ROOT + ".service..")
            .layer("Repository").definedBy(ROOT + ".repository..")
            .layer("Configuration").definedBy(ROOT + ".configuration..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Configuration")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controllersNeverExposeEntities = noClasses()
            .that().resideInAPackage(ROOT + ".controller..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".entity")
            .because("controllers return DTOs; an entity in a response would leak internals like a password hash");

    @ArchTest
    static final ArchRule servicePackageHoldsOnlyInterfaces = classes()
            .that().resideInAPackage(ROOT + ".service")
            .should().beInterfaces()
            .because("implementations live in service.impl");

    /** The money rule: a balance changes only together with a ledger entry, under the ledger's row lock. */
    @ArchTest
    static final ArchRule onlyTheLedgerChangesBalances = noClasses()
            .that().doNotBelongToAnyOf(LedgerServiceImpl.class)
            .should().callMethod(Account.class, "apply", EntryDirection.class, BigDecimal.class)
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

    /** Payments announce what happened; they never call the cashback module directly. */
    @ArchTest
    static final ArchRule paymentsDoNotKnowAboutCashback = noClasses()
            .that().belongToAnyOf(PaymentServiceImpl.class, BillPaymentProcessor.class)
            .should().dependOnClassesThat().belongToAnyOf(CashbackServiceImpl.class);

    @ArchTest
    static final ArchRule receiptTableBelongsToTheReceiptDomain = classes()
            .that().belongToAnyOf(ReceiptRepository.class)
            .should().onlyHaveDependentClassesThat().belongToAnyOf(ReceiptServiceImpl.class);

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
