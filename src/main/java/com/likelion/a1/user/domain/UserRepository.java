package com.likelion.a1.user.domain;

public interface UserRepository {
    User save(User user);
    boolean existsByEmail(String email);
}
