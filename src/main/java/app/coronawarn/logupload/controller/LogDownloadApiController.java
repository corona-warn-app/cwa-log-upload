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

package app.coronawarn.logupload.controller;

import app.coronawarn.logupload.service.FileStorageService;
import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping(value = "/portal/api/")
@RequiredArgsConstructor
@Profile("portal")
public class LogDownloadApiController {

    private final FileStorageService storageService;

    /**
     * Endpoint for downloading a log file.
     *
     * @param id the ID of the log file.
     * @return ResponseEntity with binary data.
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<Resource> downloadLogfile(@PathVariable("id") String id) {
        FileStorageService.LogDownloadResponse logDownloadResponse;

        try {
            logDownloadResponse = storageService.downloadFile(id);
        } catch (FileStorageService.FileStoreException e) {
            if (e.getReason() == FileStorageService.FileStoreException.Reason.FILE_NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        Resource resource = new InputStreamResource(logDownloadResponse.getInputStream());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + logDownloadResponse.getLogEntity().getFilename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(logDownloadResponse.getLogEntity().getSize()))
                .body(resource);
    }
}
