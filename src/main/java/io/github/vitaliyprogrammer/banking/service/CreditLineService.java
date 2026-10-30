package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.response.CreditLineResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreditLineService {

    /** Called in the transaction that approves an application: the credit account and the CREDIT card. */
    void open(UUID ownerId, BigDecimal creditLimit);

    /** The credit line as of today in Kyiv. */
    CreditLineResponseDto getCreditLine(UUID ownerId);

    /**
     * The credit line as of a given Kyiv day: which statement is the last closed one, and what is left of it.
     * A job checking a due date (and a test) asks for a day other than today.
     */
    CreditLineResponseDto getCreditLine(UUID ownerId, LocalDate today);
}
