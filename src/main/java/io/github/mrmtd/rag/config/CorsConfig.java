package io.github.mrmtd.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Lets browser apps on other origins (e.g. the React frontend) call the {@code /api/**} endpoints.
 *
 * <p>Browsers block a page on one origin (scheme + host + port, e.g. {@code http://localhost:5173})
 * from reading responses from another origin unless the server opts in with CORS headers. The
 * allowed origins come from {@code app.cors.allowed-origins} (env var {@code CORS_ALLOWED_ORIGINS}),
 * a comma-separated list. Only exact origins are allowed; there is no {@code *} wildcard.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
