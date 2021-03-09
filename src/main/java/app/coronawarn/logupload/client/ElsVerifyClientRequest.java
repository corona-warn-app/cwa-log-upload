package app.coronawarn.logupload.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElsVerifyClientRequest {

    private String otp;
}
