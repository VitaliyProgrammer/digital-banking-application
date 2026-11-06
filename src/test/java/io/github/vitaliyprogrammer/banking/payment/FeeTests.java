package io.github.vitaliyprogrammer.banking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.payment.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import io.github.vitaliyprogrammer.banking.payment.service.FeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.BillPaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.CardTransferFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.CreditRepaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.ExchangeFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.FeeCalculator;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.SavingsDepositFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.SavingsWithdrawalFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TaxPaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TopUpFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TransferFeePolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The tariffs, kopiyka by kopiyka. Each row is a case a client could check by hand: percentage, rounding,
 * and the minimum fee.
 */
class FeeTests {

    private final FeeCalculator calculator = new FeeCalculator(allPolicies());

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource({
            "1000.00, UAH, 5.00",   // 0.5% exactly the minimum
            "1025.00, UAH, 5.13",   // 5.125 rounds half up
            "100.00,  UAH, 5.00",   // 0.50 is below the minimum
            "10.00,   USD, 0.20",   // the minimum is per currency
            "200.00,  EUR, 1.00"})
    void transferByIban(BigDecimal amount, Currency currency, BigDecimal fee) {
        assertThat(calculator.calculate(PaymentType.TRANSFER, FeeRequest.withoutCard(amount, currency)))
                .isEqualByComparingTo(fee);
    }

    @ParameterizedTest(name = "{0} card, {1} → {2}")
    @CsvSource({
            "UNIVERSAL, 2000.00, 10.00",
            "UNIVERSAL, 100.00,  5.00",    // the minimum of a paid tariff
            "BUSINESS,  2000.00, 10.00",
            "PAYROLL,   2000.00, 10.00",
            "STUDENT,   2000.00, 0.00",
            "STUDENT,   1.00,    0.00",    // a free tariff has no minimum
            "VIRTUAL,   2000.00, 20.00",
            "CREDIT,    2000.00, 40.00"})
    void cardToCardDependsOnTheSendersCard(CardType cardType, BigDecimal amount, BigDecimal fee) {
        assertThat(calculator.calculate(PaymentType.CARD_TRANSFER, FeeRequest.fromCard(amount, Currency.UAH, cardType)))
                .isEqualByComparingTo(fee);
    }

    @ParameterizedTest(name = "{0}, {1} → {2}")
    @CsvSource({
            "ELECTRICITY, 300.00, 1.50",
            "INTERNET,    300.00, 1.50",
            "MOBILE,      200.00, 2.00",
            "WATER,       100.00, 1.00",   // 0.50 is below the minimum
            "MOBILE,      10.00,  1.00"})
    void billDependsOnWhatIsPaid(ProviderCategory category, BigDecimal amount, BigDecimal fee) {
        assertThat(calculator.calculate(PaymentType.BILL_PAYMENT, FeeRequest.forBill(amount, Currency.UAH, category)))
                .isEqualByComparingTo(fee);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentType.class,
            names = {"TOP_UP", "EXCHANGE", "SAVINGS_DEPOSIT", "SAVINGS_WITHDRAWAL", "CREDIT_REPAYMENT", "TAX_PAYMENT"})
    void movingOwnMoneyAndPayingDebtsOrTaxesIsFree(PaymentType type) {
        BigDecimal fee = calculator.calculate(type, FeeRequest.withoutCard(new BigDecimal("12345.67"), Currency.UAH));

        assertThat(fee).isEqualByComparingTo("0.00");
        assertThat(fee.scale()).as("money always has two decimals").isEqualTo(2);
    }

    @Test
    void paymentTypeWithoutATariffStopsTheApplication() {
        List<FeePolicy> withoutTax = new ArrayList<>(allPolicies());
        withoutTax.removeIf(policy -> policy.paymentType() == PaymentType.TAX_PAYMENT);

        // Failing at startup beats failing on the first tax payment of a real client.
        assertThatThrownBy(() -> new FeeCalculator(withoutTax))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TAX_PAYMENT");
    }

    @Test
    void twoTariffsForOneTypeStopTheApplication() {
        List<FeePolicy> twice = new ArrayList<>(allPolicies());
        twice.add(new TopUpFeePolicy());

        assertThatThrownBy(() -> new FeeCalculator(twice))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two fee policies for TOP_UP");
    }

    /** What Spring would inject: one policy per payment type. */
    private static List<FeePolicy> allPolicies() {
        return List.of(new TopUpFeePolicy(), new TransferFeePolicy(), new CardTransferFeePolicy(),
                new BillPaymentFeePolicy(), new ExchangeFeePolicy(), new SavingsDepositFeePolicy(),
                new SavingsWithdrawalFeePolicy(), new CreditRepaymentFeePolicy(), new TaxPaymentFeePolicy());
    }
}
