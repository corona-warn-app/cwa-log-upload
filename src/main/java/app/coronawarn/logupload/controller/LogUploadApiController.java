/*
 * Corona-Warn-App / cwa-log-upload
 *
 * (C) 2021, T-Systems International GmbH
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

package app.coronawarn.logupload.controller;

import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.model.LogUploadResponse;
import app.coronawarn.logupload.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
@Profile("api")
public class LogUploadApiController {

    private final FileStorageService storageService;

    /**
     * Endpoint for uploading log files.
     *
     * @param file the multipart file upload
     * @return id and hash of the uploaded log file.
     */
    @Operation(
        operationId = "uploadLogFile",
        summary = "Uploads a log file from CWA to log-upload server.",
        tags = {"PUBLIC"},
        method = "POST"
    )
    @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Object containing the ID and MD5 hash of the uploaded log.",
          content = @Content(schema = @Schema(implementation = LogUploadResponse.class))
          ),
      @ApiResponse(
          responseCode = "500",
          description = "The upload of the log file has failed.",
          content = @Content(schema = @Schema(hidden = true))
          )
    })
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            value = "/logs"
    )
    public ResponseEntity<LogUploadResponse> uploadLogFile(
            @Parameter(description = "The file to upload", required = true)
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Got file: {}, {}", file.getOriginalFilename(), file.getSize());

        LogEntity logEntity;

        String filename = "unknown";

        if (file.getOriginalFilename() != null) {
            filename = file.getOriginalFilename().length() > 100
                    ? file.getOriginalFilename().substring(0, 100)
                    : file.getOriginalFilename();
        }

        try {
            logEntity =
                    storageService.storeFileStream(filename, file.getSize(), file.getInputStream());
        } catch (FileStorageService.FileStoreException e) {
            log.error("Failed to save log file.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Saved log file in db with id {}", logEntity.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new LogUploadResponse(logEntity.getId(), logEntity.getHash()));
    }
}
