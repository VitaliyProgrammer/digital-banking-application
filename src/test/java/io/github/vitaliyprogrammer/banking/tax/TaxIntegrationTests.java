package io.github.vitaliyprogrammer.banking.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.business.dto.request.OpenBusinessRequestDto;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessResponseDto;
import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.receipt.service.ReceiptService;
import io.github.vitaliyprogrammer.banking.tax.dto.response.TaxPeriodResponseDto;
import io.github.vitaliyprogrammer.banking.tax.entity.status.TaxPeriodStatus;
import io.github.vitaliyprogrammer.banking.tax.exception.NothingToPayException;
import io.github.vitaliyprogrammer.banking.tax.service.TaxService;
import io.github.vitaliyprogrammer.banking.tax.service.impl.TaxPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for the single tax. A group 3 quarter can be paid from the tax jar with a receipt; with auto-pay on the
 * bank pays on the due date by itself, once, even if the job runs twice; a reminder goes out a few days before, once;
 * with too little money nothing is paid and one notice goes out, and the next day's run pays once money is there;
 * without auto-pay the bank never pays on its own.
 */
class TaxIntegrationTests extends IntegrationTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private TaxService taxService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptService receiptService;

    private final LocalDate today = LocalDate.now(TaxPeriod.KYIV);

    @Test
    void aGroupThreeQuarterIsPaidFromTheTaxJarWithAReceipt() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3);
        income(ownerId, business, "10000.00");                       // 500 into the jar

        TaxPeriodResponseDto quarter = taxService.getPeriods(ownerId).getFirst();
        assertThat(quarter.status()).isEqualTo(TaxPeriodStatus.ACCRUING);
        assertThat(quarter.amount()).isEqualByComparingTo("500.00");

        PaymentResponseDto payment = taxService.payNow(ownerId, quarter.period());

        assertThat(payment.type()).isEqualTo(PaymentType.TAX_PAYMENT);
        assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.amount()).isEqualByComparingTo("500.00");
        assertThat(balanceOf(business.taxReserveAccountId())).isEqualByComparingTo("0.00");
        assertThat(taxService.getPeriods(ownerId).getFirst().left()).isEqualByComparingTo("0.00");
        assertThat(receiptService.getReceipt(ownerId, payment.id()).content()).isNotEmpty();
        assertThatThrownBy(() -> taxService.payNow(ownerId, quarter.period())).isInstanceOf(NothingToPayException.class);
    }

    @Test
    void autoPayPaysOnTheDueDateOnceEvenIfTheJobRunsTwice() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3);
        businessService.setAutoPayTax(ownerId, true);
        income(ownerId, business, "20000.00");                       // 1 000 into the jar
        LocalDate dueDate = currentQuarter().dueDate();

        taxService.runDaily(dueDate.minusDays(1));
        assertThat(balanceOf(business.taxReserveAccountId())).isEqualByComparingTo("1000.00");

        taxService.runDaily(dueDate);
        taxService.runDaily(dueDate);

        assertThat(balanceOf(business.taxReserveAccountId())).isEqualByComparingTo("0.00");
        assertThat(taxPaymentsOf(ownerId)).isEqualTo(1);
    }

    @Test
    void aReminderGoesOutAFewDaysBeforeTheDueDateOnce() {
        UUID ownerId = createUser();
        open(ownerId, TaxGroup.GROUP_1);
        LocalDate dueDate = TaxPeriod.month(YearMonth.from(today)).dueDate();

        taxService.runDaily(dueDate.minusDays(6));
        assertThat(noticesOf(ownerId, "DUE_SOON")).isZero();

        taxService.runDaily(dueDate.minusDays(5));
        taxService.runDaily(dueDate.minusDays(4));
        assertThat(noticesOf(ownerId, "DUE_SOON")).isEqualTo(1);
    }

    @Test
    void withTooLittleMoneyNothingIsPaidAndTheNextRunPaysFromTheBusinessAccount() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_1);   // 302.80 a month, nothing put aside
        businessService.setAutoPayTax(ownerId, true);
        LocalDate dueDate = TaxPeriod.month(YearMonth.from(today)).dueDate();

        taxService.runDaily(dueDate);
        taxService.runDaily(dueDate);
        assertThat(taxPaymentsOf(ownerId)).isZero();
        assertThat(noticesOf(ownerId, "SHORTFALL")).isEqualTo(1);

        income(ownerId, business, "1000.00");
        taxService.runDaily(dueDate.plusDays(1));

        assertThat(taxPaymentsOf(ownerId)).isEqualTo(1);
        assertThat(balanceOf(business.businessAccountId())).isEqualByComparingTo("697.20");
    }

    @Test
    void withoutAutoPayTheBankNeverPaysOnItsOwn() {
        UUID ownerId = createUser();
        BusinessResponseDto business = open(ownerId, TaxGroup.GROUP_3);
        income(ownerId, business, "10000.00");

        taxService.runDaily(currentQuarter().dueDate());

        assertThat(taxPaymentsOf(ownerId)).isZero();
        assertThat(balanceOf(business.taxReserveAccountId())).isEqualByComparingTo("500.00");
    }

    @Test
    void groupsOneAndTwoOweAMonthAndGroupThreeAQuarter() {
        UUID monthly = createUser();
        open(monthly, TaxGroup.GROUP_2);
        UUID quarterly = createUser();
        open(quarterly, TaxGroup.GROUP_3);

        List<TaxPeriodResponseDto> months = taxService.getPeriods(monthly);
        assertThat(months.getFirst().period()).isEqualTo(YearMonth.from(today).toString());
        assertThat(months.getFirst().dueDate()).isEqualTo(YearMonth.from(today).atDay(20));
        assertThat(months.getFirst().amount()).isEqualByComparingTo("1600.00");

        TaxPeriodResponseDto quarter = taxService.getPeriods(quarterly).getFirst();
        assertThat(quarter.period()).isEqualTo(currentQuarter().code());
        assertThat(quarter.dueDate()).isEqualTo(currentQuarter().dueDate());
    }

    private BusinessResponseDto open(UUID ownerId, TaxGroup group) {
        return businessService.open(ownerId, new OpenBusinessRequestDto("ФОП Демо", uniqueTaxId(), List.of("62.01"),
                group, false, LocalDate.of(2024, 1, 15)));
    }

    /** Money from a customer outside the bank: income. */
    private void income(UUID ownerId, BusinessResponseDto business, String amount) {
        paymentService.topUp(ownerId, UUID.randomUUID().toString(),
                new TopUpRequestDto(business.businessAccountId(), new BigDecimal(amount)));
    }

    private TaxPeriod currentQuarter() {
        return TaxPeriod.quarter(today.getYear(), (today.getMonthValue() - 1) / 3 + 1);
    }

    private Integer taxPaymentsOf(UUID ownerId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE initiator_id = ? AND type = 'TAX_PAYMENT'", Integer.class, ownerId);
    }

    private Integer noticesOf(UUID ownerId, String kind) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM outbox_events
                WHERE event_type = 'tax.notice' AND payload ->> 'userId' = ? AND payload ->> 'kind' = ?
                """, Integer.class, ownerId.toString(), kind);
    }

    private static String uniqueTaxId() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }
}
