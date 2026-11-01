package com.smartretail.settings.repository;

import com.smartretail.settings.entity.StoreSetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingRepository extends JpaRepository<StoreSetting, String> {

    List<StoreSetting> findAllByPublicSettingTrue();
}
