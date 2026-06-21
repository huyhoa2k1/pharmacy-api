package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.user.UserDto;
import com.howie.pharmacy.pharmacy_store.mapper.UserMapper;
import com.howie.pharmacy.pharmacy_store.repository.UserRepository;
import com.howie.pharmacy.pharmacy_store.security.UserPrincipal;
import com.howie.pharmacy.pharmacy_store.services.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Optional<UserDto> findById(Integer id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    public Optional<UserDto> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        Integer userId = null;
        if (principal instanceof UserPrincipal userPrincipal) {
            userId = userPrincipal.getId();
        } else if (principal instanceof Integer id) {
            userId = id;
        }

        return userId == null ? Optional.empty() : findById(userId);
    }
}
