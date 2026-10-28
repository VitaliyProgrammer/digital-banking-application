package io.github.vitaliyprogrammer.banking.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.ChangePinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.VerifyPinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exception.CardPinLockedException;
import io.github.vitaliyprogrammer.banking.exception.WrongPinException;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proof tests for phase 3: card secrets never sit in the database in plain text, and PIN guessing is stopped. */
class CardSecurityIntegrationTests extends IntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private AccountService accountService;

    @Test
    void cardSecretsAreNeverStoredInPlainText() {
        UUID ownerId = createUser();
        CardResponseDto card = issueCard(ownerId, CardType.VIRTUAL);
        cardService.changePin(ownerId, card.id(), new ChangePinRequestDto(null, "1357"));

        CardDetailsResponseDto details = cardService.getCardDetails(ownerId, card.id());
        String number = details.number().replace(" ", "");

        assertThat(number).hasSize(16).startsWith("499999");
        assertThat(passesLuhnCheck(number)).as("card number passes the Luhn check").isTrue();
        assertThat(cardService.getCardDetails(ownerId, card.id()).cvv())
                .as("CVV is derived, so it is the same every time").isEqualTo(details.cvv()).matches("\\d{3}");

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM cards WHERE id = ?", card.id());
        assertThat(row.values()).noneMatch(value -> String.valueOf(value).contains(number));
        assertThat(row.keySet()).noneMatch(column -> column.contains("cvv"));
        assertThat(row.get("pin_hash")).asString().hasSize(64).isNotEqualTo("1357");
    }

    @Test
    void parallelWrongPinsLockTheCardAfterThreeAttempts() throws Exception {
        UUID ownerId = createUser();
        UUID cardId = issueCard(ownerId, CardType.PAYROLL).id();
        cardService.changePin(ownerId, cardId, new ChangePinRequestDto(null, "1357"));

        // A thief tries 10 guesses at once, hoping parallel requests slip past the limit.
        List<String> outcomes = runConcurrently(10, i -> () -> {
            try {
                cardService.verifyPin(ownerId, cardId, new VerifyPinRequestDto("2468"));
                return "ACCEPTED";
            } catch (WrongPinException e) {
                return "WRONG";
            } catch (CardPinLockedException e) {
                return "LOCKED";
            }
        });

        assertThat(outcomes).filteredOn("WRONG"::equals).hasSize(2);
        assertThat(outcomes).filteredOn("LOCKED"::equals).hasSize(8);
        assertThat(cardService.getCard(ownerId, cardId).status()).isEqualTo(CardStatus.PIN_LOCKED);

        // Even the right PIN no longer works: the lock is saved in the database, not just counted in Redis.
        assertThatThrownBy(() -> cardService.verifyPin(ownerId, cardId, new VerifyPinRequestDto("1357")))
                .isInstanceOf(CardPinLockedException.class);
    }

    private CardResponseDto issueCard(UUID ownerId, CardType type) {
        UUID accountId = accountService.open(ownerId, new OpenAccountRequestDto(Currency.UAH)).id();
        return cardService.issue(ownerId, new IssueCardRequestDto(accountId, type));
    }

    /** Independent Luhn check, written separately from the production code on purpose. */
    private static boolean passesLuhnCheck(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = number.charAt(number.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                digit = digit * 2 > 9 ? digit * 2 - 9 : digit * 2;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
