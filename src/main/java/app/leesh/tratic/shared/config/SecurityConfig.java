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
                                                .requestMatchers("/", "/login**", "/error**").permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .oidcUserService(customOidcUserService)))
                                .logout(Customizer.withDefaults());

                return http.build();
        }
}
