package com.travelbillpro.repository;

import com.travelbillpro.entity.ExtractionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractionAuditRepository extends JpaRepository<ExtractionAudit, Long> {
    List<ExtractionAudit> findBySourceFilenameOrderByCreatedAtDesc(String filename);
    List<ExtractionAudit> findByExtractionStatus(String status);
}
