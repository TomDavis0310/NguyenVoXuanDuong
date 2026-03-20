package com.example.NguyenVoXuanDuong.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymous_shouldRedirect_whenAccessProductAdd() throws Exception {
        mockMvc.perform(get("/products/add"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void manager_shouldAccessProductAdd() throws Exception {
        mockMvc.perform(get("/products/add")
                .with(user("manager").authorities(new SimpleGrantedAuthority("MANAGER"))))
            .andExpect(status().isOk());
    }

    @Test
    void manager_shouldBeForbidden_forCategoryAdd() throws Exception {
        mockMvc.perform(get("/categories/add")
                .with(user("manager").authorities(new SimpleGrantedAuthority("MANAGER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void user_shouldAccessLoyaltyLookup() throws Exception {
        mockMvc.perform(get("/loyalty/lookup")
                .with(user("user").authorities(new SimpleGrantedAuthority("USER"))))
            .andExpect(status().isOk());
    }

    @Test
    void manager_shouldBeForbidden_forLoyaltyLookup() throws Exception {
        mockMvc.perform(get("/loyalty/lookup")
                .with(user("manager").authorities(new SimpleGrantedAuthority("MANAGER"))))
            .andExpect(status().isForbidden());
    }
}
