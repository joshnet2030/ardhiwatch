package com.josh.ardhi.services.impl;

import com.josh.ardhi.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.from.email}")
    public String fromEmail;

    @Override
    public void sendEmail( String toEmail, String subject, String body) {
        log.info("\nEMAIL \n From: {}\n To: {}\n Subject: {} \n Body: {} ", fromEmail, toEmail, subject, body);
    }
}
