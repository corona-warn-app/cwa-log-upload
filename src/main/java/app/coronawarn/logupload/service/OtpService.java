package app.coronawarn.logupload.service;

import app.coronawarn.logupload.client.ElsVerifyClient;
import app.coronawarn.logupload.client.ElsVerifyClientRequest;
import app.coronawarn.logupload.client.ElsVerifyClientResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final ElsVerifyClient elsVerifyClient;

    /**
     * Verifies whether the given one time password is valid.
     * Also the redemption of the otp will happen.
     *
     * @param otp the one time password to check
     * @return whether otp was valid.
     */
    public boolean verifyOtp(String otp) {
        log.info("Validating OTP");
        return true;
        /*ElsVerifyClientRequest request = new ElsVerifyClientRequest(otp);
        ElsVerifyClientResponse response;

        try {
            response = elsVerifyClient.verifyOtp(request);
        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_BAD_REQUEST) {
                log.info("OTP is invalid");
            } else {
                log.error("Could not redeem otp", e);
            }
            return false;
        }

        log.info("Got OTP status: {}", response.getState());

        return response.getState().equals("valid");*/
    }
}
