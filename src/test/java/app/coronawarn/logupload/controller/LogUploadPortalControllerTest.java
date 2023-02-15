package app.coronawarn.logupload.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import app.coronawarn.logupload.config.LogUploadConfig;
import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.service.FileStorageService;
import app.coronawarn.logupload.service.LogService;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import java.time.ZonedDateTime;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LogUploadPortalController.class)
@ContextConfiguration(classes = LogUploadPortalController.class)
@ActiveProfiles("portal")
public class LogUploadPortalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileStorageService fileStorageServiceMock;

    @MockBean
    LogUploadConfig logUploadConfigMock;

    @MockBean
    LogService logServiceMock;

    private final static String dummyFileName = "dummy.zip";
    private final static String dummyHash = "hash123456789";
    private final static String dummyLogId = "ABCDEFG";
    private final static LogEntity dummyLogEntity =
      new LogEntity(dummyLogId, ZonedDateTime.now(), dummyFileName, 8, dummyHash, "");
    private final static String dummyPwResetUrl = "https://dummy.de";
    private final static String dummyUsername = "dummy";

    @BeforeEach
    public void setup() {
        given(logUploadConfigMock.getKeycloakPwResetUrl()).willReturn(dummyPwResetUrl);
    }

    @Test
    @WithMockJwtAuth(claims = @OpenIdClaims(sub = dummyUsername))
    public void testPortalStartPage() throws Exception {
        mockMvc.perform(get("/portal/start").header("Host", "localhost:8085"))
          .andExpectAll(
            status().isOk(),
            view().name("start"),
            model().attribute("logId", ""),
            model().attribute("pwResetUrl", dummyPwResetUrl),
            model().attribute("userName", dummyUsername)
          );
    }

    @Test
    @WithMockJwtAuth(claims = @OpenIdClaims(sub = dummyUsername))
    public void testPortalIndexRedirect() throws Exception {
        mockMvc.perform(get("/").header("Host", "localhost:8085"))
          .andExpectAll(
            status().isFound(),
            header().string(HttpHeaders.LOCATION, "/portal/start")
          );
    }

    @Test
    @WithMockJwtAuth(claims = @OpenIdClaims(sub = dummyUsername))
    public void testPortalSearchPage() throws Exception {
        given(logServiceMock.getLogEntity(eq(dummyLogId)))
          .willReturn(dummyLogEntity);

        mockMvc.perform(post("/portal/search")
            .param("logId", dummyLogId)
            .with(csrf().asHeader())
            .header("Host", "localhost:8085"))
          .andExpectAll(
            status().isOk(),
            view().name("search"),
            model().attribute("logEntity", dummyLogEntity),
            model().attribute("fileSizeHr", FileUtils.byteCountToDisplaySize(dummyLogEntity.getSize())),
            model().attribute("pwResetUrl", dummyPwResetUrl),
            model().attribute("userName", dummyUsername)
          );
    }

    @Test
    @WithMockJwtAuth(claims = @OpenIdClaims(sub = dummyUsername))
    public void testPortalSearchPageNotFound() throws Exception {
        given(logServiceMock.getLogEntity(eq(dummyLogId)))
          .willReturn(null);

        mockMvc.perform(post("/portal/search")
            .param("logId", dummyLogId)
            .with(csrf().asHeader())
            .header("Host", "localhost:8085"))
          .andExpectAll(
            status().isOk(),
            view().name("not_found")
          );
    }
}
