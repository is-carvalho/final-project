package org.raimundo.ecommerce.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/swagger-ui.html".equals(path) || "/openapi.yaml".equals(path) || "/v3/api-docs".equals(path)
                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader("Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Correlation-Id header is required");
            return;
        }
        CorrelationContext.set(correlationId);
        MDC.put("correlationId", correlationId);
        response.setHeader("Correlation-Id", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            CorrelationContext.clear();
        }
    }
}
