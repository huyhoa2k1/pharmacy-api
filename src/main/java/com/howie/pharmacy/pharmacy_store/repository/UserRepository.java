package com.howie.pharmacy.pharmacy_store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.howie.pharmacy.pharmacy_store.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findById(Integer id);
}
