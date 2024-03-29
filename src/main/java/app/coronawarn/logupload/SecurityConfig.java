/*
 * Corona-Warn-App / cwa-log-upload
 *
 * (C) 2021, T-Systems International GmbH
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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


@Slf4j
@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
@RequiredArgsConstructor
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    private static final String ROLE_C19LOG_INSPECTOR = "c19log_inspector";
    private static final String ACTUATOR_ROUTE = "/actuator/**";
    private static final String PUBLIC_API_ROUTE = "/api/**";
    private static final String PORTAL_ROUTE = "/portal/**";
    private static final String SWAGGER_ROUTE = "/v3/api-docs/**";


    private static final String SAMESITE_LAX = "Lax";
    private static final String OAUTH_TOKEN_REQUEST_STATE_COOKIE = "OAuth_Token_Request_State";
    private static final String SESSION_COOKIE = "SESSION";

    private final LogUploadHttpFilter logUploadHttpFilter;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    @Bean
    public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http
            .addFilterBefore(logUploadHttpFilter, KeycloakAuthenticationProcessingFilter.class)
            .headers().addHeaderWriter(this::addSameSiteToOAuthCookie)
            .and()
            .authorizeRequests()
            .mvcMatchers(HttpMethod.GET, ACTUATOR_ROUTE).permitAll()
            .mvcMatchers(HttpMethod.POST, PUBLIC_API_ROUTE).permitAll()
            .mvcMatchers(HttpMethod.GET, SWAGGER_ROUTE).permitAll()
            .mvcMatchers(HttpMethod.GET, PORTAL_ROUTE).hasRole(ROLE_C19LOG_INSPECTOR)
            .mvcMatchers(HttpMethod.POST, PORTAL_ROUTE).hasRole(ROLE_C19LOG_INSPECTOR)
            .anyRequest()
            .denyAll()
            .and()
            .csrf().ignoringAntMatchers(PUBLIC_API_ROUTE, SWAGGER_ROUTE);
    }

    @Bean
    public CookieSerializer defaultCookieSerializer() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(SESSION_COOKIE);
        cookieSerializer.setSameSite(SAMESITE_LAX);
        cookieSerializer.setUseHttpOnlyCookie(true);
        return cookieSerializer;
    }

    @Bean
    public SessionRepository sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }

    private void addSameSiteToOAuthCookie(final HttpServletRequest request, final HttpServletResponse response) {
        final Collection<String> setCookieValues = response.getHeaders(HttpHeaders.SET_COOKIE);
        for (String setCookie : setCookieValues) {
            if (setCookie.contains(OAUTH_TOKEN_REQUEST_STATE_COOKIE)) {
                response.setHeader(HttpHeaders.SET_COOKIE, addSameSiteStrict(setCookie));
            }
        }
    }

    private String addSameSiteStrict(String setCookie) {
        return setCookie + "; SameSite=" + SAMESITE_LAX;
    }

}
