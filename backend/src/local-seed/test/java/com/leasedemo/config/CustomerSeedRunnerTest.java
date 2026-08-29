package com.leasedemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.repository.CustomerRepository;
import com.leasedemo.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomerSeedRunner} (M4.1.3). Lives under
 * {@code src/local-seed/test} alongside the seed-only production code —
 * only compiled/run when the {@code local-seed} Maven profile is active.
 * Runs against mocks only — no real PostgreSQL/Keycloak required.
 */
@ExtendWith(MockitoExtension.class)
class CustomerSeedRunnerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void isProfileGatedToLocal() {
        Profile profile = CustomerSeedRunner.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("local");
    }

    @Test
    void isDisabledByDefault() {
        CustomerSeedProperties properties = new CustomerSeedProperties();
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void hasConditionalOnPropertyRequiringExplicitEnable() {
        boolean hasConditional = false;
        for (Annotation annotation : CustomerSeedRunner.class.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals("ConditionalOnProperty")) {
                hasConditional = true;
            }
        }
        assertThat(hasConditional).isTrue();
    }

    @Test
    void skipsRecordWhenSyntheticIdentityAlreadyExists(@TempDir Path tempDir) throws Exception {
        Path seedFile = writeSeedFile(tempDir, "mock-user-000001");

        CustomerSeedProperties properties = new CustomerSeedProperties();
        properties.setEnabled(true);
        properties.setFile(seedFile.toString());

        when(customerRepository.existsByKeycloakUserId("mock-user-000001")).thenReturn(true);

        CustomerSeedRunner runner = new CustomerSeedRunner(
                properties, customerService, customerRepository, objectMapper, applicationContext);
        runner.run(null);

        verify(customerService, never()).createCustomer(anyString(), any(CustomerCreateRequest.class));
    }

    @Test
    void createsRecordForNewSyntheticIdentity(@TempDir Path tempDir) throws Exception {
        Path seedFile = writeSeedFile(tempDir, "mock-user-000002");

        CustomerSeedProperties properties = new CustomerSeedProperties();
        properties.setEnabled(true);
        properties.setFile(seedFile.toString());

        when(customerRepository.existsByKeycloakUserId("mock-user-000002")).thenReturn(false);
        when(customerService.createCustomer(eq("mock-user-000002"), any(CustomerCreateRequest.class)))
                .thenReturn(new CustomerResponse(
                        UUID.randomUUID(), "mock-user-000002", "Jan", "Kowalski",
                        "jan.kowalski.000002@example.test", "+48100000002",
                        LocalDate.of(1990, 1, 1), Gender.MALE, null, null));

        CustomerSeedRunner runner = new CustomerSeedRunner(
                properties, customerService, customerRepository, objectMapper, applicationContext);
        runner.run(null);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerService, times(1)).createCustomer(idCaptor.capture(), any(CustomerCreateRequest.class));
        assertThat(idCaptor.getValue()).isEqualTo("mock-user-000002");
    }

    @Test
    void doesNotFailBatchWhenOneRecordIsAlreadyDuplicate(@TempDir Path tempDir) throws Exception {
        Path seedFile = writeSeedFile(tempDir, "mock-user-000003");

        CustomerSeedProperties properties = new CustomerSeedProperties();
        properties.setEnabled(true);
        properties.setFile(seedFile.toString());

        when(customerRepository.existsByKeycloakUserId("mock-user-000003")).thenReturn(false);
        when(customerService.createCustomer(eq("mock-user-000003"), any(CustomerCreateRequest.class)))
                .thenThrow(new DuplicateCustomerException("duplicate"));

        CustomerSeedRunner runner = new CustomerSeedRunner(
                properties, customerService, customerRepository, objectMapper, applicationContext);

        // Must not throw — a single malformed/duplicate record must not fail the batch.
        runner.run(null);
    }

    private Path writeSeedFile(Path tempDir, String keycloakUserId) throws Exception {
        File file = tempDir.resolve("mock-customers.json").toFile();
        String json = "[{"
                + "\"keycloakUserId\":\"" + keycloakUserId + "\","
                + "\"firstName\":\"Jan\","
                + "\"lastName\":\"Kowalski\","
                + "\"email\":\"jan.kowalski." + keycloakUserId + "@example.test\","
                + "\"phoneNumber\":\"+48100000001\","
                + "\"dateOfBirth\":\"1990-01-01\","
                + "\"gender\":\"MALE\","
                + "\"pesel\":\"90010112345\""
                + "}]";
        Files.writeString(file.toPath(), json);
        return file.toPath();
    }
}
