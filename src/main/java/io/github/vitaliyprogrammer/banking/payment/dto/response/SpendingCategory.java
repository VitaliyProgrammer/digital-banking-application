package io.github.vitaliyprogrammer.banking.payment.dto.response;

/** Where the client's money went, as the home screen groups it. */
public enum SpendingCategory {

    /** Transfers to other people, by IBAN or card number. Moving money between one's own accounts is not spending. */
    TRANSFERS,

    /** Top-ups of the client's own savings jars. */
    SAVINGS,

    /** Electricity, water, gas and heating bills. */
    UTILITIES,

    MOBILE,

    INTERNET
}
