package app.coronawarn.logupload.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.logupload.model.LogEntity;
import app.coronawarn.logupload.service.FileStorageService;
import app.coronawarn.logupload.service.OtpService;
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.WithMockKeycloakAuth;
import java.io.InputStream;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = LogUploadApiController.class)
@ContextConfiguration(classes = {LogUploadApiController.class})
@ActiveProfiles("api")
public class LogUploadApiControllerTest {


    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileStorageService fileStorageServiceMock;

    @MockBean
    OtpService otpServiceMock;

    private final static byte[] dummyBytes = new byte[]{0xD, 0xE, 0xA, 0xD, 0xB, 0xE, 0xE, 0xF};
    private final static String dummyFileName = "dummy.zip";
    private final static String dummyHash = "hash123456789";
    private final static String dummyLogId = "ABCDEFG";
    private final static String OTP_HEADER = "cwa-otp";
    private final static LogEntity dummyLogEntity = new LogEntity(dummyLogId, ZonedDateTime.now(), dummyFileName, dummyBytes.length, dummyHash, "");
    private final static String testOtp = "ea8166fa-6a42-426a-8b14-a4c53ff710b5";

    @Test
    @WithMockKeycloakAuth
    public void testLogUpload() throws Exception {
        ArgumentCaptor<InputStream> streamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);

        given(fileStorageServiceMock.storeFileStream(eq(dummyFileName), eq(Long.valueOf(dummyBytes.length)), streamArgumentCaptor.capture()))
            .willReturn(dummyLogEntity);

        given(otpServiceMock.verifyOtp(eq(testOtp))).willReturn(true);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", dummyFileName, "", dummyBytes);
        mockMvc.perform(multipart("/api/logs")
            .file(mockMultipartFile)
            .with(csrf().asHeader())
            .header(OTP_HEADER, testOtp)
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        )
            .andExpect(ResultMatcher.matchAll(
                status().isCreated(),
                jsonPath("hash").value(dummyHash),
                jsonPath("id").value(dummyLogId)
            ));

        assertArrayEquals(dummyBytes, streamArgumentCaptor.getValue().readAllBytes());
    }

    @Test
    @WithMockKeycloakAuth
    public void testLogUploadShouldFailIfFileNotSent() throws Exception {

        mockMvc.perform(multipart("/api/logs")
            .with(csrf().asHeader())
            .header(OTP_HEADER, testOtp)
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockKeycloakAuth
    public void testLogUploadShouldReturn500IfUploadFailed() throws Exception {

        given(otpServiceMock.verifyOtp(eq(testOtp))).willReturn(true);

        given(fileStorageServiceMock.storeFileStream(eq(dummyFileName), eq(Long.valueOf(dummyBytes.length)), any(InputStream.class)))
            .willThrow(new FileStorageService.FileStoreException(FileStorageService.FileStoreException.Reason.S3_UPLOAD_FAILED));

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", dummyFileName, "", dummyBytes);
        mockMvc.perform(multipart("/api/logs")
            .file(mockMultipartFile)
            .with(csrf().asHeader())
            .header(OTP_HEADER, testOtp)
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockKeycloakAuth
    public void testLogUploadShouldFailWithWrongOTP() throws Exception {
        given(otpServiceMock.verifyOtp(eq(testOtp))).willReturn(false);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", dummyFileName, "", dummyBytes);
        mockMvc.perform(multipart("/api/logs")
            .file(mockMultipartFile)
            .with(csrf().asHeader())
            .header(OTP_HEADER, testOtp)
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(fileStorageServiceMock);
    }

    @Test
    @WithMockKeycloakAuth
    public void testLogUploadShouldFailWithoutOTP() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", dummyFileName, "", dummyBytes);
        mockMvc.perform(multipart("/api/logs")
            .file(mockMultipartFile)
            .with(csrf().asHeader())
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(fileStorageServiceMock);
        verifyNoInteractions(otpServiceMock);
    }

    @Test
    @WithMockKeycloakAuth
    public void testLogUploadShouldFailWithInvalidOTP() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", dummyFileName, "", dummyBytes);
        mockMvc.perform(multipart("/api/logs")
            .file(mockMultipartFile)
            .with(csrf().asHeader())
            .header(OTP_HEADER, "no-uuid")
            .header(HttpHeaders.HOST, "localhost:8085")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(fileStorageServiceMock);
        verifyNoInteractions(otpServiceMock);
    }

}
