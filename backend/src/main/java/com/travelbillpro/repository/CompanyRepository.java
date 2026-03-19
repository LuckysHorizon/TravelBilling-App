package com.travelbillpro.repository;

import com.travelbillpro.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByGstNumber(String gstNumber);
    boolean existsByGstNumber(String gstNumber);
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
