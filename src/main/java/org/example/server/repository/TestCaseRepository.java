package org.example.server.repository;

import org.example.server.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
    Optional<TestCaseEntity> findByName(String name);
}
