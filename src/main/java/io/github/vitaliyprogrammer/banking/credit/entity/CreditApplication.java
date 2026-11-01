package io.github.vitaliyprogrammer.banking.credit.entity;

import io.github.vitaliyprogrammer.banking.credit.dto.response.ScoringResultDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.credit.entity.status.DecisionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * A request for a credit limit and its decision. Only MANUAL_REVIEW can still change: a person approves
 * or declines it once, and the decision is then final, with its author, time and comment.
 */
@Entity
@Table(name = "credit_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditApplication {

    private static final String REASON_SEPARATOR = "\n";

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal requestedLimit;

    @Column(precision = 19, scale = 2)
    private BigDecimal affordableLimit;

    @Column(precision = 19, scale = 2)
    private BigDecimal approvedLimit;

    @Enumerated(EnumType.STRING)
    private CreditApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private DecisionSource decisionSource;

    private String reasons;

    private UUID decidedBy;

    private String decisionComment;

    private Instant createdAt;

    private Instant decidedAt;

    /** A new application with the scoring's verdict; APPROVED and DECLINED are final at once. */
    public static CreditApplication scored(UUID userId, BigDecimal requestedLimit, ScoringResultDto result,
                                           Instant now) {
        CreditApplication application = new CreditApplication();
        application.userId = userId;
        application.requestedLimit = requestedLimit;
        application.affordableLimit = result.affordableLimit();
        application.approvedLimit = result.approvedLimit();
        application.status = result.status();
        application.reasons = String.join(REASON_SEPARATOR, result.reasons());
        application.createdAt = now;

        if (result.status() != CreditApplicationStatus.MANUAL_REVIEW) {
            application.decisionSource = DecisionSource.AUTO;
            application.decidedAt = now;
        }

        return application;
    }

    /** A person approves, possibly a smaller limit than asked for. */
    public void approve(UUID reviewerId, BigDecimal limit, String comment, Instant now) {
        requireUnderReview();
        this.status = CreditApplicationStatus.APPROVED;
        this.approvedLimit = limit;
        decide(reviewerId, comment, now);
    }

    public void decline(UUID reviewerId, String comment, Instant now) {
        requireUnderReview();
        this.status = CreditApplicationStatus.DECLINED;
        decide(reviewerId, comment, now);
    }

    public boolean isUnderReview() {
        return status == CreditApplicationStatus.MANUAL_REVIEW;
    }

    public List<String> reasonList() {
        return List.of(reasons.split(REASON_SEPARATOR));
    }

    private void decide(UUID reviewerId, String comment, Instant now) {
        this.decisionSource = DecisionSource.MANUAL;
        this.decidedBy = reviewerId;
        this.decisionComment = comment;
        this.decidedAt = now;
    }

    private void requireUnderReview() {
        if (!isUnderReview()) {
            throw new IllegalStateException("Credit application " + id + " is already " + status);
        }
    }
}
