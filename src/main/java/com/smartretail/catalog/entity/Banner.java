package com.smartretail.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A slide in the home screen carousel. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Setter
    private String title;

    @Setter
    private String subtitle;

    @Column(name = "cta_text")
    @Setter
    private String ctaText;

    @Setter
    private String link;

    @Column(name = "image_url")
    @Setter
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Setter
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Setter
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public Banner(String title) {
        this.title = title;
    }
}
