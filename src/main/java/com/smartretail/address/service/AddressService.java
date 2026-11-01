package com.smartretail.address.service;

import com.smartretail.address.dto.request.AddressRequest;
import com.smartretail.address.dto.response.AddressResponse;
import com.smartretail.address.entity.Address;
import com.smartretail.address.mapper.AddressMapper;
import com.smartretail.address.repository.AddressRepository;
import com.smartretail.auth.support.Identifiers;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's delivery addresses, always with exactly one default once any exist. */
@RequiredArgsConstructor
@Service
@Transactional
public class AddressService {

    public static final int MAX_ADDRESSES = 20;

    private final AddressMapper addressMapper;
    private final AddressRepository addresses;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addresses.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).stream()
                .map(addressMapper::toResponse).toList();
    }

    public AddressResponse create(UUID userId, AddressRequest request) {
        long existing = addresses.countByUserId(userId);
        if (existing >= MAX_ADDRESSES) {
            throw new ApiException(ErrorCode.ADDRESS_LIMIT_REACHED, "You can save up to " + MAX_ADDRESSES + " addresses");
        }
        Address address = new Address(userId);
        apply(address, request);
        addresses.saveAndFlush(address);
        // The first address, or one marked default, becomes the default.
        if (existing == 0 || Boolean.TRUE.equals(request.makeDefault())) {
            makeDefault(userId, address);
        }
        return addressMapper.toResponse(address);
    }

    public AddressResponse update(UUID userId, UUID id, AddressRequest request) {
        Address address = find(userId, id);
        apply(address, request);
        if (Boolean.TRUE.equals(request.makeDefault())) {
            makeDefault(userId, address);
        }
        return addressMapper.toResponse(address);
    }

    public void delete(UUID userId, UUID id) {
        Address address = find(userId, id);
        addresses.delete(address);
        addresses.flush();
        if (address.isDefaultAddress()) {
            addresses.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).stream().findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private void makeDefault(UUID userId, Address chosen) {
        addresses.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId)
                .forEach(a -> a.setDefaultAddress(a.getId().equals(chosen.getId())));
    }

    private Address find(UUID userId, UUID id) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(ErrorCode.ADDRESS_NOT_FOUND, "Address not found"));
    }

    private static void apply(Address address, AddressRequest r) {
        address.update(
                r.label() == null || r.label().isBlank() ? "Shop" : r.label().trim(),
                r.fullName().trim(),
                Identifiers.normalizePhone(r.phone()),
                r.addressLine1().trim(),
                r.addressLine2() == null || r.addressLine2().isBlank() ? null : r.addressLine2().trim(),
                r.city().trim(),
                r.state() == null || r.state().isBlank() ? "Telangana" : r.state().trim(),
                r.pincode());
    }
}
