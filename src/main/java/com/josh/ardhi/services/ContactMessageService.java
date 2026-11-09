package com.josh.ardhi.services;

import com.josh.ardhi.forms.ContactMessageForm;
import com.josh.ardhi.models.ContactMessage;

public interface ContactMessageService {

    ContactMessage submit(ContactMessageForm form);

}
