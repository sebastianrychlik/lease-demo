package com.luxsoft.dxc.exchange.service;

import com.luxsoft.dxc.exchange.client.NbpClient;
import com.luxsoft.dxc.exchange.dto.external.NbpTableDto;
import com.luxsoft.dxc.exchange.dto.response.ExchangeRateResponse;
import com.luxsoft.dxc.exchange.exception.NbpClientException;
import com.luxsoft.dxc.exchange.mapper.ExchangeRateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for retrieving and processing exchange rates.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Call the NBP client to fetch raw exchange rate data.</li>
 *   <li>Validate that the response contains usable data.</li>
 *   <li>Convert external NBP DTOs to internal response DTOs via MapStruct.</li>
 *   <li>Return a typed {@link ExchangeRateResponse}.</li>
 * </ul>
 *
 * <p>This class must not contain HTTP logic or controller concerns.
 */
@Service
public class ExchangeRateService {

    private final NbpClient nbpClient;
    private final ExchangeRateMapper exchangeRateMapper;

    public ExchangeRateService(NbpClient nbpClient, ExchangeRateMapper exchangeRateMapper) {
        this.nbpClient = nbpClient;
        this.exchangeRateMapper = exchangeRateMapper;
    }

    /**
     * Retrieves current exchange rates from the NBP Table A.
     *
     * @return {@link ExchangeRateResponse} containing the table metadata and list of rates
     * @throws NbpClientException if the NBP API is unavailable or returns an unusable response
     */
    public ExchangeRateResponse getExchangeRates() {
        List<NbpTableDto> tables = nbpClient.fetchTableA();

        NbpTableDto firstTable = tables.stream()
                .findFirst()
                .orElseThrow(() -> new NbpClientException(
                        "NBP API response contained no exchange rate tables."));

        return exchangeRateMapper.toExchangeRateResponse(firstTable);
    }
}
