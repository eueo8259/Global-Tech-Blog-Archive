package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackDailyDigestRun;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackDailyDigestRunRepository extends JpaRepository<SlackDailyDigestRun, Long> {

    Optional<SlackDailyDigestRun> findByDeliveryDate(LocalDate deliveryDate);
}
