package com.smartretail.user;

import java.util.UUID;

/**
 * Published when a shop owner saves their shop's map pin. Delivery routes listen for it, so the
 * profile code does not need to know how shops are placed on routes.
 */
public record ShopLocationChanged(UUID userId) {
}
