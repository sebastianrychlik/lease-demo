package com.luxsoft.dxc.exchange.client;

import com.luxsoft.dxc.exchange.dto.external.NbpTableDto;
import com.luxsoft.dxc.exchange.exception.NbpClientException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

/**
 * HTTP client for the Polish National Bank (NBP) public API.
 *
 * <p>Uses Spring Boot's {@link RestClient} (Spring 6.1+).
 * The base URL and the client instance are configured in
 * {@link com.luxsoft.dxc.exchange.config.NbpClientConfig}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Send HTTP requests to the NBP API.</li>
 *   <li>Deserialise the raw JSON response into external DTOs.</li>
 *   <li>Wrap any low-level HTTP errors in {@link NbpClientException}.</li>
 * </ul>
 *
 * <p>This class must not contain business logic.
 */
@Component
public class NbpClient {

    private static final String TABLES_A_PATH = "/api/exchangerates/tables/A?format=json";

    private final RestClient restClient;

    public NbpClient(RestClient nbpRestClient) {
        this.restClient = nbpRestClient;
    }

    /**
     * Fetches the current Table A exchange rates from the NBP API.
     *
     * @return list of {@link NbpTableDto} entries (normally contains exactly one element)
     * @throws NbpClientException if the NBP API is unreachable or returns an unexpected response
     */
    public List<NbpTableDto> fetchTableA() {
        try {
            NbpTableDto[] response = restClient
                    .get()
                    .uri(TABLES_A_PATH)
                    .retrieve()
                    .body(NbpTableDto[].class);

            if (response == null || response.length == 0) {
                throw new NbpClientException("NBP API returned an empty response for Table A.");
            }

            return Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new NbpClientException("Failed to fetch exchange rates from NBP API.", ex);
        }
    }
}
