package com.smartretail.address;

import com.smartretail.address.AddressDtos.AddressRequest;
import com.smartretail.address.AddressDtos.AddressResponse;
import com.smartretail.auth.Identifiers;
import com.smartretail.common.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's delivery addresses, always with exactly one default once any exist. */
@Service
@Transactional
public class AddressService {

    static final int MAX_ADDRESSES = 20;

    private final AddressRepository addresses;

    public AddressService(AddressRepository addresses) {
        this.addresses = addresses;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addresses.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).stream()
                .map(AddressResponse::from).toList();
    }

    public AddressResponse create(UUID userId, AddressRequest request) {
        long existing = addresses.countByUserId(userId);
        if (existing >= MAX_ADDRESSES) {
            throw ApiException.badRequest("You can save up to " + MAX_ADDRESSES + " addresses");
        }
        Address address = new Address(userId);
        apply(address, request);
        addresses.saveAndFlush(address);
        // The first address, or one marked default, becomes the default.
        if (existing == 0 || Boolean.TRUE.equals(request.makeDefault())) {
            makeDefault(userId, address);
        }
        return AddressResponse.from(address);
    }

    public AddressResponse update(UUID userId, UUID id, AddressRequest request) {
        Address address = find(userId, id);
        apply(address, request);
        if (Boolean.TRUE.equals(request.makeDefault())) {
            makeDefault(userId, address);
        }
        return AddressResponse.from(address);
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
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Address not found"));
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
