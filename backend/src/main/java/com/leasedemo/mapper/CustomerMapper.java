package com.leasedemo.mapper;

import com.leasedemo.dto.CustomerListItemResponse;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.entity.Customer;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting the {@link Customer} entity to its outbound
 * {@link CustomerResponse} DTO.
 *
 * <p>{@code peselEncrypted} and {@code peselLookup} are intentionally
 * excluded from {@link CustomerResponse}/{@link CustomerListItemResponse},
 * so MapStruct simply omits them — no explicit ignore mapping is needed
 * since the targets have no matching fields. {@link CustomerListItemResponse}
 * additionally excludes {@code keycloakUserId} (see M4.4).
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    CustomerListItemResponse toListItemResponse(Customer customer);
}
