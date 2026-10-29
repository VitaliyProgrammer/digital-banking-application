package io.github.vitaliyprogrammer.banking.controller;

import io.github.vitaliyprogrammer.banking.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.service.CreditApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN and SUPPORT (see SecurityConfiguration): the people who decide what the rules could not. */
@RestController
@RequestMapping("/admin/credit-applications")
@RequiredArgsConstructor
@Tag(name = "Admin: credit applications", description = "Review queue for credit limits. ADMIN and SUPPORT")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminCreditApplicationController {

    private final CreditApplicationService creditApplicationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get the review queue",
            description = "Applications the rules sent to a person, oldest first, with the reasons")
    public PageResponseDto<CreditApplicationResponseDto> getReviewQueue(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return creditApplicationService.getReviewQueue(page, size);
    }

    @PostMapping("/{applicationId}/approve")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Approve",
            description = "Approve with a limit (may be smaller than requested) and a comment. Final")
    public CreditApplicationResponseDto approve(@CurrentUserId UUID reviewerId,
                                                @PathVariable UUID applicationId,
                                                @Valid @RequestBody CreditDecisionRequestDto request) {

        return creditApplicationService.approve(reviewerId, applicationId, request);
    }

    @PostMapping("/{applicationId}/decline")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Decline",
            description = "Decline with a comment. Final; the client may apply again later")
    public CreditApplicationResponseDto decline(@CurrentUserId UUID reviewerId,
                                                @PathVariable UUID applicationId,
                                                @Valid @RequestBody CreditDecisionRequestDto request) {

        return creditApplicationService.decline(reviewerId, applicationId, request);
    }
}
