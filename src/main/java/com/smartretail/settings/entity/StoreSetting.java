package com.smartretail.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/** A key/value store setting such as {@code store_open} or {@code delivery_charge}. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store_settings")
public class StoreSetting {

    @Id
    private String key;

    @Setter
    private String value;

    @Column(name = "is_public", nullable = false)
    private boolean publicSetting;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public StoreSetting(String key, String value, boolean publicSetting) {
        this.key = key;
        this.value = value;
        this.publicSetting = publicSetting;
    }
}
