package io.github.vitaliyprogrammer.banking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exception.CardNotUsableException;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proof test for phase 3: the card-to-card fee follows the sender's card type, and blocked cards cannot pay. */
class CardTransferIntegrationTests extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CardService cardService;

    @Autowired
    private AccountService accountService;

    @Test
    void feeDependsOnSenderCardTypeAndBlockedCardsCannotPay() {
        UUID senderId = createUser();
        UUID senderAccount = accountService.open(senderId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(senderId, newKey(), new TopUpRequestDto(senderAccount, new BigDecimal("5000.00")));
        UUID studentCard = cardService.issue(senderId, new IssueCardRequestDto(senderAccount, CardType.STUDENT)).id();
        UUID virtualCard = cardService.issue(senderId, new IssueCardRequestDto(senderAccount, CardType.VIRTUAL)).id();

        UUID recipientId = createUser();
        UUID recipientAccount = accountService.open(recipientId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID recipientCard = cardService.issue(recipientId,
                new IssueCardRequestDto(recipientAccount, CardType.PAYROLL)).id();
        String recipientNumber = cardService.getCardDetails(recipientId, recipientCard).number();

        PaymentResponseDto fromStudent = paymentService.cardTransfer(senderId, newKey(),
                new CardTransferRequestDto(studentCard, recipientNumber, new BigDecimal("1000.00"), null));
        PaymentResponseDto fromVirtual = paymentService.cardTransfer(senderId, newKey(),
                new CardTransferRequestDto(virtualCard, recipientNumber, new BigDecimal("1000.00"), null));

        assertThat(fromStudent.fee()).isEqualByComparingTo("0.00");     // STUDENT: free
        assertThat(fromVirtual.fee()).isEqualByComparingTo("10.00");    // VIRTUAL: 1% of 1000.00
        assertThat(balanceOf(senderAccount)).isEqualByComparingTo("2990.00");  // 5000 - 1000 - 1000 - 10
        assertThat(balanceOf(recipientAccount)).isEqualByComparingTo("2000.00");

        cardService.block(senderId, virtualCard);

        assertThatThrownBy(() -> paymentService.cardTransfer(senderId, newKey(),
                new CardTransferRequestDto(virtualCard, recipientNumber, new BigDecimal("1.00"), null)))
                .isInstanceOf(CardNotUsableException.class);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
