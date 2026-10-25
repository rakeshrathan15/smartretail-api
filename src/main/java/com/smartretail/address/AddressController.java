package com.smartretail.address;

import com.smartretail.auth.Identifiers;
import com.smartretail.common.ApiException;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's delivery addresses. */
@RestController
@RequestMapping("/api/me/addresses")
@Transactional
public class AddressController {

    static final int MAX_ADDRESSES = 20;

    public record AddressRequest(
            @Size(max = 30) String label,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @NotBlank @Size(max = 80) String city,
            @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit PIN code") String pincode,
            Boolean makeDefault) {
    }

    public record AddressResponse(
            UUID id, String label, String fullName, String phone, String addressLine1, String addressLine2,
            String city, String state, String pincode, boolean isDefault) {

        static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(), a.getAddressLine1(),
                    a.getAddressLine2(), a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
        }
    }

    private final AddressRepository addresses;

    public AddressController(AddressRepository addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return addresses.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(CurrentUser.id(jwt)).stream()
                .map(AddressResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest request) {
        UUID userId = CurrentUser.id(jwt);
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

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        UUID userId = CurrentUser.id(jwt);
        Address address = find(userId, id);
        apply(address, request);
        if (Boolean.TRUE.equals(request.makeDefault())) {
            makeDefault(userId, address);
        }
        return AddressResponse.from(address);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = CurrentUser.id(jwt);
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
