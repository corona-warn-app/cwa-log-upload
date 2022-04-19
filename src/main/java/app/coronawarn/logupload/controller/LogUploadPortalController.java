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

import app.coronawarn.logupload.config.LogUploadConfig;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.service.LogService;
import java.security.Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * This class represents the WEB UI controller for the verification portal. It implements a very
 * simple HTML interface with one submit button to get and show a newly generated TeleTAN.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Profile("portal")
public class LogUploadPortalController {

    /**
     * Routes.
     */
    private static final String ROUTE_LOGOUT = "/portal/logout";
    private static final String ROUTE_START = "/portal/start";
    private static final String ROUTE_SEARCH = "/portal/search";
    private static final String ROUTE_INDEX = "/";

    /**
     * Template properties.
     */
    private static final String ATTR_USER = "userName";
    private static final String ATTR_PW_RESET_URL = "pwResetUrl";
    private static final String ATTR_LOG_ID = "logId";
    private static final String ATTR_LOG_ENTITY = "logEntity";
    private static final String ATTR_LOG_FILESIZE_HR = "fileSizeHr";

    /**
     * Template names.
     */
    private static final String TEMPLATE_START = "start";
    private static final String TEMPLATE_SEARCH = "search";
    private static final String TEMPLATE_NOT_FOUND = "not_found";

    private final LogUploadConfig logUploadConfig;
    private final LogService logService;

    /**
     * The Get request to log out from the portal web site.
     */
    @PostMapping(ROUTE_LOGOUT)
    public String logout(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            log.error("Logout failed", e);
        }
        return "redirect:" + TEMPLATE_START;
    }

    /**
     * Request for start template.
     */
    @GetMapping(ROUTE_START)
    public String start(HttpServletRequest request, Model model) {
        addUserDetailsToModel(request, model);
        model.addAttribute(ATTR_LOG_ID, "");

        return TEMPLATE_START;
    }

    /**
     * Request for search template.
     */
    @PostMapping(ROUTE_SEARCH)
    public String search(HttpServletRequest request, Model model, @ModelAttribute(ATTR_LOG_ID) String logId) {
        addUserDetailsToModel(request, model);

        log.info("Searching log by id");

        LogEntity logEntity = logService.getLogEntity(logId);

        if (logEntity == null) {
            return TEMPLATE_NOT_FOUND;
        }

        model.addAttribute(ATTR_LOG_ENTITY, logEntity);
        model.addAttribute(ATTR_LOG_FILESIZE_HR, FileUtils.byteCountToDisplaySize(logEntity.getSize()));

        return TEMPLATE_SEARCH;
    }

    /**
     * Request for index.
     */
    @GetMapping(ROUTE_INDEX)
    public ResponseEntity<Void> index() {
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, ROUTE_START)
            .build();

    }

    private void addUserDetailsToModel(HttpServletRequest request, Model model) {
        Principal principal = request.getUserPrincipal();
        request.getUserPrincipal().getName();
        String user = principal.getName();

        if (model != null) {
            model.addAttribute(ATTR_USER, user);
            model.addAttribute(ATTR_PW_RESET_URL, logUploadConfig.getKeycloakPwResetUrl());
        }
    }
}
