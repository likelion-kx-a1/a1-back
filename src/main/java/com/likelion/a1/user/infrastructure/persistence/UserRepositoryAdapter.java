package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.User;
import com.likelion.a1.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository repository;

    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }
}
