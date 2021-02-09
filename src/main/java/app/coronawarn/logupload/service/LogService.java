package app.coronawarn.logupload.service;

import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    /**
     * Searches for a Log with given id.
     *
     * @param id id to search for.
     * @return the {@link LogEntity} or null if not found.
     */
    public LogEntity getLogEntity(String id) {
        return logRepository.getFirstById(id).orElse(null);
    }
}
