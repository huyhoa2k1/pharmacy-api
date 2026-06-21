package com.howie.pharmacy.pharmacy_store.services;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.user.UserDto;

@Component
public interface UserService {
    Optional<UserDto> findById(Integer id);
}
