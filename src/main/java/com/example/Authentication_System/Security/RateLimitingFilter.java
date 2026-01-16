package com.example.Authentication_System.Security;

import com.example.Authentication_System.Services.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();

        Bucket bucket = null;

        if (requestUri.startsWith("/api/auth/login")) {
            bucket = rateLimitingService.resolveLoginBucket(ipAddress);
        } else if (requestUri.startsWith("/api/auth/register")) {
            bucket = rateLimitingService.resolveRegistrationBucket(ipAddress);
        }

        if (bucket != null) {
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests");
                response.getWriter().flush();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
