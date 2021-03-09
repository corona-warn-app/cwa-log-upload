package app.coronawarn.logupload.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"els-verify.tls.enabled=false"})
public class LogServiceTest {

    @Autowired
    LogService service;

    @MockBean
    LogRepository logRepositoryMock;

    @Test
    public void testGetLogEntity() {
        String id = "XXX";
        LogEntity entity = new LogEntity(null, null, null, 0, null, null);

        given(logRepositoryMock.findById(eq(id))).willReturn(Optional.of(entity));

        assertEquals(entity, service.getLogEntity(id));
    }
}
