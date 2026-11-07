package com.josh.ardhi.scheduler;

import com.josh.ardhi.models.Imail;
import com.josh.ardhi.repositories.ImailRepository;
import com.josh.ardhi.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@Slf4j
public class EmailScheduler {

    private final ImailRepository imailRepository;
    private final EmailService emailService;

    public EmailScheduler(ImailRepository imailRepository, EmailService emailService) {
        this.imailRepository = imailRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 2000L )
    public void sendPendingEmails(){
        List<Imail> pendingEmails = imailRepository.findAllByWasSentSuccessfullyIsFalseAndSendingAttemptCountLessThan(3);
        for(Imail imail: pendingEmails){
           log.info("Sending email: {}", imail.getToEmail());
            imail.setSendingAttemptCount(imail.getSendingAttemptCount()+1);
            imail.setLastSendingAttemptAt(ZonedDateTime.now());
           try {
               emailService.sendEmail(imail);
               imail.setWasSentSuccessfully(true);
           }catch(Throwable e){
               imail.setFailureMessage(e.getMessage());
           }
           imailRepository.save(imail);
        }
    }
}
