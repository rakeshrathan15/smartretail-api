package com.smartretail.address;

import com.smartretail.address.AddressDtos.AddressRequest;
import com.smartretail.address.AddressDtos.AddressResponse;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
public class AddressController {

    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return addresses.list(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest request) {
        return addresses.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return addresses.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        addresses.delete(CurrentUser.id(jwt), id);
    }
}
