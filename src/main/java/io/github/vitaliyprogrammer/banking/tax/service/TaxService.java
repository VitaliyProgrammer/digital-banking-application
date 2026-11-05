package io.github.vitaliyprogrammer.banking.tax.service;

import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.tax.dto.response.TaxPeriodResponseDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxService {

    /** Every tax period since the business was opened, newest first, as of today in Kyiv. */
    List<TaxPeriodResponseDto> getPeriods(UUID ownerId);

    /**
     * Pays what is left of a period now: from the tax jar, or from the business account if the jar is short.
     * A group 3 quarter that is still going on may be paid early for the tax so far.
     */
    PaymentResponseDto payNow(UUID ownerId, String period);

    /**
     * The daily job: a reminder a few days before each due date, and with auto-pay on, the payment on the due day
     * (or one "not enough money" notice). Safe to run twice for the same day. Returns how many periods it paid.
     */
    int runDaily(LocalDate today);
}
