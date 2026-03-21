package com.travelbillpro.repository;

import com.travelbillpro.entity.BillingPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingPanelRepository extends JpaRepository<BillingPanel, Long> {

    List<BillingPanel> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<BillingPanel> findAllByOrderByCreatedAtDesc();
}
