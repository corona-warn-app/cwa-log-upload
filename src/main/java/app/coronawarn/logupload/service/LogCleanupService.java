package app.coronawarn.logupload.service;

import app.coronawarn.logupload.config.LogUploadConfig;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("api")
public class LogCleanupService {

    private final LogRepository logRepository;
    private final LogUploadConfig config;
    private final FileStorageService fileStorageService;

    /**
     * Cleanup job for automated deletion of {@link LogEntity} older then a specified (e.g. 7 days) amount of time.
     */
    @Scheduled(cron = "${log-upload.cleanup-cron}")
    @SchedulerLock(name = "LogCleanupService_cleanup", lockAtLeastFor = "PT0S",
        lockAtMostFor = "PT30M")
    public void cleanup() {

        log.info("Starting log cleanup job");

        ZonedDateTime threshold = ZonedDateTime.now().minus(config.getLogEntityLifetime(), ChronoUnit.DAYS);

        log.info("Threshold date is {}", threshold);

        List<LogEntity> logEntities = logRepository.findByCreatedAtBefore(threshold);
        logEntities.forEach(fileStorageService::deleteFileSafe);

        log.info("Finished log cleanup job");

    }
}
