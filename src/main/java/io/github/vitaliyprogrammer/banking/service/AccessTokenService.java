package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.entity.User;

public interface AccessTokenService {

    String ROLES_CLAIM = "roles";

    String issue(User user);

    long ttlSeconds();
}
