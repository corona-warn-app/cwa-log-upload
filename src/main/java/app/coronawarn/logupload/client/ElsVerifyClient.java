package app.coronawarn.logupload.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "els-verify", url = "${els-verify.url}", configuration = ElsVerifyClientConfig.class)
public interface ElsVerifyClient {

    @PostMapping(value = "/version/v1/els",
        produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ElsVerifyClientResponse verifyOtp(ElsVerifyClientRequest body);
}
