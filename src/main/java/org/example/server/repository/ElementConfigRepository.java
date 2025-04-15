package org.example.server.repository;

import org.example.server.entity.ElementConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElementConfigRepository extends JpaRepository<ElementConfigEntity, Long> {
    List<ElementConfigEntity> findByContext(String context);
    Optional<ElementConfigEntity> findByContextAndName(String context, String name);
}
