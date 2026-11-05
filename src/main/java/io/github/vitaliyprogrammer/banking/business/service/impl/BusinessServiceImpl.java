package io.github.vitaliyprogrammer.banking.business.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.business.configuration.BusinessProperties;
import io.github.vitaliyprogrammer.banking.business.dto.request.OpenBusinessRequestDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessIncomeResponseDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessResponseDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessTaxProfileDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.IncomeTaxDto;
import io.github.vitaliyprogrammer.banking.business.entity.Business;
import io.github.vitaliyprogrammer.banking.business.entity.BusinessIncome;
import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import io.github.vitaliyprogrammer.banking.business.exception.BusinessAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.business.exception.BusinessNotFoundException;
import io.github.vitaliyprogrammer.banking.business.exception.VatPayerGroupException;
import io.github.vitaliyprogrammer.banking.business.repository.BusinessIncomeRepository;
import io.github.vitaliyprogrammer.banking.business.repository.BusinessRepository;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sole proprietors (ФОП). Opening one is a single transaction: the profile, a business account, a tax jar and a
 * BUSINESS card, so there is never a profile without its accounts. Group 3 income is taxed the moment it arrives:
 * the payment module asks for the tax before it posts and puts it into the tax jar in the same ledger transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int MONTHS_IN_QUARTER = 3;

    private final BusinessRepository businessRepository;

    private final BusinessIncomeRepository businessIncomeRepository;

    private final AccountService accountService;

    private final CardService cardService;

    private final BusinessProperties properties;

    private final Clock clock;

    @Override
    @Transactional
    public BusinessResponseDto open(UUID ownerId, OpenBusinessRequestDto request) {

        if (request.vatPayer() && request.taxGroup() != TaxGroup.GROUP_3) {
            throw new VatPayerGroupException();
        }
        if (businessRepository.existsByOwnerIdOrTaxId(ownerId, request.taxId())) {
            throw new BusinessAlreadyExistsException();
        }

        AccountResponseDto businessAccount = accountService.openBusinessAccount(ownerId);
        AccountResponseDto taxReserve = accountService.openTaxReserveAccount(ownerId);
        cardService.issue(ownerId, new IssueCardRequestDto(businessAccount.id(), CardType.BUSINESS));

        Business business;
        try {
            business = businessRepository.saveAndFlush(Business.register(ownerId, request.name().strip(),
                    request.taxId(), request.kvedCodes(), request.taxGroup(), request.vatPayer(),
                    request.registeredOn(), businessAccount.id(), taxReserve.id(), clock.instant()));
        } catch (DataIntegrityViolationException e) {
            // The same tax number registered by two clients at once: the unique index lets only one in.
            throw new BusinessAlreadyExistsException();
        }

        log.info("Sole proprietor {} opened for {}: {}", business.getId(), ownerId, business.getTaxGroup());
        return toResponse(business);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessResponseDto getBusiness(UUID ownerId) {

        return toResponse(findBusiness(ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessIncomeResponseDto> getRecentIncomes(UUID ownerId) {

        return businessIncomeRepository.findTop20ByBusinessIdOrderByCreatedAtDesc(findBusiness(ownerId).getId()).stream()
                .map(BusinessIncomeResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public BusinessResponseDto setAutoPayTax(UUID ownerId, boolean enabled) {

        Business business = findBusiness(ownerId);
        business.setAutoPayTax(enabled);
        log.info("Tax auto-pay of {} turned {}", business.getId(), enabled ? "on" : "off");
        return toResponse(business);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessTaxProfileDto getTaxProfile(UUID ownerId) {

        return toTaxProfile(findBusiness(ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessTaxProfileDto> getTaxProfiles() {

        return businessRepository.findAll().stream().map(this::toTaxProfile).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTaxPutAside(UUID businessId, Instant from, Instant to) {

        return money(businessIncomeRepository.sumTaxBetween(businessId, from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncomeTaxDto> taxOnIncome(AccountReferenceDto recipient, UUID payerId, BigDecimal amount) {

        if (recipient.type() != AccountType.BUSINESS || recipient.ownerId().equals(payerId)) {
            return Optional.empty();
        }

        return businessRepository.findByBusinessAccountId(recipient.id())
                .map(business -> new IncomeTaxDto(business.getId(), business.getTaxReserveAccountId(),
                        amount.multiply(ratePercent(business)).divide(HUNDRED, 2, RoundingMode.HALF_UP)));
    }

    @Override
    @Transactional
    public void recordIncome(IncomeTaxDto incomeTax, UUID paymentId, BigDecimal amount) {

        businessIncomeRepository.save(
                BusinessIncome.of(incomeTax.businessId(), paymentId, amount, incomeTax.tax(), clock.instant()));
    }

    private BusinessTaxProfileDto toTaxProfile(Business business) {

        return new BusinessTaxProfileDto(business.getId(), business.getOwnerId(), business.getTaxGroup(),
                monthlyTax(business), business.getBusinessAccountId(), business.getTaxReserveAccountId(),
                business.isAutoPayTax(), business.getCreatedAt());
    }

    private Business findBusiness(UUID ownerId) {

        return businessRepository.findByOwnerId(ownerId).orElseThrow(BusinessNotFoundException::new);
    }

    private BigDecimal ratePercent(Business business) {

        if (!business.getTaxGroup().paysShareOfIncome()) {
            return BigDecimal.ZERO;
        }
        return business.isVatPayer() ? properties.group3VatRatePercent() : properties.group3RatePercent();
    }

    private BigDecimal monthlyTax(Business business) {

        return switch (business.getTaxGroup()) {
            case GROUP_1 -> properties.group1MonthlyTax();
            case GROUP_2 -> properties.group2MonthlyTax();
            case GROUP_3 -> BigDecimal.ZERO;
        };
    }

    private BusinessResponseDto toResponse(Business business) {

        LocalDate today = LocalDate.now(clock.withZone(KYIV));
        int quarter = (today.getMonthValue() - 1) / MONTHS_IN_QUARTER + 1;
        Instant quarterStart = LocalDate.of(today.getYear(), (quarter - 1) * MONTHS_IN_QUARTER + 1, 1)
                .atStartOfDay(KYIV).toInstant();

        BigDecimal quarterTax = business.getTaxGroup().paysShareOfIncome()
                ? businessIncomeRepository.sumTaxSince(business.getId(), quarterStart)
                : monthlyTax(business).multiply(BigDecimal.valueOf(MONTHS_IN_QUARTER));

        AccountResponseDto account = accountService.getAccount(business.getOwnerId(), business.getBusinessAccountId());
        AccountResponseDto reserve = accountService.getAccount(business.getOwnerId(), business.getTaxReserveAccountId());

        return new BusinessResponseDto(
                business.getId(),
                business.getName(),
                business.getTaxId(),
                business.getKvedCodeList(),
                business.getTaxGroup(),
                business.isVatPayer(),
                business.getRegisteredOn(),
                account.id(),
                account.number(),
                account.balance(),
                reserve.id(),
                business.isAutoPayTax(),
                reserve.balance(),
                ratePercent(business),
                monthlyTax(business),
                today.getYear() + "-Q" + quarter,
                money(businessIncomeRepository.sumIncomeSince(business.getId(), quarterStart)),
                money(quarterTax));
    }

    private static BigDecimal money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
