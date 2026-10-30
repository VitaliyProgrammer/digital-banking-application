package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.CreditProperties;
import io.github.vitaliyprogrammer.banking.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ScoringResultDto;
import io.github.vitaliyprogrammer.banking.entity.CreditApplication;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.event.CreditDecisionMessage;
import io.github.vitaliyprogrammer.banking.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.exception.CreditApplicationAlreadyDecidedException;
import io.github.vitaliyprogrammer.banking.exception.CreditApplicationExistsException;
import io.github.vitaliyprogrammer.banking.exception.CreditApplicationNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.CurrentAccountRequiredException;
import io.github.vitaliyprogrammer.banking.exception.InvalidCreditDecisionException;
import io.github.vitaliyprogrammer.banking.exception.OwnCreditApplicationException;
import io.github.vitaliyprogrammer.banking.repository.CreditApplicationRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.service.OutboxService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collects the facts from the ledger, lets {@link CreditScoring} decide, and records the decision.
 * A person only ever decides what the rules left in MANUAL_REVIEW, and every manual decision is final and signed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditApplicationServiceImpl implements CreditApplicationService {

    /** "Open" = waiting for a person, or already gave a limit: a client has at most one of those. */
    private static final List<CreditApplicationStatus> OPEN =
            List.of(CreditApplicationStatus.MANUAL_REVIEW, CreditApplicationStatus.APPROVED);

    private final CreditApplicationRepository creditApplicationRepository;

    private final AccountService accountService;

    /** Same transaction as the decision: an approved limit without a credit line (or the reverse) cannot exist. */
    private final CreditLineService creditLineService;

    private final OutboxService outboxService;

    private final CreditScoring creditScoring;

    private final CreditProperties properties;

    private final Clock clock;

    @Override
    @Transactional
    public CreditApplicationResponseDto apply(UUID userId, CreditApplicationRequestDto request) {

        if (creditApplicationRepository.existsByUserIdAndStatusIn(userId, OPEN)) {
            throw new CreditApplicationExistsException();
        }

        AccountResponseDto account = accountService.getAccounts(userId).stream()
                .filter(candidate -> candidate.type() == AccountType.CLIENT && candidate.currency() == Currency.UAH)
                .findFirst()
                .orElseThrow(CurrentAccountRequiredException::new);

        Instant now = clock.instant();
        long accountAgeDays = Duration.between(account.createdAt(), now).toDays();
        BigDecimal income = accountService.getIncome(
                userId, account.id(), now.minus(Duration.ofDays(properties.incomeWindowDays())));

        ScoringResultDto result = creditScoring.score(accountAgeDays, income, request.requestedLimit());
        log.info("Credit application of {}: {} for {} (affordable {})",
                userId, result.status(), request.requestedLimit(), result.affordableLimit());

        CreditApplication application;
        try {
            application = creditApplicationRepository.saveAndFlush(
                    CreditApplication.scored(userId, request.requestedLimit(), result, now));
        } catch (DataIntegrityViolationException e) {
            // Two parallel submissions both passed the check above; the partial unique index lets one in.
            throw new CreditApplicationExistsException();
        }

        if (application.getStatus() == CreditApplicationStatus.APPROVED) {
            creditLineService.open(userId, application.getApprovedLimit());
        }
        if (!application.isUnderReview()) {
            announceDecision(application);
        }

        return CreditApplicationResponseDto.from(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditApplicationResponseDto> getMyApplications(UUID userId) {

        return creditApplicationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CreditApplicationResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CreditApplicationResponseDto> getReviewQueue(int page, int size) {

        return PageResponseDto.from(
                creditApplicationRepository.findAllByStatusOrderByCreatedAt(
                        CreditApplicationStatus.MANUAL_REVIEW, PageRequest.of(page, size)),
                CreditApplicationResponseDto::from);
    }

    @Override
    @Transactional
    public CreditApplicationResponseDto approve(UUID reviewerId, UUID applicationId, CreditDecisionRequestDto request) {

        if (request.approvedLimit() == null) {
            throw new InvalidCreditDecisionException("approvedLimit is required to approve");
        }
        if (request.approvedLimit().compareTo(properties.maxLimit()) > 0) {
            throw new InvalidCreditDecisionException("approvedLimit must not exceed " + properties.maxLimit());
        }

        CreditApplication application = lockForDecision(reviewerId, applicationId);
        application.approve(reviewerId, request.approvedLimit(), request.comment().strip(), clock.instant());
        creditLineService.open(application.getUserId(), request.approvedLimit());
        announceDecision(application);

        log.info("Credit application {} approved by {} for {}", applicationId, reviewerId, request.approvedLimit());
        return CreditApplicationResponseDto.from(application);
    }

    @Override
    @Transactional
    public CreditApplicationResponseDto decline(UUID reviewerId, UUID applicationId, CreditDecisionRequestDto request) {

        CreditApplication application = lockForDecision(reviewerId, applicationId);
        application.decline(reviewerId, request.comment().strip(), clock.instant());
        announceDecision(application);

        log.info("Credit application {} declined by {}", applicationId, reviewerId);
        return CreditApplicationResponseDto.from(application);
    }

    /** The client learns the answer from a notification, not by polling GET /credit-applications. */
    private void announceDecision(CreditApplication application) {

        outboxService.record(OutboxEventType.CREDIT_APPLICATION_DECIDED, application.getId(),
                CreditDecisionMessage.from(application));
    }

    /**
     * The row stays locked until the decision commits. A second reviewer waits here, then finds the application
     * already decided and gets 409 instead of overwriting the first decision.
     */
    private CreditApplication lockForDecision(UUID reviewerId, UUID applicationId) {

        CreditApplication application = creditApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(CreditApplicationNotFoundException::new);

        if (application.getUserId().equals(reviewerId)) {
            throw new OwnCreditApplicationException();
        }
        if (!application.isUnderReview()) {
            throw new CreditApplicationAlreadyDecidedException();
        }

        return application;
    }
}
