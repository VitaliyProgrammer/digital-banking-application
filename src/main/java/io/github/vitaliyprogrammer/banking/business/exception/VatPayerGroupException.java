package io.github.vitaliyprogrammer.banking.business.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** By the Tax Code only a group 3 single tax payer may also pay VAT. */
public class VatPayerGroupException extends ApplicationException {

    public VatPayerGroupException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "vat-payer-group", "Only a group 3 sole proprietor may be a VAT payer");
    }
}
