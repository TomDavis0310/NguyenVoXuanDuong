package com.example.NguyenVoXuanDuong;

import com.example.NguyenVoXuanDuong.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider(userService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/", "/register", "/login", "/error")
                .permitAll()
                .requestMatchers("/products/add", "/products/edit/**", "/products/update/**", "/products/delete/**")
                .hasAnyAuthority("ADMIN", "MANAGER")
                .requestMatchers("/categories/add", "/categories/edit/**", "/categories/update/**", "/categories/delete/**")
                .hasAuthority("ADMIN")
                .requestMatchers("/order/points", "/loyalty/**")
                .hasAuthority("USER")
                .requestMatchers("/products", "/products/*", "/categories", "/cart/**",
                    "/order/checkout", "/order/submit", "/order/confirmation", "/order/confirmation/**",
                    "/order/receipt/**", "/order/history", "/momo/**", "/vnpay/**")
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .rememberMe(rememberMe -> rememberMe
                .key("nguyenvoxuanduong-remember-me")
                .rememberMeCookieName("nguyenvoxuanduong-remember-me")
                .tokenValiditySeconds(24 * 60 * 60)
                .userDetailsService(userService)
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/403"))
            .build();
    }
}
