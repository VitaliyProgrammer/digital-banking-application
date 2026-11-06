package io.github.vitaliyprogrammer.banking.tax.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Not a period of this sole proprietor: before the business was opened, in the future, or malformed. */
public class TaxPeriodNotFoundException extends ApplicationException {

    public TaxPeriodNotFoundException() {
        super(HttpStatus.NOT_FOUND, "tax-period-not-found", "No such tax period");
    }
}
