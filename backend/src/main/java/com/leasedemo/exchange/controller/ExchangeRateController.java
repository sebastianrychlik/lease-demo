package com.leasedemo.exchange.controller;

import com.leasedemo.exchange.dto.response.ExchangeRateResponse;
import com.leasedemo.exchange.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the exchange rates endpoint.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Expose {@code GET /api/exchange-rates}.</li>
 *   <li>Return HTTP 200 with the exchange rate response.</li>
 *   <li>Delegate all processing to {@link ExchangeRateService}.</li>
 * </ul>
 *
 * <p>This class must not contain business logic or mapping code.
 */
@RestController
@RequestMapping("/api")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Returns current NBP Table A exchange rates.
     *
     * @return HTTP 200 with {@link ExchangeRateResponse}
     */
    @GetMapping("/exchange-rates")
    public ResponseEntity<ExchangeRateResponse> getExchangeRates() {
        return ResponseEntity.ok(exchangeRateService.getExchangeRates());
    }
}
