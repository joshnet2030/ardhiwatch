package com.josh.ardhi.repositories;

import com.josh.ardhi.models.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppSessionRepository extends JpaRepository<AppSession, UUID> {

}
