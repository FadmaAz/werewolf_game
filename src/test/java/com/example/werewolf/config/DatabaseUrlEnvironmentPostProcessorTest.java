package com.example.werewolf.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsNeonConnectionString() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.datasourceProperties(
                "postgresql://neondb_owner:secret123@ep-cool-name-123456.eu-central-1.aws.neon.tech/neondb?sslmode=require");

        assertEquals(
                "jdbc:postgresql://ep-cool-name-123456.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require",
                props.get("spring.datasource.url"));
        assertEquals("neondb_owner", props.get("spring.datasource.username"));
        assertEquals("secret123", props.get("spring.datasource.password"));
    }

    @Test
    void supportsPostgresSchemeAndExplicitPort() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.datasourceProperties(
                "postgres://user:pass@db.example.com:6543/appdb");

        assertEquals("jdbc:postgresql://db.example.com:6543/appdb", props.get("spring.datasource.url"));
        assertEquals("user", props.get("spring.datasource.username"));
        assertEquals("pass", props.get("spring.datasource.password"));
    }

    @Test
    void acceptsJdbcUrlAsIs() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.datasourceProperties(
                "jdbc:postgresql://localhost:5432/werewolf");

        assertEquals("jdbc:postgresql://localhost:5432/werewolf", props.get("spring.datasource.url"));
        assertTrue(props.containsKey("spring.datasource.url"));
    }

    @Test
    void ignoresInvalidOrBlankValues() {
        assertTrue(DatabaseUrlEnvironmentPostProcessor.datasourceProperties("not a valid url").isEmpty());
        assertTrue(DatabaseUrlEnvironmentPostProcessor.datasourceProperties("").isEmpty());
    }
}
