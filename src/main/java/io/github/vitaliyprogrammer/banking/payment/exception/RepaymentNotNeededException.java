package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** With the limit on the account itself, any money that arrives on it pays the debt off: no separate repayment. */
public class RepaymentNotNeededException extends ApplicationException {

    public RepaymentNotNeededException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "repayment-not-needed", "Your credit limit is on this account: any money that arrives on it repays the debt by itself");
    }
}
