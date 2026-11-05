package io.github.vitaliyprogrammer.banking.business.service;

import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.business.dto.request.OpenBusinessRequestDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessIncomeResponseDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessResponseDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessTaxProfileDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.IncomeTaxDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessService {

    /** Registers the client as a sole proprietor: the profile, a business account, a tax jar and a BUSINESS card. */
    BusinessResponseDto open(UUID ownerId, OpenBusinessRequestDto request);

    BusinessResponseDto getBusiness(UUID ownerId);

    List<BusinessIncomeResponseDto> getRecentIncomes(UUID ownerId);

    /** The client's consent (or its withdrawal) that the bank pays the tax on its due date by itself. */
    BusinessResponseDto setAutoPayTax(UUID ownerId, boolean enabled);

    /** For the tax module. */
    BusinessTaxProfileDto getTaxProfile(UUID ownerId);

    /** For the tax job: every sole proprietor of the bank. */
    List<BusinessTaxProfileDto> getTaxProfiles();

    /** For the tax module: the group 3 tax put aside from income in [from, to). */
    BigDecimal getTaxPutAside(UUID businessId, Instant from, Instant to);

    /**
     * For the payment module, before it posts: whether money arriving on this account is a sole proprietor's income
     * and how much of it goes into the tax jar. Empty for any other account, and for the owner's own money moved in:
     * that is not income.
     */
    Optional<IncomeTaxDto> taxOnIncome(AccountReferenceDto recipient, UUID payerId, BigDecimal amount);

    /** For the payment module, in the same transaction as the payment: remembers the income and its tax. */
    void recordIncome(IncomeTaxDto incomeTax, UUID paymentId, BigDecimal amount);
}
