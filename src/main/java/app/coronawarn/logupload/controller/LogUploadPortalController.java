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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * This class represents the WEB UI controller for the verification portal. It implements a very
 * simple HTML interface with one submit button to get and show a newly generated TeleTAN.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LogUploadPortalController {

    /**
     * The route to log out from the portal web site.
     */
    private static final String ROUTE_LOGOUT = "/cwa/logout";

    /**
     * The html Thymeleaf template for the TeleTAN portal start web site.
     */
    private static final String TEMPLATE_START = "start";

    /**
     * The Get request to log out from the portal web site.
     *
     * @param request the http request object
     * @return the redirect path after the logout
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
}
