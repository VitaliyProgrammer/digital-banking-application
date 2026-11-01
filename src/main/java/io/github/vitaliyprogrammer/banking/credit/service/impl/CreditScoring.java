package io.github.vitaliyprogrammer.banking.credit.service.impl;

import io.github.vitaliyprogrammer.banking.credit.configuration.CreditProperties;
import io.github.vitaliyprogrammer.banking.credit.dto.response.ScoringResultDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The credit rules as a pure function: numbers in, decision out. No database, no clock, no Spring beans
 * besides the configuration, so every rule is checked by a plain unit test in milliseconds.
 * <p>
 * Order matters: hard rules first (they decline whatever was asked), then the size of the request.
 */
@Component
@RequiredArgsConstructor
public class CreditScoring {

    /** Limits are offered in whole hundreds: 4 300 ₴, not 4 317.46 ₴. */
    private static final BigDecimal STEP = BigDecimal.valueOf(100);

    private static final BigDecimal DAYS_IN_MONTH = BigDecimal.valueOf(30);

    private final CreditProperties properties;

    /**
     * @param accountAgeDays how long the client has had a UAH current account
     * @param income         money that came to that account from outside in the income window
     * @param requested      the limit the client asked for
     */
    public ScoringResultDto score(long accountAgeDays, BigDecimal income, BigDecimal requested) {

        BigDecimal affordable = affordableLimit(income);
        List<String> reasons = new ArrayList<>();

        if (accountAgeDays < properties.minAccountAgeDays()) {
            reasons.add("Рахунок відкрито " + accountAgeDays + " дн. тому, потрібно щонайменше "
                    + properties.minAccountAgeDays());
        }
        if (income.compareTo(properties.minIncome()) < 0) {
            reasons.add("Надходження за " + properties.incomeWindowDays() + " днів: " + money(income)
                    + ", потрібно щонайменше " + money(properties.minIncome()));
        }
        if (!reasons.isEmpty()) {
            return new ScoringResultDto(CreditApplicationStatus.DECLINED, affordable, null, reasons);
        }

        reasons.add("Розрахунковий ліміт: " + money(affordable) + " (частка середнього місячного доходу)");

        if (requested.compareTo(affordable) <= 0 && requested.compareTo(properties.autoApproveUpTo()) <= 0) {
            reasons.add("Запит у межах розрахунку і автоматичного схвалення");
            return new ScoringResultDto(CreditApplicationStatus.APPROVED, affordable, requested, reasons);
        }

        if (requested.compareTo(affordable.multiply(properties.reviewFactor())) > 0) {
            reasons.add("Запит " + money(requested) + " значно перевищує розрахунковий ліміт");
            return new ScoringResultDto(CreditApplicationStatus.DECLINED, affordable, null, reasons);
        }

        reasons.add(requested.compareTo(affordable) > 0
                ? "Запит " + money(requested) + " більший за розрахунковий: потрібне рішення працівника банку"
                : "Запит " + money(requested) + " більший за поріг автоматичного схвалення "
                        + money(properties.autoApproveUpTo()));
        return new ScoringResultDto(CreditApplicationStatus.MANUAL_REVIEW, affordable, null, reasons);
    }

    /** Share of the average monthly income, rounded down to a whole hundred and never above the bank's maximum. */
    private BigDecimal affordableLimit(BigDecimal income) {

        BigDecimal monthlyIncome = income.multiply(DAYS_IN_MONTH)
                .divide(BigDecimal.valueOf(properties.incomeWindowDays()), 2, RoundingMode.FLOOR);
        BigDecimal limit = monthlyIncome.multiply(properties.incomeShare())
                .divide(STEP, 0, RoundingMode.FLOOR)
                .multiply(STEP);

        return limit.min(properties.maxLimit()).setScale(2);
    }

    private static String money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.FLOOR).toPlainString() + " ₴";
    }
}
