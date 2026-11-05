package io.github.vitaliyprogrammer.banking.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.business.dto.request.OpenBusinessRequestDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessResponseDto;
import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import io.github.vitaliyprogrammer.banking.business.exception.BusinessAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.business.exception.VatPayerGroupException;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.CardTypeNotForAccountException;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for sole proprietors (ФОП). Opening one gives a business account, a tax jar and a BUSINESS card at
 * once; group 3 income puts its tax into the jar in the same posting (5%, or 3% for a VAT payer); the owner's own
 * money moved in is not income; groups 1 and 2 pay a fixed tax instead; and the tax jar cannot be spent.
 */
class BusinessIntegrationTests extends IntegrationTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CardService cardService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void openingGivesAnAccountATaxJarAndABusinessCard() {
        UUID ownerId = createUser();

        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3, false);

        assertThat(business.taxGroup()).isEqualTo(TaxGroup.GROUP_3);
        assertThat(business.kvedCodes()).containsExactly("62.01", "63.11");
        assertThat(business.businessBalance()).isEqualByComparingTo("0.00");
        assertThat(business.taxReserve()).isEqualByComparingTo("0.00");
        assertThat(cardService.getCards(ownerId)).singleElement().satisfies(card -> {
            assertThat(card.type()).isEqualTo(CardType.BUSINESS);
            assertThat(card.accountId()).isEqualTo(business.businessAccountId());
        });
        assertThatThrownBy(() -> open(ownerId, TaxGroup.GROUP_2, false))
                .isInstanceOf(BusinessAlreadyExistsException.class);
    }

    @Test
    void onlyTheThirdGroupMayPayVat() {
        assertThatThrownBy(() -> open(createUser(), TaxGroup.GROUP_2, true)).isInstanceOf(VatPayerGroupException.class);
    }

    @Test
    void thirdGroupPutsFivePercentOfEveryIncomeIntoTheTaxJar() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3, false);

        // A customer pays from another bank, then another client of this bank pays by IBAN.
        paymentService.topUp(ownerId, newKey(), new TopUpRequestDto(business.businessAccountId(), new BigDecimal("10000.00")));
        UUID customerId = createUser();
        UUID customerAccount = accountWithMoney(customerId, "5000.00");
        paymentService.transfer(customerId, newKey(), new TransferRequestDto(
                customerAccount, business.businessAccountNumber(), new BigDecimal("2000.00"), "За розробку сайту"));

        BusinessResponseDto after = businessService.getBusiness(ownerId);
        assertThat(after.businessBalance()).isEqualByComparingTo("11400.00");   // 9 500 + 1 900
        assertThat(after.taxReserve()).isEqualByComparingTo("600.00");         // 500 + 100
        assertThat(after.quarterIncome()).isEqualByComparingTo("12000.00");
        assertThat(after.quarterTax()).isEqualByComparingTo("600.00");
        assertThat(businessService.getRecentIncomes(ownerId)).hasSize(2);
    }

    @Test
    void aVatPayerPutsThreePercentAside() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3, true);

        paymentService.topUp(ownerId, newKey(), new TopUpRequestDto(business.businessAccountId(), new BigDecimal("10000.00")));

        assertThat(businessService.getBusiness(ownerId).taxReserve()).isEqualByComparingTo("300.00");
    }

    @Test
    void theOwnersOwnMoneyMovedInIsNotIncome() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3, false);
        UUID personalAccount = accountWithMoney(ownerId, "5000.00");

        paymentService.transfer(ownerId, newKey(), new TransferRequestDto(
                personalAccount, business.businessAccountNumber(), new BigDecimal("1000.00"), null));

        BusinessResponseDto after = businessService.getBusiness(ownerId);
        assertThat(after.businessBalance()).isEqualByComparingTo("1000.00");
        assertThat(after.taxReserve()).isEqualByComparingTo("0.00");
        assertThat(after.quarterIncome()).isEqualByComparingTo("0.00");
    }

    @Test
    void fixedTaxGroupsPutNothingAsideButStillCountIncome() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_1, false);

        paymentService.topUp(ownerId, newKey(), new TopUpRequestDto(business.businessAccountId(), new BigDecimal("10000.00")));

        BusinessResponseDto after = businessService.getBusiness(ownerId);
        assertThat(after.businessBalance()).isEqualByComparingTo("10000.00");
        assertThat(after.taxReserve()).isEqualByComparingTo("0.00");
        assertThat(after.quarterIncome()).isEqualByComparingTo("10000.00");
        assertThat(after.quarterTax()).isEqualByComparingTo("908.40");   // 3 × 302.80 a month
    }

    @Test
    void theTaxJarCannotBeSpentAndCardsStayOnTheirKindOfAccount() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3, false);
        paymentService.topUp(ownerId, newKey(), new TopUpRequestDto(business.businessAccountId(), new BigDecimal("10000.00")));
        UUID personalAccount = accountService.open(ownerId, new OpenAccountRequestDto(Currency.UAH)).id();
        String someIban = accountService.getAccount(ownerId, personalAccount).number();

        assertThatThrownBy(() -> paymentService.transfer(ownerId, newKey(), new TransferRequestDto(
                business.taxReserveAccountId(), someIban, new BigDecimal("100.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> cardService.issue(ownerId,
                new IssueCardRequestDto(business.taxReserveAccountId(), CardType.UNIVERSAL)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> cardService.issue(ownerId,
                new IssueCardRequestDto(business.businessAccountId(), CardType.UNIVERSAL)))
                .isInstanceOf(CardTypeNotForAccountException.class);
        assertThatThrownBy(() -> cardService.issue(ownerId,
                new IssueCardRequestDto(personalAccount, CardType.BUSINESS)))
                .isInstanceOf(CardTypeNotForAccountException.class);
    }

    private BusinessResponseDto open(UUID ownerId, TaxGroup group, boolean vatPayer) {
        return businessService.open(ownerId, new OpenBusinessRequestDto("ФОП Демо Олена", uniqueTaxId(),
                List.of("62.01", "63.11"), group, vatPayer, LocalDate.of(2024, 3, 1)));
    }

    private UUID accountWithMoney(UUID userId, String amount) {
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(userId, newKey(), new TopUpRequestDto(account, new BigDecimal(amount)));
        return account;
    }

    /** Tax numbers are unique across the bank, and all tests share one database. */
    private static String uniqueTaxId() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
