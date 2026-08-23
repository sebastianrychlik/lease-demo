package com.leasedemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedemo.config.SecurityConfig;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link CustomerController}, focused on the JWT-to-identity
 * trust boundary introduced in M4.1.2:
 *
 * <ul>
 *   <li>the authenticated JWT {@code sub} is used as {@code keycloakUserId}</li>
 *   <li>a client cannot supply/override this identity via the request body
 *       (there is no such field on {@code CustomerCreateRequest})</li>
 *   <li>a missing JWT is rejected with 401 before reaching the controller</li>
 *   <li>a duplicate authenticated identity results in 409</li>
 * </ul>
 */
@WebMvcTest(controllers = CustomerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
@DisplayName("CustomerController")
class CustomerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CustomerService customerService;

    /** Prevents OAuth2 resource server auto-config from calling Keycloak's OIDC discovery endpoint. */
    @MockBean
    JwtDecoder jwtDecoder;

    private String requestBodyJson() {
        return """
                {
                  "firstName": "Jan",
                  "lastName": "Kowalski",
                  "email": "jan.kowalski@example.com",
                  "phoneNumber": "+48123456789",
                  "dateOfBirth": "1944-05-14",
                  "gender": "MALE",
                  "pesel": "44051401359"
                }
                """;
    }

    @Test
    @DisplayName("no JWT -> 401 Unauthorized")
    void createCustomer_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType("application/json")
                        .content(requestBodyJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("valid JWT -> identity is bound to JWT sub, request body cannot override it")
    void createCustomer_validJwt_bindsIdentityFromJwtSubject() throws Exception {
        String trustedSubject = "keycloak-user-123";
        CustomerResponse response = new CustomerResponse(
                UUID.randomUUID(), trustedSubject, "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now(), Instant.now());
        when(customerService.createCustomer(eq(trustedSubject), any())).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .with(jwt().jwt(builder -> builder.subject(trustedSubject)))
                        .contentType("application/json")
                        .content(requestBodyJson()))
                .andExpect(status().isCreated());

        // The controller must derive the identity from the JWT subject only — the
        // request body (asserted above via requestBodyJson()) has no such field.
        verify(customerService).createCustomer(eq(trustedSubject), any());
    }

    @Test
    @DisplayName("duplicate authenticated Keycloak identity -> 409 Conflict")
    void createCustomer_duplicateKeycloakIdentity_returns409() throws Exception {
        when(customerService.createCustomer(anyString(), any()))
                .thenThrow(new DuplicateCustomerException(
                        "A customer is already registered for this authenticated identity"));

        mockMvc.perform(post("/api/customers")
                        .with(jwt().jwt(builder -> builder.subject("keycloak-user-123")))
                        .contentType("application/json")
                        .content(requestBodyJson()))
                .andExpect(status().isConflict());
    }
}
