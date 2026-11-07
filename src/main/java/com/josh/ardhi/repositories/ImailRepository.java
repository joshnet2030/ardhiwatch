package com.josh.ardhi.repositories;

import com.josh.ardhi.models.Imail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImailRepository extends JpaRepository<Imail, UUID> {
    List<Imail> findAllByWasSentSuccessfullyIsFalseAndSendingAttemptCountLessThan(boolean wasSentSuccessfully, int sendingAttemptCountIsLessThan);

    List<Imail> findAllByWasSentSuccessfullyIsFalseAndSendingAttemptCountLessThan(int maxSendingAttempt);
}
