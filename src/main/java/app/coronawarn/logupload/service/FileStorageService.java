/*
 * Corona-Warn-App / cwa-verification
 *
 * (C) 2021, T-Systems International GmbH
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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;
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
    public LogEntity storeFileStream(String fileName, long size, InputStream stream) throws FileStoringException {
        log.info("Persisting file stream");

        String id;
        Optional<LogEntity> existingLogEntity;

        // Checking if generated ID already exists
        do {
            id = generateLogId();
            existingLogEntity = logRepository.getFirstById(id);
        } while (existingLogEntity.isPresent());


        PutObjectResult putObjectResult;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            
            putObjectResult = s3Client.putObject(s3Config.getBucketName(), id, stream, metadata);
            log.info("File stored to S3 with id {}", id);
        } catch (SdkClientException e) {
            log.error("Upload to S3 bucket failed.", e);
            throw new FileStoringException(FileStoringException.Reason.S3_UPLOAD_FAILED);
        }

        log.info("Storing LogEntity to database");
        LogEntity logEntity =
                new LogEntity(id, LocalDateTime.now(), fileName, size, putObjectResult.getContentMd5(), "");

        return logRepository.save(logEntity);
    }

    private String generateLogId() {
        byte[] bytes = new byte[logUploadConfig.getLogIdByteLength()];
        Random random = new Random();

        random.nextBytes(bytes);

        return DatatypeConverter.printHexBinary(bytes);
    }

    @Getter
    public static class FileStoringException extends Exception {
        private final Reason reason;

        FileStoringException(Reason reason) {
            super();
            this.reason = reason;
        }

        public enum Reason {
            S3_UPLOAD_FAILED
        }
    }
}
