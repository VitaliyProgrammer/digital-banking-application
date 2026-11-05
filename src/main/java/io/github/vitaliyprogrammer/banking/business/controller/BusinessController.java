package io.github.vitaliyprogrammer.banking.business.controller;

import io.github.vitaliyprogrammer.banking.business.dto.request.OpenBusinessRequestDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessIncomeResponseDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessResponseDto;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
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
@RequestMapping("/business")
@RequiredArgsConstructor
@Tag(name = "Sole proprietor", description = "A ФОП account with its card, single tax group and a tax jar")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a sole proprietor's account",
            description = "Registers the profile and opens a business account, a tax jar and a BUSINESS card. "
                    + "Group 3 income is taxed when it arrives: 5% (3% for a VAT payer) goes into the tax jar")
    public BusinessResponseDto open(@CurrentUserId UUID userId, @Valid @RequestBody OpenBusinessRequestDto request) {

        return businessService.open(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my sole proprietor's account",
            description = "The profile, both balances and the income and tax of the current quarter")
    public BusinessResponseDto getBusiness(@CurrentUserId UUID userId) {

        return businessService.getBusiness(userId);
    }

    @GetMapping("/incomes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my recent business incomes", description = "The last 20, each with the tax put aside")
    public List<BusinessIncomeResponseDto> getIncomes(@CurrentUserId UUID userId) {

        return businessService.getRecentIncomes(userId);
    }
}
