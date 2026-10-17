package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.UserRequest;
import com.leaksense.leaksense_backend.dto.UserResponse;
import com.leaksense.leaksense_backend.entity.AppUser;
import com.leaksense.leaksense_backend.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME_MESSAGE =
            "Username must be 3 to 30 characters: letters, numbers, dot, dash or underscore";
    private static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters with a letter and a number";

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRequest request(String username, String password, String fullName, String role) {
        return new UserRequest(username, password, fullName, role);
    }

    private AppUser savedUser(ArgumentCaptor<AppUser> captor) {
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- create ----------

    @Test
    @DisplayName("create() stores an encrypted password and an uppercase role")
    void createEncryptsPasswordAndUppercasesRole() {
        when(passwordEncoder.encode("Tester@123")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response =
                userService.create(request("tester1", "Tester@123", "Test User", "operator"));

        AppUser saved = savedUser(ArgumentCaptor.forClass(AppUser.class));
        assertThat(saved.getPassword()).isEqualTo("HASHED");
        assertThat(saved.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getUsername()).isEqualTo("tester1");
        assertThat(response.getRole()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("create() uses the username as the full name when it is blank")
    void createDefaultsFullNameToUsername() {
        when(passwordEncoder.encode("Tester@123")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(request("tester1", "Tester@123", "   ", "ADMIN"));

        AppUser saved = savedUser(ArgumentCaptor.forClass(AppUser.class));
        assertThat(saved.getFullName()).isEqualTo("tester1");
    }

    @Test
    @DisplayName("create() trims spaces around the username")
    void createTrimsUsername() {
        when(passwordEncoder.encode("Tester@123")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(request("  tester1  ", "Tester@123", "Test User", "OPERATOR"));

        AppUser saved = savedUser(ArgumentCaptor.forClass(AppUser.class));
        assertThat(saved.getUsername()).isEqualTo("tester1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "has space", "bad!name", "this_username_is_way_too_long_for_it", "  "})
    @DisplayName("create() rejects invalid usernames")
    void createRejectsInvalidUsername(String username) {
        assertThatThrownBy(() ->
                userService.create(request(username, "Tester@123", "Name", "OPERATOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(USERNAME_MESSAGE);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() rejects a username that is already taken")
    void createRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.create(request("admin", "Tester@123", "Name", "OPERATOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That username is already taken");

        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "short1A", "onlyletters", "12345678"})
    @DisplayName("create() rejects weak passwords")
    void createRejectsWeakPassword(String password) {
        assertThatThrownBy(() ->
                userService.create(request("tester2", password, "Weak", "OPERATOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PASSWORD_MESSAGE);

        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"SUPERUSER", "  "})
    @DisplayName("create() rejects roles other than ADMIN and OPERATOR")
    void createRejectsInvalidRole(String role) {
        assertThatThrownBy(() ->
                userService.create(request("tester3", "Tester@123", "Name", role)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role must be ADMIN or OPERATOR");

        verify(userRepository, never()).save(any());
    }

    // ---------- resetPassword ----------

    @Test
    @DisplayName("resetPassword() stores the new encrypted password")
    void resetPasswordStoresNewHash() {
        AppUser user = new AppUser(5L, "tester1", "OLDHASH", "Test User", "OPERATOR");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("NEWHASH");

        userService.resetPassword(5L, "NewPass@123");

        assertThat(user.getPassword()).isEqualTo("NEWHASH");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword() rejects a weak password and keeps the old one")
    void resetPasswordRejectsWeakPassword() {
        AppUser user = new AppUser(5L, "tester1", "OLDHASH", "Test User", "OPERATOR");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resetPassword(5L, "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PASSWORD_MESSAGE);

        assertThat(user.getPassword()).isEqualTo("OLDHASH");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword() fails for an unknown user")
    void resetPasswordUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(99L, "NewPass@123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    // ---------- delete ----------

    @Test
    @DisplayName("delete() fails for an unknown user")
    void deleteUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("delete() does not let you delete your own account")
    void deleteOwnAccountIsBlocked() {
        AppUser admin = new AppUser(1L, "admin", "HASH", "Admin", "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.delete(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot delete your own account");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete() does not let you remove the last administrator")
    void deleteLastAdminIsBlocked() {
        AppUser admin = new AppUser(1L, "admin", "HASH", "Admin", "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.delete(1L, "someoneelse"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot delete the last administrator");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete() removes an admin when another admin exists")
    void deleteAdminWhenAnotherExists() {
        AppUser admin = new AppUser(2L, "admin2", "HASH", "Second Admin", "ADMIN");
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole("ADMIN")).thenReturn(2L);

        userService.delete(2L, "admin");

        verify(userRepository).delete(admin);
    }

    @Test
    @DisplayName("delete() removes an operator")
    void deleteOperator() {
        AppUser operator = new AppUser(3L, "tester1", "HASH", "Tester", "OPERATOR");
        when(userRepository.findById(3L)).thenReturn(Optional.of(operator));

        userService.delete(3L, "admin");

        verify(userRepository).delete(operator);
    }
}
