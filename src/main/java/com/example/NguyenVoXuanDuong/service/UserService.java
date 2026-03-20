package com.example.NguyenVoXuanDuong.service;

import com.example.NguyenVoXuanDuong.model.Role;
import com.example.NguyenVoXuanDuong.model.User;
import com.example.NguyenVoXuanDuong.repository.RoleRepository;
import com.example.NguyenVoXuanDuong.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void save(@NotNull User user) {
        normalizeUserInput(user);
        if (user.getId() == null) { // New user
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else { // Existing user
            userRepository.findById(user.getId()).ifPresent(existingUser -> {
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    user.setPassword(existingUser.getPassword());
                } else {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                }
            });
        }
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return phone != null && !phone.isBlank() && userRepository.existsByPhone(phone);
    }

    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
            user -> {
                user.getRoles().add(getOrCreateDefaultRole());
                userRepository.save(user);
            },
            () -> {
                throw new UsernameNotFoundException("User not found: " + username);
            }
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getAuthorities())
            .accountExpired(!user.isAccountNonExpired())
            .accountLocked(!user.isAccountNonLocked())
            .credentialsExpired(!user.isCredentialsNonExpired())
            .disabled(!user.isEnabled())
            .build();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private Role getOrCreateDefaultRole() {
        return roleRepository.findByName(DEFAULT_ROLE_NAME)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(DEFAULT_ROLE_NAME)
                .description("Default user role")
                .build()));
    }

    private void normalizeUserInput(User user) {
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }
        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
            if (user.getPhone().isEmpty()) {
                user.setPhone(null);
            }
        }
    }
}
