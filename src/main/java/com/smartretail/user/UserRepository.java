package com.smartretail.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("select count(u) > 0 from User u join u.roles r where r = :role")
    boolean existsWithRole(@Param("role") Role role);

    interface CustomerMatch {
        UUID getUserId();

        String getFullName();

        String getShopName();

        String getPhone();

        String getEmail();
    }

    /** Up to 20 shop owners whose name, shop name, phone or email matches {@code pattern} (lower-case, with %). */
    @Query(nativeQuery = true, value = """
            SELECT u.id AS userId, p.full_name AS fullName, p.shop_name AS shopName, u.phone AS phone, u.email AS email
            FROM users u
            JOIN user_roles r ON r.user_id = u.id AND r.role = 'CUSTOMER'
            LEFT JOIN profiles p ON p.user_id = u.id
            WHERE lower(coalesce(p.full_name, '')) LIKE :pattern
               OR lower(coalesce(p.shop_name, '')) LIKE :pattern
               OR coalesce(u.phone, '') LIKE :pattern
               OR lower(coalesce(u.email, '')) LIKE :pattern
            ORDER BY p.full_name NULLS LAST
            LIMIT 20
            """)
    List<CustomerMatch> searchCustomers(@Param("pattern") String pattern);
}
