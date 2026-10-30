package com.smartretail.store;

import com.smartretail.common.ApiException;
import com.smartretail.store.StoreDtos.StoreRequest;
import com.smartretail.store.StoreDtos.StoreResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StoreService {

    private final StoreRepository stores;

    public StoreService(StoreRepository stores) {
        this.stores = stores;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> list() {
        return stores.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> namesById() {
        return stores.findAll().stream().collect(Collectors.toMap(Store::getId, Store::getName));
    }

    public StoreResponse create(StoreRequest request) {
        String name = request.name().trim();
        if (stores.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A store named " + name + " already exists");
        }
        Store store = new Store(name);
        apply(store, request);
        return toResponse(stores.save(store));
    }

    public StoreResponse update(UUID id, StoreRequest request) {
        Store store = find(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(store.getName()) && stores.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A store named " + name + " already exists");
        }
        apply(store, request);
        return toResponse(store);
    }

    public StoreResponse setActive(UUID id, boolean active) {
        Store store = find(id);
        store.setActive(active);
        return toResponse(store);
    }

    /** A store with routes keeps them pointing somewhere real; move or delete the routes first. */
    public void delete(UUID id) {
        Store store = find(id);
        long routes = stores.countRoutes(id);
        if (routes > 0) {
            throw ApiException.conflict(store.getName() + " has " + routes
                    + (routes == 1 ? " delivery route" : " delivery routes") + "; move them to another store first");
        }
        stores.delete(store);
    }

    /** Throws 404 when there is no such store; used by routes to check the store they belong to. */
    @Transactional(readOnly = true)
    public Store find(UUID id) {
        return stores.findById(id).orElseThrow(() -> ApiException.notFound("Store not found"));
    }

    private static void apply(Store store, StoreRequest r) {
        store.update(r.name().trim(),
                blankToNull(r.gstin()) == null ? null : r.gstin().trim().toUpperCase(Locale.ROOT),
                blankToNull(r.phone()),
                blankToNull(r.email()) == null ? null : r.email().trim().toLowerCase(Locale.ROOT),
                blankToNull(r.address()), blankToNull(r.city()), blankToNull(r.pincode()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StoreResponse toResponse(Store store) {
        return StoreResponse.from(store, store.getId() == null ? 0 : stores.countRoutes(store.getId()));
    }
}
