package com.josh.ardhi.services.impl;

import com.josh.ardhi.forms.ContactMessageForm;
import com.josh.ardhi.models.ContactMessage;
import com.josh.ardhi.repositories.ContactMessageRepository;
import com.josh.ardhi.services.ContactMessageService;
import org.springframework.stereotype.Service;

@Service
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactMessageServiceImpl(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Override
    public ContactMessage submit(ContactMessageForm form) {
        var message = ContactMessage.builder()
                .fullName(form.getFullName())
                .email(form.getEmail())
                .phone(form.getPhone())
                .subject(form.getSubject())
                .message(form.getMessage())
                .build();
        return contactMessageRepository.save(message);
    }

}
