package com.globaltechblogarchive.slack.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class SlackDeliveryRepositoryTest extends MySqlIntegrationTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 7, 12, 9, 0);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 7, 13, 9, 0);

    @Autowired
    private SlackDeliveryRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void uniqueConstraintRejectsSameChannelAndDate() {
        SlackChannel channel = persistChannel();
        entityManager.persist(SlackDelivery.pending(channel, WINDOW_END.toLocalDate(), WINDOW_START, WINDOW_END));
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.persist(SlackDelivery.pending(
                    channel,
                    WINDOW_END.toLocalDate(),
                    WINDOW_START.minusDays(1),
                    WINDOW_END
            ));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void findReadyDeliveryIdsExcludesSentAndFutureRetry() {
        SlackChannel pendingChannel = persistChannel("C-PENDING");
        SlackChannel readyRetryChannel = persistChannel("C-READY");
        SlackChannel futureRetryChannel = persistChannel("C-FUTURE");
        SlackChannel sentChannel = persistChannel("C-SENT");
        SlackDelivery pending = persistDelivery(pendingChannel);
        SlackDelivery readyRetry = persistDelivery(readyRetryChannel);
        readyRetry.startProcessing(WINDOW_END);
        readyRetry.markRetryWaiting(WINDOW_END.plusMinutes(1), "ratelimited", "retry");
        SlackDelivery futureRetry = persistDelivery(futureRetryChannel);
        futureRetry.startProcessing(WINDOW_END);
        futureRetry.markRetryWaiting(WINDOW_END.plusMinutes(10), "ratelimited", "retry");
        SlackDelivery sent = persistDelivery(sentChannel);
        sent.startProcessing(WINDOW_END);
        sent.markSent(WINDOW_END, "123.456");
        entityManager.flush();

        assertThat(repository.findReadyDeliveryIds(
                SlackDeliveryStatus.PENDING,
                SlackDeliveryStatus.RETRY_WAITING,
                WINDOW_END.plusMinutes(5)
        )).containsExactly(pending.getId(), readyRetry.getId());
    }

    private SlackDelivery persistDelivery(SlackChannel channel) {
        SlackDelivery delivery = SlackDelivery.pending(
                channel,
                WINDOW_END.toLocalDate(),
                WINDOW_START,
                WINDOW_END
        );
        entityManager.persist(delivery);
        return delivery;
    }

    private SlackChannel persistChannel() {
        return persistChannel("C-UNIQUE");
    }

    private SlackChannel persistChannel(String channelId) {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T-" + channelId,
                "Workspace " + channelId,
                "encrypted-token",
                "B-" + channelId,
                "commands",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        entityManager.persist(workspace);
        SlackChannel channel = SlackChannel.create(workspace, channelId, "digest");
        entityManager.persist(channel);
        return channel;
    }
}
