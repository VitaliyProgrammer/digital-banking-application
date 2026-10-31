package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.DecisionSource;
import io.github.vitaliyprogrammer.banking.exception.CreditApplicationAlreadyDecidedException;
import io.github.vitaliyprogrammer.banking.exception.CreditApplicationExistsException;
import io.github.vitaliyprogrammer.banking.exception.OwnCreditApplicationException;
import io.github.vitaliyprogrammer.banking.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import io.github.vitaliyprogrammer.banking.service.SavingsService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proof tests for phase 8.1. Income is read from the ledger and moving one's own money does not count,
 * the grey zone waits for a person, a decision is made once and never on one's own application,
 * and only ADMIN and SUPPORT see the review queue.
 */
class CreditApplicationIntegrationTests extends IntegrationTest {

    @Autowired
    private CreditApplicationService creditApplicationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SavingsService savingsService;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private UUID clientId;

    private UUID account;

    /** A client with a two-month-old account and 60 000 of income: affordable limit 10 000. */
    @BeforeEach
    void establishedClient() {
        clientId = createUser();
        account = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        jdbcTemplate.update("UPDATE accounts SET created_at = now() - interval '60 days' WHERE id = ?", account);
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(account, new BigDecimal("60000.00")));
    }

    @Test
    void movingOwnMoneyAroundDoesNotRaiseTheLimit() {
        // 10 000 into savings and back again: two credits to the account's history, but not income.
        UUID jar = savingsService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.depositToSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(account, new BigDecimal("10000.00")));
        paymentService.withdrawFromSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(account, new BigDecimal("10000.00")));

        CreditApplicationResponseDto application = apply("8000.00");

        assertThat(application.affordableLimit()).isEqualByComparingTo("10000.00");
        assertThat(application.status()).isEqualTo(CreditApplicationStatus.APPROVED);
        assertThat(application.decisionSource()).isEqualTo(DecisionSource.AUTO);
    }

    @Test
    void theGreyZoneWaitsForAPersonWhoDecidesOnceWithAComment() {
        CreditApplicationResponseDto application = apply("12000.00");
        assertThat(application.status()).isEqualTo(CreditApplicationStatus.MANUAL_REVIEW);
        assertThat(creditApplicationService.getReviewQueue(0, 100).content())
                .extracting(CreditApplicationResponseDto::id).contains(application.id());

        UUID reviewer = createStaff("SUPPORT");
        CreditApplicationResponseDto approved = creditApplicationService.approve(reviewer, application.id(),
                new CreditDecisionRequestDto(new BigDecimal("10000.00"), "Stable salary, approved a bit less"));

        assertThat(approved.status()).isEqualTo(CreditApplicationStatus.APPROVED);
        assertThat(approved.approvedLimit()).isEqualByComparingTo("10000.00");
        assertThat(approved.decisionSource()).isEqualTo(DecisionSource.MANUAL);

        // A second reviewer, a second click: the first decision stands.
        UUID secondReviewer = createStaff("ADMIN");
        assertThatThrownBy(() -> creditApplicationService.decline(secondReviewer, application.id(),
                new CreditDecisionRequestDto(null, "Too risky")))
                .isInstanceOf(CreditApplicationAlreadyDecidedException.class);
        // And with a limit already granted, the client cannot open another application.
        assertThatThrownBy(() -> apply("1000.00")).isInstanceOf(CreditApplicationExistsException.class);
    }

    @Test
    void nobodyDecidesOnTheirOwnApplication() {
        jdbcTemplate.update("UPDATE users SET role = 'SUPPORT' WHERE id = ?", clientId);
        CreditApplicationResponseDto application = apply("12000.00");

        assertThatThrownBy(() -> creditApplicationService.approve(clientId, application.id(),
                new CreditDecisionRequestDto(new BigDecimal("12000.00"), "Looks fine to me")))
                .isInstanceOf(OwnCreditApplicationException.class);
    }

    @Test
    void onlyAdminAndSupportSeeTheReviewQueue() throws Exception {
        mockMvc.perform(get("/admin/credit-applications").header("Authorization", bearer(clientId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/credit-applications").header("Authorization", bearer(createStaff("SUPPORT"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/credit-applications").header("Authorization", bearer(createStaff("ADMIN"))))
                .andExpect(status().isOk());
    }

    private CreditApplicationResponseDto apply(String limit) {
        return creditApplicationService.apply(clientId, new CreditApplicationRequestDto(new BigDecimal(limit)));
    }

    private UUID createStaff(String role) {
        UUID id = createUser();
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, id);
        return id;
    }

    private String bearer(UUID userId) {
        return "Bearer " + accessTokenService.issue(userRepository.findById(userId).orElseThrow());
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
