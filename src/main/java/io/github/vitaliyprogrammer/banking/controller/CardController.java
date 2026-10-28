package io.github.vitaliyprogrammer.banking.controller;

import io.github.vitaliyprogrammer.banking.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.dto.request.ChangePinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.VerifyPinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardResponseDto;
import io.github.vitaliyprogrammer.banking.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Payment cards: issue, details, block, PIN")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue a card",
            description = "Issues a PAYROLL, STUDENT or VIRTUAL card for one of your accounts. "
                    + "One open card of each type per account")
    public CardResponseDto issueCard(@CurrentUserId UUID userId,
                                     @Valid @RequestBody IssueCardRequestDto request) {

        return cardService.issue(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my cards",
            description = "All cards of the authenticated client with masked numbers")
    public List<CardResponseDto> getMyCards(@CurrentUserId UUID userId) {

        return cardService.getCards(userId);
    }

    @GetMapping("/{cardId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get card",
            description = "One card with a masked number. Someone else's card returns 404")
    public CardResponseDto getCard(@CurrentUserId UUID userId,
                                   @PathVariable UUID cardId) {

        return cardService.getCard(userId, cardId);
    }

    @GetMapping("/{cardId}/details")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Show card details",
            description = "Full number, expiry and CVV, e.g. for an online payment. The CVV is derived, not stored")
    public CardDetailsResponseDto getCardDetails(@CurrentUserId UUID userId,
                                                 @PathVariable UUID cardId) {

        return cardService.getCardDetails(userId, cardId);
    }

    @PostMapping("/{cardId}/block")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Block card",
            description = "Temporarily blocks an active card, e.g. when it is lost. Can be unblocked later")
    public CardResponseDto blockCard(@CurrentUserId UUID userId,
                                     @PathVariable UUID cardId) {

        return cardService.block(userId, cardId);
    }

    @PostMapping("/{cardId}/unblock")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unblock card",
            description = "Unblocks a card you blocked yourself. A card locked by wrong PINs needs support")
    public CardResponseDto unblockCard(@CurrentUserId UUID userId,
                                       @PathVariable UUID cardId) {

        return cardService.unblock(userId, cardId);
    }

    @PostMapping("/{cardId}/close")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Close card",
            description = "Closes the card permanently")
    public CardResponseDto closeCard(@CurrentUserId UUID userId,
                                     @PathVariable UUID cardId) {

        return cardService.close(userId, cardId);
    }

    @PutMapping("/{cardId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set or change PIN",
            description = "Sets the first PIN, or changes it when the current PIN is given. "
                    + "Repeated and sequential digits are rejected")
    public void changePin(@CurrentUserId UUID userId,
                          @PathVariable UUID cardId,
                          @Valid @RequestBody ChangePinRequestDto request) {

        cardService.changePin(userId, cardId, request);
    }

    @PostMapping("/{cardId}/pin/verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify PIN",
            description = "Checks the PIN like a terminal would. The third wrong PIN within 24 hours locks the card")
    public void verifyPin(@CurrentUserId UUID userId,
                          @PathVariable UUID cardId,
                          @Valid @RequestBody VerifyPinRequestDto request) {

        cardService.verifyPin(userId, cardId, request);
    }
}
