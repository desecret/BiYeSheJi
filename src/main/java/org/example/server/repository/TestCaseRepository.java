package org.example.server.repository;

import org.example.server.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
    Optional<TestCaseEntity> findByName(String name);

    @Modifying
    @Query("DELETE FROM TestCaseStepEntity s WHERE s.testCase.id = :testCaseId")
    void deleteStepsByTestCaseId(@Param("testCaseId") Long testCaseId);
}
