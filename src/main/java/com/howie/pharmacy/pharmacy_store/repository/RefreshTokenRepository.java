package com.howie.pharmacy.pharmacy_store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.RefreshToken;
import com.howie.pharmacy.pharmacy_store.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);

    @Modifying
    @Query("delete from RefreshToken token where token.token = :token")
    int deleteByTokenValue(@Param("token") String token);
}
