package app.coronawarn.logupload.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.service.FileStorageService;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LogDownloadApiController.class)
@ContextConfiguration(classes = LogDownloadApiController.class)
@ActiveProfiles("portal")
public class LogDownloadApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileStorageService fileStorageServiceMock;

    private final static byte[] dummyBytes = new byte[] {0xD, 0xE, 0xA, 0xD, 0xB, 0xE, 0xE, 0xF};
    private final static String dummyFileName = "dummy.zip";
    private final static String dummyHash = "hash123456789";
    private final static String dummyLogId = "ABCDEFG";
    private final static LogEntity dummyLogEntity =
      new LogEntity(dummyLogId, ZonedDateTime.now(), dummyFileName, dummyBytes.length, dummyHash, "");

    @Test
    @WithMockJwtAuth
    public void testLogDownload() throws Exception {
        InputStream stream = new ByteArrayInputStream(dummyBytes);

        given(fileStorageServiceMock.downloadFile(eq(dummyLogId)))
          .willReturn(new FileStorageService.LogDownloadResponse(dummyLogEntity, stream));

        mockMvc.perform(get("/portal/api/logs/" + dummyLogId).header("Host", "localhost:8085"))
          .andExpectAll(
            status().isOk(),
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dummyFileName + "\""),
            header().longValue(HttpHeaders.CONTENT_LENGTH, dummyBytes.length),
            content().bytes(dummyBytes)
          );
    }

    @Test
    public void testLogDownloadShouldFailWithInvalidRole() throws Exception {
        InputStream stream = new ByteArrayInputStream(dummyBytes);

        given(fileStorageServiceMock.downloadFile(eq(dummyLogId)))
          .willReturn(new FileStorageService.LogDownloadResponse(dummyLogEntity, stream));

        mockMvc.perform(get("/portal/api/logs/" + dummyLogId).header("Host", "localhost:8085"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockJwtAuth
    public void testLogDownloadShouldReturn404IfFileNotFound() throws Exception {
        given(fileStorageServiceMock.downloadFile(eq(dummyLogId)))
          .willThrow(
            new FileStorageService.FileStoreException(FileStorageService.FileStoreException.Reason.FILE_NOT_FOUND));

        mockMvc.perform(get("/portal/api/logs/" + dummyLogId).header("Host", "localhost:8085"))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockJwtAuth
    public void testLogDownloadShouldReturn500IfDownloadFails() throws Exception {
        given(fileStorageServiceMock.downloadFile(eq(dummyLogId)))
          .willThrow(
            new FileStorageService.FileStoreException(FileStorageService.FileStoreException.Reason.S3_DOWNLOAD_FAILED));

        mockMvc.perform(get("/portal/api/logs/" + dummyLogId).header("Host", "localhost:8085"))
          .andExpect(status().isInternalServerError());

    }
}
