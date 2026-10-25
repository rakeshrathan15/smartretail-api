package com.smartretail.catalog;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Product list filters. {@code publicOnly} hides inactive products and products whose
 * category is inactive; admins see everything.
 */
public record ProductFilter(String categorySlug, String query, boolean dealsOnly, Boolean active, boolean publicOnly) {

    Specification<Product> toSpecification() {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Product, Category> category = root.join("category", JoinType.LEFT);
            if (publicOnly) {
                predicates.add(cb.isTrue(root.get("active")));
                predicates.add(cb.or(cb.isNull(category.get("id")), cb.isTrue(category.get("active"))));
            } else if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(category.get("slug"), categorySlug));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("brand")), pattern, '\\'),
                        cb.like(cb.lower(root.get("flavor")), pattern, '\\')));
            }
            if (dealsOnly) {
                predicates.add(cb.greaterThan(root.get("originalPrice"), root.get("price")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
