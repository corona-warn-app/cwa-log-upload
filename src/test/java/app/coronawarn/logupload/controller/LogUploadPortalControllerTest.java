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

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.WithMockKeycloakAuth;
import com.c4_soft.springaddons.security.oauth2.test.mockmvc.ServletUnitTestingSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@WebMvcTest(LogUploadPortalController.class)
@TestPropertySource(properties = {"rateLimiting.enabled=true", "rateLimiting.seconds=30"})
@ContextConfiguration(classes = LogUploadPortalController.class)
public class LogUploadPortalControllerTest extends ServletUnitTestingSupport {

    public static final String TELETAN_NAME = "teletan";
    public static final String TELETAN_VALUE = "TeleTAN";
    private static final String TOKEN_ATTR_NAME = "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";

    private HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository;
    private CsrfToken csrfToken;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    public void setup() {
        httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());
    }

    /**
     * Test of index method, of class VerificationPortalController.
     *
     * @throws Exception if the test cannot be performed.
     */
    @Test
    @WithMockKeycloakAuth("Role_Test")
    public void testIndex() throws Exception {
        log.info("process testIndex()");
        mockMvc.perform(get("/cwa"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    /**
     * Test of start method, of class VerificationPortalController.
     *
     * @throws Exception if the test cannot be performed.
     */
    @Test
    @WithMockKeycloakAuth(name = "tester", value = "Role_Test")
    public void testStart() throws Exception {
        log.info("process testStart() RequestMethod.GET");
        mockMvc.perform(get("/cwa/start"))
                .andExpect(status().isOk())
                .andExpect(view().name("start"))
                .andExpect(model().attribute("userName", equalTo("tester")))
                .andExpect(request().sessionAttribute(TELETAN_NAME, equalTo(TELETAN_VALUE)));

        String TOKEN_ATTR_NAME = "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";

        log.info("process testStart() RequestMethod.POST");
        mockMvc.perform(post("/cwa/start")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken())
                .sessionAttr(TELETAN_NAME, TELETAN_VALUE).param(TELETAN_NAME, TELETAN_VALUE))
                .andExpect(status().isOk())
                .andExpect(view().name("start"))
                .andExpect(model().attribute("userName", equalTo("tester")))
                .andExpect(request().sessionAttribute(TELETAN_NAME, equalTo(TELETAN_VALUE)));
    }

    /**
     * Test of start method, of class VerificationPortalController.
     *
     * @throws Exception if the test cannot be performed.
     */
    @Test
    @WithMockKeycloakAuth(name = "tester", value = "Role_Test")
    public void testStartNotFound() throws Exception {
        log.info("process testStartNotFound()");
        mockMvc.perform(get("/corona/start"))
                .andExpect(status().isNotFound());
    }


    /**
     * Test of logout method, of class VerificationPortalController.
     *
     * @throws Exception if the test cannot be performed.
     */
    @Test
    @WithMockKeycloakAuth("Role_Test")
    public void testLogout() throws Exception {
        log.info("process testLogout()");

        mockMvc.perform(post("/cwa/logout")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
                .andExpect(redirectedUrl("start"))
                .andExpect(status().isFound());
    }

}
