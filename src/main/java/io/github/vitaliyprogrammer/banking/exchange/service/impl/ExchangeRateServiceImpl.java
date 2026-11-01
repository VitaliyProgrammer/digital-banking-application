package io.github.vitaliyprogrammer.banking.exchange.service.impl;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.audit.service.AuditService;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.configuration.ExchangeRateProperties;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.SetBankRateRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.BankRateChangeResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ConversionDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ExchangeRateResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.NbuRateDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.OfficialRateResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.RateSyncResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.entity.BankRate;
import io.github.vitaliyprogrammer.banking.exchange.entity.OfficialRate;
import io.github.vitaliyprogrammer.banking.exchange.exception.BankRateNotSetException;
import io.github.vitaliyprogrammer.banking.exchange.exception.InvalidBankRateException;
import io.github.vitaliyprogrammer.banking.exchange.exception.InvalidDateRangeException;
import io.github.vitaliyprogrammer.banking.exchange.exception.OfficialRatesUnavailableException;
import io.github.vitaliyprogrammer.banking.exchange.exception.UnsupportedCurrencyException;
import io.github.vitaliyprogrammer.banking.exchange.repository.BankRateRepository;
import io.github.vitaliyprogrammer.banking.exchange.repository.OfficialRateRepository;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeRateService;
import io.github.vitaliyprogrammer.banking.exchange.service.OfficialRateClient;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    /** The NBU sets its rates for Kyiv calendar days, so "today" is the Kyiv date, not the UTC one. */
    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    /** Currencies the bank exchanges. UAH is the base: every rate means "UAH per 1 unit". */
    private static final List<Currency> QUOTED = List.of(Currency.USD, Currency.EUR);

    private static final int DEFAULT_HISTORY_DAYS = 30;

    private static final int MAX_HISTORY_DAYS = 366;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final int CONVERSION_SCALE = 8;

    private final OfficialRateRepository officialRateRepository;

    private final BankRateRepository bankRateRepository;

    private final OfficialRateClient officialRateClient;

    private final UserService userService;

    private final AuditService auditService;

    private final ExchangeRateProperties properties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    /**
     * The HTTP call runs outside any transaction: a slow NBU must not hold a database connection.
     * Each currency is then saved in its own short transaction.
     */
    @Override
    public RateSyncResponseDto syncOfficialRates() {

        int saved = 0;

        for (NbuRateDto nbuRate : officialRateClient.fetchToday()) {
            Optional<Currency> currency = quotedCurrency(nbuRate.cc());
            if (currency.isEmpty()) {
                continue;
            }
            if (nbuRate.rate() == null || nbuRate.rate().signum() <= 0 || nbuRate.exchangedate() == null) {
                log.warn("Skipping an invalid NBU rate: {}", nbuRate);
                continue;
            }
            if (Boolean.TRUE.equals(transactionTemplate.execute(_ -> saveOfficialRate(currency.get(), nbuRate)))) {
                saved++;
            }
        }

        return new RateSyncResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getCurrentRates() {

        LocalDate today = today();
        List<ExchangeRateResponseDto> rates = new ArrayList<>();

        for (Currency currency : QUOTED) {
            officialRateRepository.findFirstByCurrencyOrderByRateDateDesc(currency).ifPresent(official ->
                    rates.add(toRateDto(official, bankRateRepository
                            .findFirstByCurrencyOrderByValidFromDescIdDesc(currency).orElse(null), today)));
        }

        return rates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficialRateResponseDto> getOfficialHistory(Currency currency, LocalDate from, LocalDate to) {

        requireQuoted(currency);
        LocalDate end = to != null ? to : today();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_HISTORY_DAYS - 1);

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_HISTORY_DAYS) {
            throw new InvalidDateRangeException("The range must not exceed " + MAX_HISTORY_DAYS + " days");
        }

        return officialRateRepository.findAllByCurrencyAndRateDateBetweenOrderByRateDate(currency, start, end)
                .stream()
                .map(OfficialRateResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public ExchangeRateResponseDto setBankRate(UUID adminId, Currency currency, SetBankRateRequestDto request) {

        requireQuoted(currency);
        OfficialRate official = officialRateRepository.findFirstByCurrencyOrderByRateDateDesc(currency)
                .orElseThrow(() -> new OfficialRatesUnavailableException(
                        "No NBU rate for " + currency + " has been loaded yet"));

        lockBankRate(currency);
        Instant now = clock.instant();

        BankRate rate = switch (request.mode()) {
            case SPREAD -> {
                if (request.spreadPercent() == null || request.buy() != null || request.sell() != null) {
                    throw new InvalidBankRateException("SPREAD mode takes spreadPercent only");
                }
                yield BankRate.fromSpread(currency, official.getRate(), request.spreadPercent(),
                        adminId, request.comment(), now);
            }
            case MANUAL -> {
                validateManual(request, official.getRate());
                yield BankRate.manual(currency, request.buy(), request.sell(), official.getRate(),
                        adminId, request.comment(), now);
            }
        };

        bankRateRepository.save(rate);
        auditService.record(adminId, AuditAction.BANK_RATE_SET, AuditTarget.BANK_RATE, rate.getId(), rateDetails(rate));
        log.info("Bank rate {} set by {}: buy {} / sell {} ({})", currency, adminId, rate.getBuy(), rate.getSell(),
                rate.getMode());
        return toRateDto(official, rate, today());
    }

    /** Spread is null for a manual rate, so a plain map that allows nulls, in a stable order. */
    private static Map<String, Object> rateDetails(BankRate rate) {

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("currency", rate.getCurrency());
        details.put("mode", rate.getMode());
        details.put("buy", rate.getBuy());
        details.put("sell", rate.getSell());
        details.put("spreadPercent", rate.getSpreadPercent());
        details.put("comment", rate.getComment());
        return details;
    }

    /**
     * Through UAH: the bank buys the client's currency at its buy rate and sells the other at its sell rate.
     * Intermediate values keep 8 decimals; only the final amount is rounded, once, down to cents.
     * Rounding at every step would lose or invent fractions of a cent on each of them.
     */
    @Override
    @Transactional(readOnly = true)
    public ConversionDto convert(Currency from, Currency to, BigDecimal amount) {

        if (from == to) {
            throw new IllegalArgumentException("Nothing to convert: both currencies are " + from);
        }

        BigDecimal inUah = from == Currency.UAH ? amount : amount.multiply(currentBankRate(from).getBuy());
        BigDecimal exact = to == Currency.UAH
                ? inUah
                : inUah.divide(currentBankRate(to).getSell(), CONVERSION_SCALE, RoundingMode.FLOOR);

        return new ConversionDto(
                exact.setScale(2, RoundingMode.FLOOR),
                exact.divide(amount, CONVERSION_SCALE, RoundingMode.FLOOR));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<BankRateChangeResponseDto> getBankRateChanges(Currency currency, int page, int size) {

        requireQuoted(currency);
        Page<BankRate> changes = bankRateRepository.findAllByCurrencyOrderByValidFromDescIdDesc(
                currency, PageRequest.of(page, size));

        // One lookup per distinct author, not per row: a page usually has one or two admins.
        Map<UUID, String> emails = changes.stream()
                .map(BankRate::getSetBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), id -> userService.getById(id).email()));

        return PageResponseDto.from(changes, rate -> new BankRateChangeResponseDto(
                rate.getId(),
                rate.getBuy(),
                rate.getSell(),
                rate.getMode(),
                rate.getSpreadPercent(),
                rate.getOfficialRate(),
                rate.getSetBy(),
                emails.get(rate.getSetBy()),
                rate.getComment(),
                rate.getValidFrom()));
    }

    /** Returns true if this day's rate was new. Only the newest day moves the bank's spread rate. */
    private boolean saveOfficialRate(Currency currency, NbuRateDto nbuRate) {

        LocalDate date = nbuRate.date();
        if (officialRateRepository.insertIfAbsent(currency.name(), date, nbuRate.rate(), clock.instant()) == 0) {
            return false;
        }
        log.info("NBU rate {} for {}: {}", currency, date, nbuRate.rate());

        LocalDate newest = officialRateRepository.findFirstByCurrencyOrderByRateDateDesc(currency)
                .map(OfficialRate::getRateDate)
                .orElse(date);
        if (newest.equals(date)) {
            followOfficialRate(currency, nbuRate.rate());
        }
        return true;
    }

    /** A spread rate moves with the NBU rate; a manual one stays exactly as the admin set it. */
    private void followOfficialRate(Currency currency, BigDecimal officialRate) {

        lockBankRate(currency);
        Instant now = clock.instant();
        Optional<BankRate> current = bankRateRepository.findFirstByCurrencyOrderByValidFromDescIdDesc(currency);

        if (current.isEmpty()) {
            bankRateRepository.save(BankRate.fromSpread(currency, officialRate, properties.defaultSpreadPercent(),
                    null, "Default spread", now));
        } else if (current.get().followsOfficialRate()) {
            bankRateRepository.save(current.get().recalculate(officialRate, now));
        }
    }

    /**
     * An admin's change and the automatic recalculation both read "the current rate" and append a new one.
     * Without this lock, a recalculation that read the old spread rate could land after an admin's manual
     * rate and silently replace it.
     */
    private void lockBankRate(Currency currency) {

        bankRateRepository.lockForChange("bank_rate:" + currency.name());
    }

    private void validateManual(SetBankRateRequestDto request, BigDecimal officialRate) {

        if (request.buy() == null || request.sell() == null || request.spreadPercent() != null) {
            throw new InvalidBankRateException("MANUAL mode takes buy and sell only");
        }
        if (request.buy().compareTo(request.sell()) >= 0) {
            throw new InvalidBankRateException("Buy rate must be lower than sell rate");
        }

        BigDecimal maxDeviation = properties.maxManualDeviationPercent();
        if (deviationPercent(request.buy(), officialRate).compareTo(maxDeviation) > 0
                || deviationPercent(request.sell(), officialRate).compareTo(maxDeviation) > 0) {
            throw new InvalidBankRateException("Rates must be within " + maxDeviation + "% of the NBU rate "
                    + officialRate);
        }
    }

    private static BigDecimal deviationPercent(BigDecimal value, BigDecimal officialRate) {

        return value.subtract(officialRate).abs().multiply(HUNDRED).divide(officialRate, 2, RoundingMode.HALF_UP);
    }

    private static ExchangeRateResponseDto toRateDto(OfficialRate official, BankRate bank, LocalDate today) {

        return new ExchangeRateResponseDto(
                official.getCurrency(),
                official.getRate(),
                official.getRateDate(),
                official.getRateDate().isBefore(today),
                bank != null ? bank.getBuy() : null,
                bank != null ? bank.getSell() : null,
                bank != null ? bank.getMode() : null,
                bank != null ? bank.getSpreadPercent() : null,
                bank != null ? bank.getValidFrom() : null);
    }

    private BankRate currentBankRate(Currency currency) {

        return bankRateRepository.findFirstByCurrencyOrderByValidFromDescIdDesc(currency)
                .orElseThrow(() -> new BankRateNotSetException(currency.name()));
    }

    private static Optional<Currency> quotedCurrency(String code) {

        return QUOTED.stream().filter(currency -> currency.name().equals(code)).findFirst();
    }

    private static void requireQuoted(Currency currency) {

        if (!QUOTED.contains(currency)) {
            throw new UnsupportedCurrencyException();
        }
    }

    private LocalDate today() {

        return LocalDate.now(clock.withZone(KYIV));
    }
}
