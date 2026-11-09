package com.josh.ardhi.repositories;

import com.josh.ardhi.models.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findAllByIsReadFalseOrderByCreatedAtDesc();

}
