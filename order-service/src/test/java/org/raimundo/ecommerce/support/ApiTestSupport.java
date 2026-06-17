package org.raimundo.ecommerce.support;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class ApiTestSupport {
    private ApiTestSupport() {
    }

    public static JwtRequestPostProcessor jwtWithScopes(String... scopes) {
        return jwt().jwt(token -> token.claim("scope", String.join(" ", scopes)));
    }
}
