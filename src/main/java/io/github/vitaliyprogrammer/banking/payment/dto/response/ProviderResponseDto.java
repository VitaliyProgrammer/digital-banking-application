package io.github.vitaliyprogrammer.banking.payment.dto.response;

import io.github.vitaliyprogrammer.banking.payment.entity.Provider;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import java.util.UUID;

public record ProviderResponseDto(
        UUID id,
        String code,
        String name,
        ProviderCategory category
) {

    public static ProviderResponseDto from(Provider provider) {
        return new ProviderResponseDto(provider.getId(), provider.getCode(), provider.getName(), provider.getCategory());
    }
}
