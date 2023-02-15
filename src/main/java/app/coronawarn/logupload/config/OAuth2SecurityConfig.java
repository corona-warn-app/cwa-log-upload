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

package app.coronawarn.logupload.config;

import app.coronawarn.logupload.LogUploadHttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!test")
class OAuth2SecurityConfig {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_C19LOG_INSPECTOR = "c19log_inspector";
    private static final String ACTUATOR_ROUTE = "/actuator/**";
    private static final String PUBLIC_API_ROUTE = "/api/**";
    private static final String PORTAL_ROUTE = "/portal/**";
    private static final String INDEX_ROUTE = "/";
    private static final String ERROR_ROUTE = "/error/**";
    private static final String SWAGGER_ROUTE = "/v3/api-docs/**";


    private static final String SAMESITE_LAX = "Lax";
    private static final String OAUTH_TOKEN_REQUEST_STATE_COOKIE = "OAuth_Token_Request_State";
    private static final String SESSION_COOKIE = "SESSION";

    private final LogUploadHttpFilter logUploadHttpFilter;

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .addFilterBefore(logUploadHttpFilter, BasicAuthenticationFilter.class)
          .headers().addHeaderWriter(this::addSameSiteToOAuthCookie)
          .and()
          .authorizeHttpRequests()
          .requestMatchers(HttpMethod.GET, ACTUATOR_ROUTE).permitAll()
          .requestMatchers(HttpMethod.GET, ERROR_ROUTE).permitAll()
          .requestMatchers(HttpMethod.GET, INDEX_ROUTE).permitAll()
          .requestMatchers(HttpMethod.POST, PUBLIC_API_ROUTE).permitAll()
          .requestMatchers(HttpMethod.GET, SWAGGER_ROUTE).permitAll()
          .requestMatchers(HttpMethod.GET, PORTAL_ROUTE).hasRole(ROLE_C19LOG_INSPECTOR)
          .requestMatchers(HttpMethod.POST, PORTAL_ROUTE).hasRole(ROLE_C19LOG_INSPECTOR)
          .anyRequest()
          .denyAll()
          .and()
          .csrf().ignoringRequestMatchers(PUBLIC_API_ROUTE, SWAGGER_ROUTE);

        http.oauth2Login()
          .and()
          .logout()
          .addLogoutHandler(keycloakLogoutHandler)
          .logoutSuccessUrl("/");

        return http.build();
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
    public MapSessionRepository sessionRepository() {
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

    @Bean
    @SuppressWarnings("unchecked")
    public GrantedAuthoritiesMapper userAuthoritiesMapperForKeycloak() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            var authority = authorities.iterator().next();
            boolean isOidc = authority instanceof OidcUserAuthority;

            if (isOidc) {
                var oidcUserAuthority = (OidcUserAuthority) authority;
                var userInfo = oidcUserAuthority.getUserInfo();

                if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
                    var realmAccess = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
                    var roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                }
            } else {
                var oauth2UserAuthority = (OAuth2UserAuthority) authority;
                Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                if (userAttributes.containsKey(REALM_ACCESS_CLAIM)) {
                    var realmAccess = (Map<String, Object>) userAttributes.get(REALM_ACCESS_CLAIM);
                    var roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                }
            }
            return mappedAuthorities;
        };
    }

    Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList());
    }
}
