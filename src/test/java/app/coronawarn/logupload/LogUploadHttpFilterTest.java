package app.coronawarn.logupload;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = LogUploadHttpFilter.class)
@TestPropertySource(properties = {"host-header.whitelist=localhost,localhost:8081", "pod.ip=127.0.0.1", "pod.port=8081"})
@EnableConfigurationProperties
public class LogUploadHttpFilterTest {

    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String INVALID_HOST = "invalid-server.local";
    private static final String INVALID_PORT = "9988";

    private static final String VALID_HOST = "localhost";
    private static final String VALID_HOST_PORT = "localhost:8081";

    private static final String POD_HOST = "127.0.0.1";
    private static final String POD_PORT = "8081";
    private static final String INVALID_POD_PORT = "8085";

    @Autowired
    private LogUploadHttpFilter logUploadHttpFilter;

    @Test
    public void doFilterReturnsOkForValidHost() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, VALID_HOST);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void doFilterReturnsOkForValidHostAndPort() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, VALID_HOST_PORT);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void doFilterReturnsOkForValidPodIPAndHost() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, POD_HOST + ":" + POD_PORT);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void doFilterReturnsBadRequestForValidPodPort() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, POD_HOST + ":" + INVALID_POD_PORT);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void doFilterReturnsBadRequestWhenXForwardedHostHeaderInRequest() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, VALID_HOST);
        request.addHeader(X_FORWARDED_HOST_HEADER, INVALID_HOST);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void doFilterReturnsBadRequestWhenHostHeaderNotInRequest() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void doFilterReturnsBadRequestWhenHostHeaderIsNotValid() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, INVALID_HOST);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void doFilterReturnsBadRequestWhenPortInHostHeaderNotValid() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        request.addHeader(HttpHeaders.HOST, VALID_HOST + ":" + INVALID_PORT);
        logUploadHttpFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

}
