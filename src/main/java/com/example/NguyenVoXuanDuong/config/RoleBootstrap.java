package com.example.NguyenVoXuanDuong.config;

import com.example.NguyenVoXuanDuong.model.Role;
import com.example.NguyenVoXuanDuong.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleBootstrap implements ApplicationRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureRole("USER", "Default user role");
        ensureRole("ADMIN", "Administrator role");
        ensureRole("MANAGER", "Product manager role");
    }

    private void ensureRole(String roleName, String description) {
        roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(roleName)
                .description(description)
                .build()));
    }
}
