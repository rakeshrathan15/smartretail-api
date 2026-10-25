package com.smartretail.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/** A key/value store setting such as {@code store_open} or {@code delivery_charge}. */
@Entity
@Table(name = "store_settings")
public class StoreSetting {

    @Id
    private String key;

    private String value;

    @Column(name = "is_public", nullable = false)
    private boolean publicSetting;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected StoreSetting() {
    }

    StoreSetting(String key, String value, boolean publicSetting) {
        this.key = key;
        this.value = value;
        this.publicSetting = publicSetting;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    public boolean isPublicSetting() {
        return publicSetting;
    }
}
