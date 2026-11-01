package com.smartretail.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id, on each of its log lines and in the X-Request-Id response header, and logs
 * one line when it finishes. Paths only, never query strings or bodies, which can hold personal details.
 */
@Slf4j
@Component
// Just inside the tracing filter, so these log lines carry the trace id too.
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";

    private static final String HEADER = "X-Request-Id";
    // A caller's id is only reused when it is plainly safe to put in a log line.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sent = request.getHeader(HEADER);
        String id = sent != null && SAFE_ID.matcher(sent).matches() ? sent : UUID.randomUUID().toString();
        long start = System.nanoTime();
        MDC.put(REQUEST_ID, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(),
                    (System.nanoTime() - start) / 1_000_000);
            MDC.remove(REQUEST_ID);
        }
    }

    /** Health checks arrive every few seconds and would drown out everything else. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
