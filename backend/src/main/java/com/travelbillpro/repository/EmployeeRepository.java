package com.travelbillpro.repository;

import com.travelbillpro.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT e FROM Employee e JOIN FETCH e.company WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR e.mobile LIKE CONCAT('%', :query, '%')")
    List<Employee> searchByNameOrMobile(String query);

    List<Employee> findByCompanyId(Long companyId);

    Page<Employee> findByCompanyId(Long companyId, Pageable pageable);
}
