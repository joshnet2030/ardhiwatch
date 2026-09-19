package com.josh.ardhi.services;

import com.josh.ardhi.models.Imail;

public interface EmailService {

    public void queueEmail(String toEmail, String subject, String body);

    public void sendEmail(Imail imail);
}
