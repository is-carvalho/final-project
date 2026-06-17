package org.raimundo.ecommerce.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("GET", "/api/v1/orders/**").hasAuthority("SCOPE_orders:read")
                        .requestMatchers("/api/v1/orders/**").hasAuthority("SCOPE_orders:write")
                        .requestMatchers("GET", "/api/v1/payments/**").hasAuthority("SCOPE_payments:read")
                        .requestMatchers("/api/v1/payments/**").hasAuthority("SCOPE_payments:write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
