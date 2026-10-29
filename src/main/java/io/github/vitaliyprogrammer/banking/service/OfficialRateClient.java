package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.response.NbuRateDto;
import java.util.List;

/** Source of the official exchange rates (the NBU). */
public interface OfficialRateClient {

    /**
     * Today's official rates of all currencies the NBU publishes.
     *
     * @throws io.github.vitaliyprogrammer.banking.exception.OfficialRatesUnavailableException if the NBU did not answer
     */
    List<NbuRateDto> fetchToday();
}
