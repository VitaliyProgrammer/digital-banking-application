package io.github.vitaliyprogrammer.banking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.audit.dto.request.AuditSearchRequestDto;
import io.github.vitaliyprogrammer.banking.audit.dto.response.AuditEntryResponseDto;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.audit.service.AuditService;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.credit.exception.CreditApplicationAlreadyDecidedException;
import io.github.vitaliyprogrammer.banking.credit.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.identity.entity.status.Role;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.support.dto.request.OpenTicketRequestDto;
import io.github.vitaliyprogrammer.banking.support.service.SupportTicketService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proof tests for phase 9.4. A staff action leaves exactly one entry with who, what and when; an action that was
 * refused leaves none; merely looking at a client's accounts is recorded; the log cannot be rewritten;
 * only ADMIN reads it.
 */
class AuditIntegrationTests extends IntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private CreditApplicationService creditApplicationService;

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aDecisionIsRecordedOnceWithWhoAndWhatAndARefusedOneLeavesNoTrace() {
        UUID application = applicationWaitingForAPerson();
        UUID reviewer = createStaff("SUPPORT");

        creditApplicationService.approve(reviewer, application,
                new CreditDecisionRequestDto(new BigDecimal("10000.00"), "Stable salary"));
        // A second employee is too late: 409, and the log must not claim that they decided anything.
        UUID lateReviewer = createStaff("ADMIN");
        assertThatThrownBy(() -> creditApplicationService.decline(lateReviewer, application,
                new CreditDecisionRequestDto(null, "Too risky")))
                .isInstanceOf(CreditApplicationAlreadyDecidedException.class);

        assertThat(entriesAbout(AuditTarget.CREDIT_APPLICATION, application)).singleElement().satisfies(entry -> {
            assertThat(entry.actorId()).isEqualTo(reviewer);
            assertThat(entry.actorRole()).isEqualTo(Role.SUPPORT);
            assertThat(entry.action()).isEqualTo(AuditAction.CREDIT_APPLICATION_APPROVED);
            assertThat(entry.details().get("from").asString()).isEqualTo("MANUAL_REVIEW");
            assertThat(entry.details().get("to").asString()).isEqualTo("APPROVED");
            assertThat(entry.details().get("approvedLimit").decimalValue()).isEqualByComparingTo("10000.00");
            assertThat(entry.details().get("comment").asString()).isEqualTo("Stable salary");
        });
    }

    @Test
    void lookingAtAClientsAccountsIsRecordedNotOnlyChangingThings() {
        UUID clientId = createUser();
        UUID ticket = supportTicketService.open(clientId,
                new OpenTicketRequestDto("Списали двічі", "Двічі списали за інтернет")).ticket().id();
        UUID support = createStaff("SUPPORT");

        supportTicketService.take(support, ticket);
        supportTicketService.getClientView(support, ticket);

        assertThat(entriesAbout(AuditTarget.SUPPORT_TICKET, ticket)).extracting(AuditEntryResponseDto::action)
                .containsExactly(AuditAction.TICKET_TAKEN);
        assertThat(entriesAbout(AuditTarget.CLIENT, clientId)).singleElement().satisfies(entry -> {
            assertThat(entry.actorId()).isEqualTo(support);
            assertThat(entry.action()).isEqualTo(AuditAction.CLIENT_ACCOUNTS_VIEWED);
            assertThat(entry.details().get("ticketId").asString()).isEqualTo(ticket.toString());
        });
    }

    @Test
    void theAuditLogCannotBeRewrittenEvenWithPlainSql() {
        UUID clientId = createUser();
        UUID ticket = supportTicketService.open(clientId,
                new OpenTicketRequestDto("Питання", "Як змінити PIN?")).ticket().id();
        supportTicketService.take(createStaff("SUPPORT"), ticket);
        UUID entry = entriesAbout(AuditTarget.SUPPORT_TICKET, ticket).getFirst().id();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit_log SET actor_id = ? WHERE id = ?", createStaff("ADMIN"), entry))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_log WHERE id = ?", entry))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void onlyAnAdminReadsTheAuditLog() throws Exception {
        mockMvc.perform(get("/admin/audit").header("Authorization", bearer(createStaff("SUPPORT"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit").header("Authorization", bearer(createStaff("ADMIN"))))
                .andExpect(status().isOk());
    }

    /** 60 000 of income gives an affordable limit of 10 000; asking for 12 000 sends it to a person. */
    private UUID applicationWaitingForAPerson() {
        UUID clientId = createUser();
        UUID account = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        jdbcTemplate.update("UPDATE accounts SET created_at = now() - interval '60 days' WHERE id = ?", account);
        paymentService.topUp(clientId, UUID.randomUUID().toString(),
                new TopUpRequestDto(account, new BigDecimal("60000.00")));
        return creditApplicationService.apply(clientId,
                new CreditApplicationRequestDto(new BigDecimal("12000.00"))).id();
    }

    private List<AuditEntryResponseDto> entriesAbout(AuditTarget targetType, UUID targetId) {
        return auditService.search(new AuditSearchRequestDto(null, null, targetType, targetId, null, null), 0, 50)
                .content();
    }

    private UUID createStaff(String role) {
        UUID id = createUser();
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, id);
        return id;
    }

    private String bearer(UUID userId) {
        return "Bearer " + accessTokenService.issue(userRepository.findById(userId).orElseThrow());
    }
}
