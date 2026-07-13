package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.domain.SlackDeliveryType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlackDeliveryRepository extends JpaRepository<SlackDelivery, Long> {

    boolean existsBySlackChannelAndDeliveryDateAndDeliveryType(
            SlackChannel slackChannel,
            LocalDate deliveryDate,
            SlackDeliveryType deliveryType
    );

    boolean existsBySlackChannelAndStatusIn(
            SlackChannel slackChannel,
            Collection<SlackDeliveryStatus> statuses
    );

    Optional<SlackDelivery> findTopBySlackChannelAndDeliveryTypeAndStatusOrderByWindowEndedAtDesc(
            SlackChannel slackChannel,
            SlackDeliveryType deliveryType,
            SlackDeliveryStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from SlackDelivery delivery where delivery.id = :deliveryId")
    Optional<SlackDelivery> findByIdForUpdate(@Param("deliveryId") Long deliveryId);

    @Query("""
            select delivery.id
            from SlackDelivery delivery
            where delivery.status = :pending
               or (delivery.status = :retryWaiting and delivery.nextRetryAt <= :now)
            order by delivery.id
            """)
    List<Long> findReadyDeliveryIds(
            @Param("pending") SlackDeliveryStatus pending,
            @Param("retryWaiting") SlackDeliveryStatus retryWaiting,
            @Param("now") LocalDateTime now
    );

    List<SlackDelivery> findByStatusAndProcessingStartedAtBefore(
            SlackDeliveryStatus status,
            LocalDateTime threshold
    );
}
