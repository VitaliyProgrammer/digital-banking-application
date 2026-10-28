package io.github.vitaliyprogrammer.banking.entity.status;

/** What a call to a provider told us. Only ACCEPTED and REJECTED are final answers. */
public enum ProviderOutcome {

    ACCEPTED,

    REJECTED,

    /** Status check: the provider has no payment with this reference, so it never received it. */
    NOT_FOUND,

    /** The connection could not even be opened: the request certainly did not reach the provider. */
    NOT_DELIVERED,

    /** Timeout or server error: the provider may have processed the request. The dangerous case. */
    NO_ANSWER
}
