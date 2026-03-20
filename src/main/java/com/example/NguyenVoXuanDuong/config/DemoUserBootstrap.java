package com.example.NguyenVoXuanDuong.config;

import com.example.NguyenVoXuanDuong.model.Role;
import com.example.NguyenVoXuanDuong.model.User;
import com.example.NguyenVoXuanDuong.repository.RoleRepository;
import com.example.NguyenVoXuanDuong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DemoUserBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        ensureUser(
            "manager_demo",
            "manager_demo@local.test",
            "0900000002",
            "manager123",
            "MANAGER"
        );

        ensureUser(
            "user_demo",
            "user_demo@local.test",
            "0900000001",
            "user12345",
            "USER"
        );
    }

    private void ensureUser(String username, String email, String phone, String rawPassword, String roleName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        Role role = roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(roleName)
                .description("Demo bootstrap role: " + roleName)
                .build()));

        User user = User.builder()
            .username(username)
            .email(email)
            .phone(phone)
            .password(passwordEncoder.encode(rawPassword))
            .roles(Set.of(role))
            .build();

        userRepository.save(user);
    }
}
