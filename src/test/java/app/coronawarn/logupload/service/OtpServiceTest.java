package app.coronawarn.logupload.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import app.coronawarn.logupload.client.ElsVerifyClient;
import app.coronawarn.logupload.client.ElsVerifyClientRequest;
import app.coronawarn.logupload.client.ElsVerifyClientResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"els-verify.tls.enabled=false"})
public class OtpServiceTest {

    @Autowired
    OtpService otpService;

    @MockBean
    ElsVerifyClient elsVerifyClientMock;

    private static final String testOtp = "ea8166fa-6a42-426a-8b14-a4c53ff710b5";

    @Test
    public void shouldReturnTrueIfOtpIsValid() {
        ArgumentCaptor<ElsVerifyClientRequest> captor =
            ArgumentCaptor.forClass(ElsVerifyClientRequest.class);

        ElsVerifyClientResponse response =
            new ElsVerifyClientResponse(testOtp, true, "valid");

        when(elsVerifyClientMock.verifyOtp(captor.capture())).thenReturn(response);

        Assertions.assertTrue(otpService.verifyOtp(testOtp));
        Assertions.assertEquals(testOtp, captor.getValue().getOtp());
    }

    @Test
    public void shouldReturnFalseIfOtpIsAlreadyUsed() {
        ArgumentCaptor<ElsVerifyClientRequest> captor =
            ArgumentCaptor.forClass(ElsVerifyClientRequest.class);

        doThrow(new FeignException.BadRequest("", getDummyFeignRequest(), null))
            .when(elsVerifyClientMock).verifyOtp(captor.capture());

        Assertions.assertFalse(otpService.verifyOtp(testOtp));
        Assertions.assertEquals(testOtp, captor.getValue().getOtp());
    }

    @Test
    public void shouldReturnFalseIfOtpRequestFailed() {
        ArgumentCaptor<ElsVerifyClientRequest> captor =
            ArgumentCaptor.forClass(ElsVerifyClientRequest.class);

        doThrow(new FeignException.InternalServerError("", getDummyFeignRequest(), null))
            .when(elsVerifyClientMock).verifyOtp(captor.capture());

        Assertions.assertFalse(otpService.verifyOtp(testOtp));
        Assertions.assertEquals(testOtp, captor.getValue().getOtp());
    }

    private Request getDummyFeignRequest() {
        return Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
    }
}
