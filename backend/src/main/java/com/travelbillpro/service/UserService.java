package com.travelbillpro.service;

import com.travelbillpro.dto.UserDto;
import com.travelbillpro.entity.User;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public User createUser(UserDto.CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists", "USERNAME_EXISTS", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists", "EMAIL_EXISTS", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setIsActive(true);
        user.setFailedAttempts(0);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UserDto.UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already exists", "EMAIL_EXISTS", HttpStatus.CONFLICT);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (Boolean.TRUE.equals(request.getUnlockAccount())) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (user.getUsername().equals("admin")) {
            throw new BusinessException("Cannot delete primary admin account", "CANNOT_DELETE_ADMIN", HttpStatus.FORBIDDEN);
        }
        userRepository.delete(user);
    }
}
