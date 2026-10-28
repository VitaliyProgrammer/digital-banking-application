package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.ProviderProperties;
import io.github.vitaliyprogrammer.banking.dto.request.ProviderPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderCallResult;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderPaymentResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.ProviderOutcome;
import io.github.vitaliyprogrammer.banking.service.ProviderClient;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Translates HTTP into business meaning. The key distinction is between "the request never left"
 * (safe to try again) and "we sent it and got no answer" (the provider may have taken the money).
 */
@Slf4j
@Component
public class HttpProviderClient implements ProviderClient {

    private final RestClient restClient;

    public HttpProviderClient(ProviderProperties properties) {

        // HTTP/1.1 explicitly: by default the JDK client first tries an HTTP/2 upgrade on plain http. A server that
        // does not support it drops the connection, and the client then sends the POST again: a payment twice.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ProviderCallResult send(String providerCode, ProviderPaymentRequestDto request) {

        try {
            ProviderPaymentResponseDto response = restClient.post()
                    .uri("/{code}/payments", providerCode)
                    .body(request)
                    .retrieve()
                    .body(ProviderPaymentResponseDto.class);

            return toResult(response);
        } catch (HttpClientErrorException e) {
            // 4xx: the provider received the request and refused it, e.g. an unknown personal account.
            return new ProviderCallResult(ProviderOutcome.REJECTED, "Provider refused the payment: " + e.getStatusCode());
        } catch (HttpServerErrorException | ResourceAccessException e) {
            return unclear(e, request.reference());
        }
    }

    @Override
    public ProviderCallResult checkStatus(String providerCode, UUID reference) {

        try {
            ProviderPaymentResponseDto response = restClient.get()
                    .uri("/{code}/payments/{reference}", providerCode, reference)
                    .retrieve()
                    .body(ProviderPaymentResponseDto.class);

            return toResult(response);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ProviderCallResult.of(ProviderOutcome.NOT_FOUND);
            }
            return new ProviderCallResult(ProviderOutcome.NO_ANSWER, "Status check refused: " + e.getStatusCode());
        } catch (HttpServerErrorException | ResourceAccessException e) {
            return unclear(e, reference);
        }
    }

    private static ProviderCallResult toResult(ProviderPaymentResponseDto response) {

        if (response != null && "ACCEPTED".equals(response.status())) {
            return ProviderCallResult.of(ProviderOutcome.ACCEPTED);
        }

        if (response != null && "REJECTED".equals(response.status())) {
            return new ProviderCallResult(ProviderOutcome.REJECTED, response.reason());
        }

        // A 200 we cannot understand is not a decision: treat it like no answer and check the status later.
        return new ProviderCallResult(ProviderOutcome.NO_ANSWER, "Unexpected provider response: " + response);
    }

    /** A refused connection means nothing was sent; a timeout or 5xx means the request may have been processed. */
    private static ProviderCallResult unclear(RuntimeException e, UUID reference) {

        if (e.getCause() instanceof ConnectException || e.getCause() instanceof HttpConnectTimeoutException) {
            log.warn("Provider unreachable for payment {}: {}", reference, e.getMessage());
            return new ProviderCallResult(ProviderOutcome.NOT_DELIVERED, e.getMessage());
        }

        log.warn("No clear answer from provider for payment {}: {}", reference, e.getMessage());
        return new ProviderCallResult(ProviderOutcome.NO_ANSWER, e.getMessage());
    }
}
