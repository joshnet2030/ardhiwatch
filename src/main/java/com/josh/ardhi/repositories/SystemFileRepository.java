package com.josh.ardhi.repositories;

import com.josh.ardhi.models.SystemFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SystemFileRepository extends JpaRepository<SystemFile, UUID> {
}
