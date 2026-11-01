package io.github.vitaliyprogrammer.banking.credit.service;

import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditStatementResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    /** Every closed month of the client's credit line, newest first. */
    List<CreditStatementResponseDto> getStatements(UUID ownerId);

    /**
     * The monthly job, for the given Kyiv day (normally the 1st): settles every statement whose due date has passed,
     * charging interest and late fees, then closes last month. Safe to run again: nothing happens twice.
     * Returns the number of statements closed.
     */
    int closeMonth(LocalDate today);
}
