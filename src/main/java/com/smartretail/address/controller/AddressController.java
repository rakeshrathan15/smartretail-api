package com.smartretail.address.controller;

import com.smartretail.address.dto.request.AddressRequest;
import com.smartretail.address.dto.response.AddressResponse;
import com.smartretail.address.service.AddressService;
import com.smartretail.common.response.ApiResponse;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/me/addresses")
public class AddressController {

    private final AddressService addresses;

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(addresses.list(CurrentUser.id(jwt)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addresses.create(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addresses.update(CurrentUser.id(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        addresses.delete(CurrentUser.id(jwt), id);
    }
}
