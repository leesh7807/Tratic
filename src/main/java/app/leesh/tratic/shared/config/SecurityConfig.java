package app.leesh.tratic.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import app.leesh.tratic.auth.service.CustomOidcUserService;
import app.leesh.tratic.shared.logging.TraceIdFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                        CustomOidcUserService customOidcUserService,
                        TraceIdFilter traceIdFilter)
                        throws Exception {

                http
                                .addFilterBefore(traceIdFilter, SecurityContextHolderFilter.class)

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/index.html",
                                                                "/static/**",
                                                                "/favicon.ico",
                                                                "/error**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .oidcUserService(customOidcUserService)))
                                .logout(Customizer.withDefaults())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                (request, response, accessDeniedException) -> response
                                                                                .sendError(HttpServletResponse.SC_NOT_FOUND))
                                                .accessDeniedHandler(
                                                                (request, response, accessDeniedException) -> response
                                                                                .sendError(HttpServletResponse.SC_NOT_FOUND)));

                return http.build();
        }
}
