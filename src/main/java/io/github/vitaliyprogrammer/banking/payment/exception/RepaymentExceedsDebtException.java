package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

/** A repayment larger than the debt would park the client's own money on the credit line by mistake. */
public class RepaymentExceedsDebtException extends ApplicationException {

    public RepaymentExceedsDebtException(BigDecimal debt) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "repayment-exceeds-debt", "The repayment is larger than the debt of {0,number,#,##0.00}",
                debt);
    }
}
