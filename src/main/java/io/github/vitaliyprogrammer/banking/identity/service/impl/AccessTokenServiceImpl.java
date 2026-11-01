package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.identity.configuration.JwtProperties;
import io.github.vitaliyprogrammer.banking.identity.entity.User;
import io.github.vitaliyprogrammer.banking.identity.service.AccessTokenService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessTokenServiceImpl implements AccessTokenService {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties properties;

    private final Clock clock;

    /** Short-lived signed JWT: subject = user id, "roles" = authorities. Not stored anywhere. */
    @Override
    public String issue(User user) {

        Instant now = clock.instant();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(ROLES_CLAIM, List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public long ttlSeconds() {

        return properties.accessTokenTtl().toSeconds();
    }
}
