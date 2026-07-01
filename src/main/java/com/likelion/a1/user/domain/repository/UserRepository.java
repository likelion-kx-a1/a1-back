package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.User;

public interface UserRepository {
    User save(User user);
    boolean existsByEmail(String email);
}
