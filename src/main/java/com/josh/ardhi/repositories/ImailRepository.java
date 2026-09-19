package com.josh.ardhi.repositories;

import com.josh.ardhi.models.Imail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImailRepository extends JpaRepository<Imail, UUID> {
}
