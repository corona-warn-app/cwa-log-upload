package app.coronawarn.logupload.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElsVerifyClientResponse {

    private String otp;
    private boolean strongClientIntegrityCheck;
    private String state;

}
