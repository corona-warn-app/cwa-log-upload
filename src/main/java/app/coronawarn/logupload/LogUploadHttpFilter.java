/*
 * Corona-Warn-App / cwa-verification
 *
 * (C) 2021 - 2022, T-Systems International GmbH
 *
 * Deutsche Telekom AG, SAP AG and all other contributors /
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


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogUploadHttpFilter implements Filter {

    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

    @Value("${host-header.whitelist}")
    private List<String> validHostHeaders;

    @Value("${pod.ip}")
    private String podIp;

    @Value("${pod.port}")
    private String podPort;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (isHostHeaderValid(request)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Invalid Host Header");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getOutputStream().flush();
            response.getOutputStream().println("Bad Request");
        }
    }

    private boolean isHostHeaderValid(final HttpServletRequest request) {
        final String host = request.getHeader(HttpHeaders.HOST);
        final String xForwardedHost = request.getHeader(X_FORWARDED_HOST_HEADER);
        if (xForwardedHost != null || host == null) {
            return false;
        } else {
            return validHostHeaders.contains(host) || host.equals(podIp + ":" + podPort);
        }
    }

}
