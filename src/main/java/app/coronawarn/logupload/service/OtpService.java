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

        ElsVerifyClientRequest request = new ElsVerifyClientRequest(otp);
        ElsVerifyClientResponse response;

        try {
            response = elsVerifyClient.verifyOtp(request);
        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_BAD_REQUEST) {
                log.info("Bad request validating OTP");
            } else {
                log.error("Could not redeem otp", e);
            }
            return false;
        }

        log.info("Got OTP status: {}", response.getState());

        return response.getState().equals("valid");
    }
}
