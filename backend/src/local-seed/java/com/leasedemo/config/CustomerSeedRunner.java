package com.leasedemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.leasedemo.dto.CustomerSeedRecord;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.exception.InvalidPeselException;
import com.leasedemo.repository.CustomerRepository;
import com.leasedemo.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * LOCAL-DEVELOPMENT-ONLY mock Customer data importer (M4.1.3).
 *
 * <p><strong>Build-time isolation (Layer 1):</strong> this class lives
 * under {@code src/local-seed/java}, a source root compiled and packaged
 * ONLY when the {@code local-seed} Maven profile is explicitly activated
 * (see {@code backend/pom.xml}, {@code build-helper-maven-plugin}). A
 * normal {@code mvn clean package} / {@code mvn clean verify} never
 * compiles this class, so it is physically absent from the ordinary
 * production JAR — no combination of {@code SPRING_PROFILES_ACTIVE},
 * environment variables, or Spring properties can activate code that is
 * not present in the artifact at all.
 *
 * <p><strong>Runtime activation (Layers 2-3, defense in depth on top of
 * Layer 1):</strong> even when compiled in (local-seed build only), this
 * runner additionally requires BOTH the {@code local} Spring profile to
 * be active AND {@code application.seed.customers.enabled=true} to be set
 * explicitly. Neither condition alone is sufficient.
 *
 * <p><strong>Trust boundary:</strong> reads a local, developer-generated
 * JSON file (see {@code scripts/generate-customer-mock-data.py}) and, for
 * each record, calls the exact same
 * {@code CustomerService.createCustomer(String, CustomerCreateRequest)}
 * business path used by the authenticated REST API — passing the
 * record's {@code keycloakUserId} as a locally-trusted synthetic identity,
 * analogous to how {@code CustomerController} passes the verified JWT
 * {@code sub}. This mechanism never fabricates JWTs, never disables
 * security, and never talks to Keycloak.
 *
 * <p><strong>Idempotency:</strong> a synthetic identity that already has a
 * Customer record is skipped, not overwritten. Re-running the seeder is
 * safe.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "application.seed.customers.enabled", havingValue = "true")
public class CustomerSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CustomerSeedRunner.class);

    private final CustomerSeedProperties seedProperties;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;
    private final ConfigurableApplicationContext applicationContext;

    public CustomerSeedRunner(
            CustomerSeedProperties seedProperties,
            CustomerService customerService,
            CustomerRepository customerRepository,
            ObjectMapper objectMapper,
            ConfigurableApplicationContext applicationContext) {
        this.seedProperties = seedProperties;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        String filePath = seedProperties.getFile();
        if (!StringUtils.hasText(filePath)) {
            log.warn("Customer seeding is enabled but no application.seed.customers.file was configured; skipping.");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Customer seed file not found at {}; skipping.", file.getAbsolutePath());
            return;
        }

        CollectionType listType =
                objectMapper.getTypeFactory().constructCollectionType(List.class, CustomerSeedRecord.class);
        List<CustomerSeedRecord> records = objectMapper.readValue(file, listType);

        int requested = records.size();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (CustomerSeedRecord record : records) {
            try {
                if (customerRepository.existsByKeycloakUserId(record.keycloakUserId())) {
                    skipped++;
                    continue;
                }
                customerService.createCustomer(record.keycloakUserId(), record.toCreateRequest());
                created++;
            } catch (DuplicateCustomerException e) {
                // Already-present synthetic identity or PESEL — safe to skip on re-run.
                skipped++;
            } catch (InvalidPeselException e) {
                // A malformed generated record is a generator bug, not reason to abort
                // the whole batch — never log the PESEL itself.
                failed++;
                log.warn("Skipped seed customer {}: invalid PESEL", record.keycloakUserId());
            } catch (RuntimeException e) {
                failed++;
                log.warn("Skipped seed customer {}: {}", record.keycloakUserId(), e.getClass().getSimpleName());
            }
        }

        log.info("Customer seeding complete. Requested: {}, Created: {}, Skipped: {}, Failed: {}",
                requested, created, skipped, failed);

        // This runner exists purely as a one-shot local CLI import tool — exit
        // the application afterwards instead of leaving a full web server
        // running, so scripts/postgres-seed-local-data.sh can invoke it and
        // return.
        SpringApplication.exit(applicationContext, () -> 0);
    }
}
