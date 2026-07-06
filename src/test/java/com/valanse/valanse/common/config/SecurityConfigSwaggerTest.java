package com.valanse.valanse.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
class SecurityConfigSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("prod profile에서는 Swagger UI와 API 문서를 공개하지 않는다")
    void swaggerEndpoints_ProdProfile_ReturnForbidden() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }
}
