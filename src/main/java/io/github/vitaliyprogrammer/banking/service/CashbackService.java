package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.response.CashbackResponseDto;
import java.time.YearMonth;
import java.util.UUID;

public interface CashbackService {

    CashbackResponseDto getCashback(UUID userId, YearMonth period);

    /** Pays every account its cashback for the month; a month already paid is skipped. Returns payouts made. */
    int payCashback(YearMonth period);
}
