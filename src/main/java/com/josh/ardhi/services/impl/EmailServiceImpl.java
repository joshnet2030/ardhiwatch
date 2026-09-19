package com.josh.ardhi.services.impl;

import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.Imail;
import com.josh.ardhi.repositories.ImailRepository;
import com.josh.ardhi.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final ImailRepository imailRepository;
    @Value("${app.from.email}")
    public String fromEmail;

    public EmailServiceImpl(ImailRepository imailRepository) {
        this.imailRepository = imailRepository;
    }

    @Override
    public void queueEmail(String toEmail, String subject, String body) {
       Imail imail = Imail.builder()
               .fromEmail(fromEmail)
               .toEmail(toEmail)
               .subject(subject)
               .body(body)
               .build();
       imailRepository.save(imail);
    }

    @Override
    public void sendEmail(Imail imail) {
        log.info("\nEMAIL \n From: {}\n To: {}\n Subject: {} \n Body: {} ", imail.getFromEmail(),
                imail.getToEmail(), imail.getSubject(), imail.getBody());
    }
}
