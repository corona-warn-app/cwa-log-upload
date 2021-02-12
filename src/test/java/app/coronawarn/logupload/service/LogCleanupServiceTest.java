package app.coronawarn.logupload.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import app.coronawarn.logupload.config.LogUploadConfig;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import com.amazonaws.services.s3.AmazonS3;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class LogCleanupServiceTest {

    @Autowired
    LogCleanupService service;

    @Autowired
    LogRepository logRepository;

    @MockBean
    AmazonS3 amazonS3Mock;

    @MockBean
    LogUploadConfig logUploadConfigMock;

    @BeforeEach
    public void setup() {
        logRepository.deleteAll();
    }


    @Test
    public void testLogFileDeleteJob() {
        given(logUploadConfigMock.getLogEntityLifetime()).willReturn(7);

        // should be deleted
        LogEntity le1 = new LogEntity("A", ZonedDateTime.now().minus(8, ChronoUnit.DAYS), "", 0, "", null);
        LogEntity le2 = new LogEntity("B", ZonedDateTime.now().minus(7, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS), "", 0, "", null);

        // should remain in database
        LogEntity le3 = new LogEntity("C", ZonedDateTime.now().minus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), "", 0, "", null);
        LogEntity le4 = new LogEntity("D", ZonedDateTime.now().minus(5, ChronoUnit.DAYS), "", 0, "", null);
        logRepository.saveAll(Arrays.asList(le1, le2, le3, le4));

        service.cleanup();

        List<LogEntity> remainingEntities = logRepository.findAll();

        assertEquals(2, remainingEntities.size());
        assertEquals(le3.getId(), remainingEntities.get(0).getId());
        assertEquals(le4.getId(), remainingEntities.get(1).getId());
    }
}
