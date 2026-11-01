package io.github.vitaliyprogrammer.banking.credit.service;

import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditApplicationResponseDto;
import java.util.List;
import java.util.UUID;

public interface CreditApplicationService {

    /** Scores the application at once: APPROVED, DECLINED, or MANUAL_REVIEW for a person to decide. */
    CreditApplicationResponseDto apply(UUID userId, CreditApplicationRequestDto request);

    List<CreditApplicationResponseDto> getMyApplications(UUID userId);

    /** The reviewers' queue, oldest first. */
    PageResponseDto<CreditApplicationResponseDto> getReviewQueue(int page, int size);

    CreditApplicationResponseDto approve(UUID reviewerId, UUID applicationId, CreditDecisionRequestDto request);

    CreditApplicationResponseDto decline(UUID reviewerId, UUID applicationId, CreditDecisionRequestDto request);
}
