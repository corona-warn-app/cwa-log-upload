/*
 * Corona-Warn-App / cwa-log-upload
 *
 * (C) 2021 - 2022, T-Systems International GmbH
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
