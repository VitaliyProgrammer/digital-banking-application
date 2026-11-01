package io.github.vitaliyprogrammer.banking.credit.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.credit.service.CreditApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-applications")
@RequiredArgsConstructor
@Tag(name = "Credit applications", description = "Apply for a credit limit and see the decision")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CreditApplicationController {

    private final CreditApplicationService creditApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Apply for a credit limit",
            description = "Decided at once (APPROVED or DECLINED) or sent to a bank employee (MANUAL_REVIEW). "
                    + "The answer always explains why")
    public CreditApplicationResponseDto apply(@CurrentUserId UUID userId,
                                              @Valid @RequestBody CreditApplicationRequestDto request) {

        return creditApplicationService.apply(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my credit applications",
            description = "All your applications, newest first, with their decisions")
    public List<CreditApplicationResponseDto> getMyApplications(@CurrentUserId UUID userId) {

        return creditApplicationService.getMyApplications(userId);
    }
}
