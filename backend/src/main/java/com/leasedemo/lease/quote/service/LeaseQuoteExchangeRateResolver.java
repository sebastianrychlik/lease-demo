package com.leasedemo.lease.quote.service;

import com.leasedemo.exchange.client.NbpClient;
import com.leasedemo.exchange.dto.external.NbpRateDto;
import com.leasedemo.exchange.dto.external.NbpTableDto;
import com.leasedemo.exchange.exception.NbpClientException;
import com.leasedemo.lease.quote.exception.UnsupportedCurrencyException;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Resolves the PLN exchange rate for a {@link LeaseCurrency}, reusing the
 * existing {@link NbpClient} NBP Table A integration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>{@link LeaseCurrency#PLN} always resolves to a rate of {@code 1}
 *       with no NBP call and no effective date.</li>
 *   <li>{@link LeaseCurrency#EUR} (and any future non-PLN currency) is
 *       looked up in the current NBP Table A response.</li>
 * </ul>
 *
 * <p>This class contains no lease-calculation business logic — only
 * currency-to-rate resolution. It must not duplicate {@link NbpClient}.
 */
@Component
public class LeaseQuoteExchangeRateResolver {

    /** NBP mid rates are published with 4 decimal places. */
    private static final int NBP_RATE_SCALE = 4;

    private final NbpClient nbpClient;

    public LeaseQuoteExchangeRateResolver(NbpClient nbpClient) {
        this.nbpClient = nbpClient;
    }

    /**
     * A resolved exchange rate together with the NBP table's effective date
     * (or {@code null} when no NBP lookup was performed, i.e. PLN).
     */
    public record ResolvedRate(BigDecimal rate, String effectiveDate) {
    }

    public ResolvedRate resolve(LeaseCurrency currency) {
        if (currency == LeaseCurrency.PLN) {
            return new ResolvedRate(BigDecimal.ONE.setScale(NBP_RATE_SCALE, RoundingMode.HALF_UP), null);
        }

        List<NbpTableDto> tables = nbpClient.fetchTableA();
        NbpTableDto firstTable = tables.stream()
                .findFirst()
                .orElseThrow(() -> new NbpClientException(
                        "NBP API response contained no exchange rate tables."));

        NbpRateDto rateDto = firstTable.rates().stream()
                .filter(rate -> currency.name().equals(rate.code()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCurrencyException(
                        "No NBP exchange rate available for currency " + currency));

        BigDecimal rate = BigDecimal.valueOf(rateDto.mid()).setScale(NBP_RATE_SCALE, RoundingMode.HALF_UP);
        return new ResolvedRate(rate, firstTable.effectiveDate());
    }
}
