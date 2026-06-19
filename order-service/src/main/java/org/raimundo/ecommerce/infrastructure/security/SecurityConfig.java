package org.raimundo.ecommerce.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.net.URI;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAnyAuthority("SCOPE_orders:read", "SCOPE_order:read")
                        .requestMatchers("/api/v1/orders/**").hasAnyAuthority("SCOPE_orders:write", "SCOPE_order:write")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").hasAnyAuthority("SCOPE_payments:read", "SCOPE_payment:read")
                        .requestMatchers("/api/v1/payments/**").hasAnyAuthority("SCOPE_payments:write", "SCOPE_payment:write")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Insufficient scope");
                    problem.setType(URI.create("https://ecommerce.raimundo.org/problems/forbidden"));
                    problem.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
                    problem.setProperty("code", "forbidden");
                    problem.setProperty("correlationId", request.getHeader("Correlation-Id"));
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    mapper.writeValue(response.getWriter(), problem);
                }))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
