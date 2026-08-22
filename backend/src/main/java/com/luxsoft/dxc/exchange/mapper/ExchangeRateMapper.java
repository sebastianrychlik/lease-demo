package com.luxsoft.dxc.exchange.mapper;

import com.luxsoft.dxc.exchange.dto.external.NbpRateDto;
import com.luxsoft.dxc.exchange.dto.external.NbpTableDto;
import com.luxsoft.dxc.exchange.dto.response.ExchangeRateDto;
import com.luxsoft.dxc.exchange.dto.response.ExchangeRateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting NBP external DTOs to our internal response DTOs.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Convert {@link NbpRateDto} → {@link ExchangeRateDto}</li>
 *   <li>Convert {@link NbpTableDto} → {@link ExchangeRateResponse}</li>
 * </ul>
 *
 * <p>This mapper contains no business logic.
 * All field-level transformations are declarative mappings only.
 */
@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    /**
     * Maps a single NBP rate entry to our internal ExchangeRateDto.
     *
     * <ul>
     *   <li>{@code code}    ← {@code NbpRateDto.code}</li>
     *   <li>{@code name}    ← {@code NbpRateDto.currency}</li>
     *   <li>{@code midRate} ← {@code NbpRateDto.mid}</li>
     * </ul>
     */
    @Mapping(target = "code",    source = "code")
    @Mapping(target = "name",    source = "currency")
    @Mapping(target = "midRate", source = "mid")
    ExchangeRateDto toExchangeRateDto(NbpRateDto nbpRateDto);

    /**
     * Maps an NBP table to our internal ExchangeRateResponse.
     *
     * <ul>
     *   <li>{@code tableNo}       ← {@code NbpTableDto.no}</li>
     *   <li>{@code effectiveDate} ← {@code NbpTableDto.effectiveDate}</li>
     *   <li>{@code rates}         ← mapped via {@link #toExchangeRateDto}</li>
     * </ul>
     */
    @Mapping(target = "tableNo",       source = "no")
    @Mapping(target = "effectiveDate", source = "effectiveDate")
    @Mapping(target = "rates",         source = "rates")
    ExchangeRateResponse toExchangeRateResponse(NbpTableDto nbpTableDto);
}
