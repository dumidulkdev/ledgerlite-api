package com.ledgerlite.exception;

import com.ledgerlite.config.ClockConfig;
import com.ledgerlite.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new ClockConfig().clock());
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void returnsConsistentErrorShapeForApiException() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("missing"))
                .andExpect(jsonPath("$.path").value("/probe/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/probe/not-found")
        ResponseEntity<ErrorResponse> notFound() {
            throw new ResourceNotFoundException("missing");
        }

        @GetMapping(value = "/probe/plain", produces = MediaType.APPLICATION_JSON_VALUE)
        String ok() {
            return "{}";
        }
    }
}