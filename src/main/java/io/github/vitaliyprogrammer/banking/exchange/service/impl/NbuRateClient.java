package io.github.vitaliyprogrammer.banking.exchange.service.impl;

import io.github.vitaliyprogrammer.banking.exchange.configuration.ExchangeRateProperties;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.NbuRateDto;
import io.github.vitaliyprogrammer.banking.exchange.exception.OfficialRatesUnavailableException;
import io.github.vitaliyprogrammer.banking.exchange.service.OfficialRateClient;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reading rates is a safe GET: unlike a payment, a failed call can simply be repeated at the next sync. */
@Component
public class NbuRateClient implements OfficialRateClient {

    private final RestClient restClient;

    private final String nbuUrl;

    public NbuRateClient(ExchangeRateProperties properties) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.nbuUrl = properties.nbuUrl();
    }

    @Override
    public List<NbuRateDto> fetchToday() {

        try {
            List<NbuRateDto> rates = restClient.get()
                    .uri(nbuUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return rates == null ? List.of() : rates;
        } catch (RestClientException e) {
            // Covers timeouts, 5xx and a body that is not the expected JSON.
            throw new OfficialRatesUnavailableException("NBU rates are unavailable: " + e.getMessage());
        }
    }
}
