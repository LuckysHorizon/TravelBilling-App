package com.travelbillpro.repository;

import com.travelbillpro.entity.GstConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GstConfigRepository extends JpaRepository<GstConfig, Long> {
    
    @Query("SELECT g FROM GstConfig g WHERE g.effectiveFrom <= :date ORDER BY g.effectiveFrom DESC LIMIT 1")
    Optional<GstConfig> findActiveConfigForDate(LocalDate date);
}
