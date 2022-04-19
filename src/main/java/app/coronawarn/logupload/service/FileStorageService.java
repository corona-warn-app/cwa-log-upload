/*
 * Corona-Warn-App / cwa-verification
 *
 * (C) 2021 - 2022, T-Systems International GmbH
 *
 * Deutsche Telekom AG, SAP AG and all other contributors /
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
import app.coronawarn.logupload.config.LogUploadS3Config;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.repository.LogRepository;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final LogUploadS3Config s3Config;
    private final LogUploadConfig logUploadConfig;
    private final AmazonS3 s3Client;
    private final LogRepository logRepository;

    /**
     * Stores the given FileStream in configured S3 bucket and creates an entry in LogFile database table.
     *
     * @param stream the FileInputStream of the file to store.
     * @return an LogEntity object with information about the stored file.
     */
    public LogEntity storeFileStream(String fileName, long size, InputStream stream) throws FileStoreException {
        log.info("Persisting file stream");

        String id;
        Optional<LogEntity> existingLogEntity;

        // Checking if generated ID already exists
        do {
            id = generateLogId();
            existingLogEntity = logRepository.findById(id);
        } while (existingLogEntity.isPresent());


        PutObjectResult putObjectResult;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);

            putObjectResult = s3Client.putObject(s3Config.getBucketName(), id, stream, metadata);
            log.info("File stored to S3 with id {}", id);
        } catch (SdkClientException e) {
            log.error("Upload to S3 bucket failed.", e);
            throw new FileStoreException(FileStoreException.Reason.S3_UPLOAD_FAILED);
        }

        log.info("Storing LogEntity to database");
        LogEntity logEntity =
            new LogEntity(id, ZonedDateTime.now(), fileName, size, putObjectResult.getContentMd5(), "");

        return logRepository.save(logEntity);
    }

    /**
     * Method to download a log file from S3 bucket. This method returns
     * an {@link InputStream} which can be used to get the file.
     *
     * @param id the ID of the file to download.
     * @return {@link LogDownloadResponse} object containing the {@link InputStream}
     *     of the file and a {@link LogEntity} with meta-data of the file.
     * @throws FileStoreException if anything went wrong during download.
     */
    public LogDownloadResponse downloadFile(String id) throws FileStoreException {
        Optional<LogEntity> entity = logRepository.findById(id);

        if (entity.isEmpty()) {
            throw new FileStoreException(FileStoreException.Reason.FILE_NOT_FOUND);
        }

        S3Object s3Object;
        try {
            s3Object = s3Client.getObject(s3Config.getBucketName(), entity.get().getId());
        } catch (SdkClientException e) {
            log.error("Failed to download log file from S3 bucket.", e);
            throw new FileStoreException(FileStoreException.Reason.S3_DOWNLOAD_FAILED);
        }

        log.info("Got file from S3 bucket with id {}", entity.get().getId());

        return new LogDownloadResponse(entity.get(), s3Object.getObjectContent());
    }

    /**
     * Delete a log file from S3 bucket and database.
     * This method does not throw any exception if something goes wrong.
     * In case of a misbehaviour only a information in the log file will be written.
     *
     * @param logEntity the {@link LogEntity} to be deleted.
     */
    public void deleteFileSafe(LogEntity logEntity) {
        try {
            log.info("Deleting object with id {} from bucket", logEntity.getId());
            s3Client.deleteObject(s3Config.getBucketName(), logEntity.getId());
        } catch (SdkClientException e) {
            log.error("Failed to delete log file from bucket.", e);
        }

        log.info("Deleting entity for log with id {} from database", logEntity.getId());
        logRepository.delete(logEntity);
    }

    private String generateLogId() {
        byte[] bytes = new byte[logUploadConfig.getLogIdByteLength()];
        Random random = new Random();

        random.nextBytes(bytes);

        return DatatypeConverter.printHexBinary(bytes);
    }

    @Getter
    @AllArgsConstructor
    public static class LogDownloadResponse {
        private final LogEntity logEntity;
        private final InputStream inputStream;
    }

    @Getter
    public static class FileStoreException extends Exception {
        private final Reason reason;

        public FileStoreException(Reason reason) {
            super();
            this.reason = reason;
        }

        public enum Reason {
            S3_UPLOAD_FAILED, S3_DOWNLOAD_FAILED, S3_DELETE_FAILED, FILE_NOT_FOUND
        }
    }
}
