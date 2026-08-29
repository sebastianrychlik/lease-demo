package com.leasedemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the LOCAL-DEVELOPMENT-ONLY mock Customer seeder
 * (M4.1.3, see {@code CustomerSeedRunner}).
 *
 * <p><strong>Build-time isolation:</strong> this class lives under
 * {@code src/local-seed/java}, a source root compiled and packaged ONLY
 * when the {@code local-seed} Maven profile is explicitly activated (see
 * {@code backend/pom.xml}). A normal {@code mvn clean package} / {@code
 * mvn clean verify} never compiles or packages this class at all — it is
 * physically absent from the ordinary production artifact, regardless of
 * Spring profile or property configuration at runtime.
 *
 * <p>Bound from {@code application.seed.customers.*}. Even when compiled
 * in, this mechanism is additionally gated by the {@code local} Spring
 * profile ({@code @Profile("local")} on {@code CustomerSeedRunner}) and by
 * {@code enabled=false} by default, so a normal local startup never
 * unexpectedly inserts mock data.
 */
@Component
@ConfigurationProperties(prefix = "application.seed.customers")
public class CustomerSeedProperties {

    /**
     * Master switch. Must be explicitly set to {@code true} (e.g. via
     * {@code -Dapplication.seed.customers.enabled=true} or an environment
     * variable) in addition to running under the {@code local} profile.
     */
    private boolean enabled = false;

    /**
     * Path to the JSON file produced by
     * {@code scripts/generate-customer-mock-data.py}, containing an array
     * of synthetic Customer seed records.
     */
    private String file;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
