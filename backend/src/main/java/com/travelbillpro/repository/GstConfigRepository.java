package com.travelbillpro.repository;

import com.travelbillpro.entity.GstConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GstConfigRepository extends JpaRepository<GstConfig, Long> {
    
    @Query(value = "SELECT * FROM gst_config g WHERE g.effective_from <= :date ORDER BY g.effective_from DESC LIMIT 1", nativeQuery = true)
    Optional<GstConfig> findActiveConfigForDate(LocalDate date);

    default Optional<GstConfig> findActiveConfig() {
        return findActiveConfigForDate(LocalDate.now());
    }
}
