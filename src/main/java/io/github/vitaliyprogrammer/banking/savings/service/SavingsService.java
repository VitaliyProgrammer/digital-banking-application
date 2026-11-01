package io.github.vitaliyprogrammer.banking.savings.service;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.savings.dto.response.SavingsResponseDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface SavingsService {

    SavingsResponseDto open(UUID userId, OpenAccountRequestDto request);

    List<SavingsResponseDto> getSavings(UUID userId);

    /** Accrues one day of interest for every savings account; a day already accrued is skipped. Returns new rows. */
    int accrueInterest(LocalDate day);

    /** Pays the interest accrued up to the end of the month; a month already paid is skipped. Returns payouts made. */
    int payInterest(YearMonth period);
}
