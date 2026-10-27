package io.github.vitaliyprogrammer.banking.identity.service;

import io.github.vitaliyprogrammer.banking.identity.entity.User;

public interface AccessTokenService {

    String ROLES_CLAIM = "roles";

    String issue(User user);

    long ttlSeconds();
}
