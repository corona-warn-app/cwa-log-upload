package app.coronawarn.logupload.client;

import app.coronawarn.logupload.config.ElsVerifyConfig;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Profile("api")

public class ElsVerifyClientConfig {

    private final ElsVerifyConfig config;

    /**
     * Build Http Client for accessing els-verify service.
     */
    @Bean
    public Client client() {
        if (config.getTls().getEnabled()) { // Build HTTPS client
            return new ApacheHttpClient(
                HttpClientBuilder
                    .create()
                    .setSSLContext(getSslContext())
                    .setSSLHostnameVerifier(getSslHostnameVerifier())
                    .build()
            );
        } else { // Build HTTP client
            return new ApacheHttpClient(HttpClientBuilder.create().build());
        }
    }

    private SSLContext getSslContext() {
        try {
            return SSLContextBuilder.create()
            .loadTrustMaterial(ResourceUtils.getFile(config.getTls().getTrustStore()),
                config.getTls().getTrustStorePassword().toCharArray())
            .loadKeyMaterial(ResourceUtils.getFile(config.getTls().getKeyStore()),
                config.getTls().getKeyStorePassword().toCharArray(),
                config.getTls().getKeyStorePassword().toCharArray())
            .build();

        } catch (IOException | GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The SSL context could not be loaded.");
        }
    }

    private HostnameVerifier getSslHostnameVerifier() {
        return config.getTls().getHostnameVerify() ? new DefaultHostnameVerifier() : new NoopHostnameVerifier();
    }
}
