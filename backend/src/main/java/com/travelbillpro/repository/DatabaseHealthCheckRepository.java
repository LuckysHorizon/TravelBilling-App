package com.travelbillpro.repository;

import com.travelbillpro.entity.DatabaseHealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatabaseHealthCheckRepository extends JpaRepository<DatabaseHealthCheck, Long> {

    /** All results from a single keep-alive execution */
    List<DatabaseHealthCheck> findByExecutionId(UUID executionId);

    /** Latest ping for a specific database */
    Optional<DatabaseHealthCheck> findTopByDatabaseIdOrderByCheckedAtDesc(String databaseId);

    /** Last successful ping for a specific database */
    Optional<DatabaseHealthCheck> findTopByDatabaseIdAndSuccessTrueOrderByCheckedAtDesc(String databaseId);

    /** Last failed ping for a specific database */
    Optional<DatabaseHealthCheck> findTopByDatabaseIdAndSuccessFalseOrderByCheckedAtDesc(String databaseId);

    /** All health checks after a given timestamp */
    List<DatabaseHealthCheck> findByCheckedAtAfter(LocalDateTime since);

    /** Latest execution's checks (most recent execution overall) */
    @Query("SELECT h FROM DatabaseHealthCheck h WHERE h.executionId = " +
           "(SELECT h2.executionId FROM DatabaseHealthCheck h2 ORDER BY h2.checkedAt DESC LIMIT 1)")
    List<DatabaseHealthCheck> findLatestExecution();

    /** All distinct database IDs ever checked */
    @Query("SELECT DISTINCT h.databaseId FROM DatabaseHealthCheck h")
    List<String> findAllDistinctDatabaseIds();

    /** Cleanup: delete records older than the given timestamp */
    @Modifying
    @Query("DELETE FROM DatabaseHealthCheck h WHERE h.checkedAt < :cutoff")
    int deleteByCheckedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
