package io.github.vitaliyprogrammer.banking.exchange.service;

import io.github.vitaliyprogrammer.banking.exchange.dto.response.NbuRateDto;
import java.util.List;

/** Source of the official exchange rates (the NBU). */
public interface OfficialRateClient {

    /**
     * Today's official rates of all currencies the NBU publishes.
     *
     * @throws io.github.vitaliyprogrammer.banking.exchange.exception.OfficialRatesUnavailableException if the NBU did not answer
     */
    List<NbuRateDto> fetchToday();
}
