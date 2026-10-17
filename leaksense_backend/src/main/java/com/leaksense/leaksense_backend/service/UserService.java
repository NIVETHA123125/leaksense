package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.UserRequest;
import com.leaksense.leaksense_backend.dto.UserResponse;
import com.leaksense.leaksense_backend.entity.AppUser;
import com.leaksense.leaksense_backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse create(UserRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        if (!username.matches("[A-Za-z0-9._-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be 3 to 30 characters: letters, numbers, dot, dash or underscore");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("That username is already taken");
        }
        checkPassword(request.getPassword());

        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!role.equals("ADMIN") && !role.equals("OPERATOR")) {
            throw new IllegalArgumentException("Role must be ADMIN or OPERATOR");
        }

        String fullName = request.getFullName() == null || request.getFullName().isBlank()
                ? username
                : request.getFullName().trim();

        AppUser user = new AppUser(null, username,
                passwordEncoder.encode(request.getPassword()), fullName, role);
        return toResponse(userRepository.save(user));
    }

    public void resetPassword(Long id, String newPassword) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        checkPassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void delete(Long id, String currentUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        if ("ADMIN".equals(user.getRole()) && userRepository.countByRole("ADMIN") <= 1) {
            throw new IllegalArgumentException("You cannot delete the last administrator");
        }
        userRepository.delete(user);
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters with a letter and a number");
        }
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
