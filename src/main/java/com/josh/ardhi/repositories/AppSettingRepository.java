package com.josh.ardhi.repositories;

import com.josh.ardhi.models.AppSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

    Optional<AppSetting> findFirstByIsActiveTrueOrderByCreatedAtDesc();

    List<AppSetting> findAllByIsActiveTrue();

    List<AppSetting> findAllByOrderByCreatedAtDesc();

    Page<AppSetting> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
