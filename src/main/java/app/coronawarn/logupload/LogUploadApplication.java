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

package app.coronawarn.logupload;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Spring Boot application class.
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableFeignClients
@EnableScheduling
@OpenAPIDefinition(info = @Info(description = "API to upload and store log files in context of Corona Warn App.",
  version = "v1.0", title = "CWA Log Upload"))
public class LogUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogUploadApplication.class, args);
    }

    /*
     * Enable the cipher suites from server to be preferred.
     *
     * @return the WebServerFactoryCustomizer with cipher suites configuration

     @Bean
    @ConditionalOnProperty(value = "server.ssl.cipher.suites.order", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory
                .addConnectorCustomizers(connector ->
                        ((AbstractHttp11Protocol<?>) connector.getProtocolHandler())
                                .setUseServerCipherSuitesOrder(true)
                );
    }*/

}
