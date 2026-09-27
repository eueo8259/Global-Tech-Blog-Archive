package com.globaltechblogarchive.slack.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.slack.domain.SlackDailyDigestRun;
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
class SlackDailyDigestRunRepositoryTest extends MySqlIntegrationTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2031, 8, 3);
    private static final LocalDateTime STARTED_AT = DELIVERY_DATE.atTime(9, 0);

    @Autowired
    private SlackDailyDigestRunRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void uniqueConstraintRejectsSameDeliveryDate() {
        entityManager.persist(SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT));
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.persist(SlackDailyDigestRun.start(
                    DELIVERY_DATE,
                    STARTED_AT.plusMinutes(1),
                    STARTED_AT.plusMinutes(1)
            ));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void findByDeliveryDateReturnsStoredRun() {
        SlackDailyDigestRun run = entityManager.persist(
                SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByDeliveryDate(DELIVERY_DATE))
                .get()
                .extracting(SlackDailyDigestRun::getId)
                .isEqualTo(run.getId());
    }
}
