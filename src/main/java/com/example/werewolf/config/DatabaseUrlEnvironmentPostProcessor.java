package com.example.werewolf.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets the app consume a standard PostgreSQL connection string (as provided by
 * Neon, Render, Heroku, ...) through the DATABASE_URL environment variable.
 *
 * <p>Spring needs a JDBC URL ({@code jdbc:postgresql://...}), while managed
 * providers expose a plain {@code postgresql://user:pass@host:port/db?sslmode=require}.
 * This post-processor converts the latter and injects it with the highest
 * precedence, so no manual transformation is required at deploy time.</p>
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "databaseUrlProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("SPRING_DATASOURCE_URL"));
        if (databaseUrl == null || !databaseUrl.startsWith("postgres")) {
            return;
        }
        Map<String, Object> properties = datasourceProperties(databaseUrl);
        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    static Map<String, Object> datasourceProperties(String databaseUrl) {
        Map<String, Object> properties = new HashMap<>();
        String url = databaseUrl == null ? "" : databaseUrl.trim();

        if (url.startsWith("jdbc:")) {
            properties.put("spring.datasource.url", url);
            return properties;
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return properties;
        }
        if (uri.getHost() == null) {
            return properties;
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath() == null ? "" : uri.getPath();
        String query = uri.getQuery();
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port).append(path);
        if (query != null && !query.isBlank()) {
            jdbcUrl.append('?').append(query);
        }
        properties.put("spring.datasource.url", jdbcUrl.toString());

        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":", 2);
            properties.put("spring.datasource.username", decode(userInfo[0]));
            if (userInfo.length > 1) {
                properties.put("spring.datasource.password", decode(userInfo[1]));
            }
        }
        return properties;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
