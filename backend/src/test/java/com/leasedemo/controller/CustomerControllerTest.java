package com.leasedemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedemo.config.SecurityConfig;
import com.leasedemo.dto.CustomerListItemResponse;
import com.leasedemo.dto.CustomerProfileResponse;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.dto.PageResponse;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.CustomerProfileNotFoundException;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.exception.InvalidSortFieldException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private CustomerProfileResponse sampleProfile() {
        return new CustomerProfileResponse(
                UUID.randomUUID(), "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now());
    }

    @Test
    @DisplayName("GET /api/customers/me — no JWT -> 401 Unauthorized")
    void getMe_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/customers/me — CUSTOMER with matching record -> 200 OK, uses JWT.sub")
    void getMe_customerWithProfile_returns200() throws Exception {
        when(customerService.getCurrentCustomerProfile("customer-sub-1")).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/customers/me")
                        .with(jwt().jwt(builder -> builder.subject("customer-sub-1"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.peselEncrypted").doesNotExist())
                .andExpect(jsonPath("$.peselLookup").doesNotExist())
                .andExpect(jsonPath("$.pesel").doesNotExist())
                .andExpect(jsonPath("$.keycloakUserId").doesNotExist());

        verify(customerService).getCurrentCustomerProfile("customer-sub-1");
    }

    @Test
    @DisplayName("GET /api/customers/me — CUSTOMER without profile -> 404 Not Found")
    void getMe_customerWithoutProfile_returns404() throws Exception {
        when(customerService.getCurrentCustomerProfile(anyString()))
                .thenThrow(new CustomerProfileNotFoundException(
                        "No customer profile exists for the authenticated identity"));

        mockMvc.perform(get("/api/customers/me")
                        .with(jwt().jwt(builder -> builder.subject("customer-sub-2"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/customers/me — ADMIN-only identity -> 403 Forbidden")
    void getMe_adminOnly_returns403() throws Exception {
        mockMvc.perform(get("/api/customers/me")
                        .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                .authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/customers/me — ADMIN+CUSTOMER identity -> 200 OK (has CUSTOMER authority)")
    void getMe_adminAndCustomer_returns200() throws Exception {
        when(customerService.getCurrentCustomerProfile("admin-customer-1")).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/customers/me")
                        .with(jwt().jwt(builder -> builder.subject("admin-customer-1"))
                                .authorities(() -> "ROLE_ADMIN", () -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk());
    }

    private CustomerListItemResponse sampleListItem() {
        return new CustomerListItemResponse(
                UUID.randomUUID(), "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now());
    }

    @Test
    @DisplayName("GET /api/customers — no JWT -> 401 Unauthorized")
    void getCustomers_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/customers — CUSTOMER role -> 403 Forbidden")
    void getCustomers_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .with(jwt().jwt(builder -> builder.subject("customer-1"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/customers — ADMIN role -> 200 OK with paged content")
    void getCustomers_adminRole_returns200() throws Exception {
        PageResponse<CustomerListItemResponse> page =
                new PageResponse<>(List.of(sampleListItem()), 0, 20, 1, 1, true, true);
        when(customerService.getCustomers(anyInt(), anyInt(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/customers")
                        .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                .authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.content[0].peselEncrypted").doesNotExist())
                .andExpect(jsonPath("$.content[0].peselLookup").doesNotExist())
                .andExpect(jsonPath("$.content[0].keycloakUserId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/customers — unsupported sort field -> 400 Bad Request")
    void getCustomers_unsupportedSortField_returns400() throws Exception {
        when(customerService.getCustomers(anyInt(), anyInt(), any(), eq("nope"), any()))
                .thenThrow(new InvalidSortFieldException("Unsupported sort field: 'nope'"));

        mockMvc.perform(get("/api/customers")
                        .param("sortField", "nope")
                        .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                .authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
