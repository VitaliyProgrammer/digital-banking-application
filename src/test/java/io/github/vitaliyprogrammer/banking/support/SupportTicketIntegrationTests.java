package io.github.vitaliyprogrammer.banking.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenTicketRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TicketReplyRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketMessageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.TicketStatus;
import io.github.vitaliyprogrammer.banking.exception.InvalidTicketStateException;
import io.github.vitaliyprogrammer.banking.exception.NotTicketAssigneeException;
import io.github.vitaliyprogrammer.banking.exception.OwnTicketException;
import io.github.vitaliyprogrammer.banking.exception.TicketNotFoundException;
import io.github.vitaliyprogrammer.banking.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.SupportTicketService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proof tests for phase 9.3. A ticket goes OPEN → IN_PROGRESS → RESOLVED; exactly one of several employees
 * taking it at once wins; only that employee answers, resolves and sees the client's accounts;
 * a staff answer becomes an event for the client's notification; clients see only their own tickets.
 */
@AutoConfigureMockMvc
class SupportTicketIntegrationTests extends IntegrationTest {

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private UUID clientId;

    private UUID ticketId;

    @BeforeEach
    void clientAsksAQuestion() {
        clientId = createUser();
        ticketId = supportTicketService.open(clientId,
                new OpenTicketRequestDto("Не дійшов переказ", "Переказав 500 грн учора, отримувач не бачить")).ticket().id();
    }

    @Test
    void aTicketGoesThroughTheQueueToAnAnswerAndIsResolvedOnce() {
        assertThat(supportTicketService.getTickets(TicketStatus.OPEN, 0, 100).content())
                .extracting(TicketResponseDto::id).contains(ticketId);

        UUID support = createStaff("SUPPORT");
        supportTicketService.take(support, ticketId);
        supportTicketService.replyAsStaff(support, ticketId,
                new TicketReplyRequestDto("Переказ у дорозі, зарахування до 15:00"));
        supportTicketService.replyAsClient(clientId, ticketId, new TicketReplyRequestDto("Дійшло, дякую!"));
        supportTicketService.resolve(support, ticketId);

        TicketDetailsResponseDto ticket = supportTicketService.getMyTicket(clientId, ticketId);
        assertThat(ticket.ticket().status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.messages()).extracting(TicketMessageResponseDto::fromStaff)
                .containsExactly(false, true, false);

        // The staff answer and the resolution are events for the client's notifications.
        assertThat(jdbcTemplate.queryForList(
                "SELECT event_type FROM outbox_events WHERE aggregate_id = ? ORDER BY created_at", String.class, ticketId))
                .containsExactly("support.ticket.replied", "support.ticket.resolved");

        // Resolved is final: no more messages, no second resolution.
        assertThatThrownBy(() -> supportTicketService.replyAsClient(clientId, ticketId,
                new TicketReplyRequestDto("І ще одне питання")))
                .isInstanceOf(InvalidTicketStateException.class);
        assertThatThrownBy(() -> supportTicketService.resolve(support, ticketId))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void ofSeveralEmployeesTakingTheSameTicketAtOnceExactlyOneWins() throws Exception {
        List<UUID> employees = List.of(createStaff("SUPPORT"), createStaff("SUPPORT"), createStaff("SUPPORT"),
                createStaff("ADMIN"), createStaff("SUPPORT"));

        List<Boolean> results = runConcurrently(employees.size(), i -> () -> {
            try {
                supportTicketService.take(employees.get(i), ticketId);
                return true;
            } catch (InvalidTicketStateException e) {
                return false;
            }
        });

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(1);
        UUID assignee = supportTicketService.getTicket(ticketId).ticket().assigneeId();
        assertThat(employees).contains(assignee);
    }

    @Test
    void onlyTheEmployeeWhoTookTheTicketAnswersItAndSeesTheClientsAccounts() {
        accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH));
        UUID assignee = createStaff("SUPPORT");
        UUID colleague = createStaff("SUPPORT");
        supportTicketService.take(assignee, ticketId);

        assertThatThrownBy(() -> supportTicketService.replyAsStaff(colleague, ticketId,
                new TicketReplyRequestDto("Я теж допоможу")))
                .isInstanceOf(NotTicketAssigneeException.class);
        assertThatThrownBy(() -> supportTicketService.getClientView(colleague, ticketId))
                .isInstanceOf(NotTicketAssigneeException.class);

        assertThat(supportTicketService.getClientView(assignee, ticketId).accounts()).hasSize(1);
    }

    @Test
    void anEmployeeNeverHandlesTheirOwnTicketAndClientsSeeOnlyTheirOwn() {
        jdbcTemplate.update("UPDATE users SET role = 'SUPPORT' WHERE id = ?", clientId);
        assertThatThrownBy(() -> supportTicketService.take(clientId, ticketId))
                .isInstanceOf(OwnTicketException.class);

        UUID otherClient = createUser();
        assertThatThrownBy(() -> supportTicketService.getMyTicket(otherClient, ticketId))
                .isInstanceOf(TicketNotFoundException.class);
        assertThatThrownBy(() -> supportTicketService.replyAsClient(otherClient, ticketId,
                new TicketReplyRequestDto("Це не моє, але напишу")))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void onlySupportAndAdminSeeTheQueue() throws Exception {
        mockMvc.perform(get("/admin/support/tickets").header("Authorization", bearer(clientId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/support/tickets").header("Authorization", bearer(createStaff("SUPPORT"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/support/tickets").header("Authorization", bearer(createStaff("ADMIN"))))
                .andExpect(status().isOk());
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
