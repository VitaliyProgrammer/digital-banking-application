package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

/** A repayment larger than the debt would park the client's own money on the credit line by mistake. */
public class RepaymentExceedsDebtException extends ApplicationException {

    public RepaymentExceedsDebtException(BigDecimal debt) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "The repayment is larger than the debt of " + debt.toPlainString());
    }
}
