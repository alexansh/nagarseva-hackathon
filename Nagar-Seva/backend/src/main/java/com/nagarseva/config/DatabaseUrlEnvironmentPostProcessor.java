package com.nagarseva.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically detects and normalizes cloud database connection strings (Render, Railway, Heroku, Supabase, Neon)
 * converting 'postgres://user:password@host:port/dbname' or 'postgresql://...' into standard JDBC format:
 * 'jdbc:postgresql://host:port/dbname?sslmode=require' with extracted credentials.
 * Also loads local .env files into the Spring environment for seamless local development.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Load local .env files if present (without committing secrets to git)
        loadLocalDotEnv(environment);

        String rawUrl = environment.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        rawUrl = rawUrl.trim();
        if (rawUrl.startsWith("jdbc:h2:")) {
            return;
        }

        try {
            Map<String, Object> overrides = new HashMap<>();

            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                String normalizedUriStr = rawUrl.startsWith("postgres://")
                        ? "postgresql://" + rawUrl.substring("postgres://".length())
                        : rawUrl;

                URI uri = new URI(normalizedUriStr);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String userInfo = uri.getUserInfo();
                String query = uri.getQuery();

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                } else if (host != null && (host.contains("render.com") || host.contains("neon.tech") || host.contains("supabase.co") || host.contains("railway.app"))) {
                    jdbcUrl.append("?sslmode=require");
                }

                String finalJdbcUrl = jdbcUrl.toString();
                overrides.put("spring.datasource.url", finalJdbcUrl);
                overrides.put("DATABASE_URL", finalJdbcUrl);
                overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    overrides.put("spring.datasource.username", parts[0]);
                    overrides.put("spring.datasource.password", parts[1]);
                    log.info("Parsed PostgreSQL credentials from DATABASE_URL for user '{}' on host '{}'", parts[0], host);
                }
                log.info("Normalized cloud DATABASE_URL to JDBC format: {}", finalJdbcUrl.replaceAll(":[^/@]+@", ":***@"));
            } else if (rawUrl.startsWith("jdbc:postgres://")) {
                String fixed = "jdbc:postgresql://" + rawUrl.substring("jdbc:postgres://".length());
                overrides.put("spring.datasource.url", fixed);
                overrides.put("DATABASE_URL", fixed);
                overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            } else if (!rawUrl.startsWith("jdbc:")) {
                String fixed = "jdbc:" + rawUrl;
                overrides.put("spring.datasource.url", fixed);
                overrides.put("DATABASE_URL", fixed);
            }

            if (!overrides.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlNormalization", overrides));
            }
        } catch (Exception e) {
            log.warn("Could not normalize cloud DATABASE_URL '{}': {}", rawUrl, e.getMessage());
        }
    }

    private void loadLocalDotEnv(ConfigurableEnvironment environment) {
        Path[] candidatePaths = new Path[]{
                Path.of(".env"),
                Path.of("../.env"),
                Path.of("../../.env")
        };

        for (Path path : candidatePaths) {
            if (Files.exists(path) && !Files.isDirectory(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    Map<String, Object> envMap = new HashMap<>();
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                            int eq = line.indexOf('=');
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            }
                            envMap.put(key, val);
                        }
                    }
                    if (!envMap.isEmpty()) {
                        environment.getPropertySources().addLast(new MapPropertySource("localDotEnvFile:" + path, envMap));
                        log.info("Loaded {} environment variables from local {}", envMap.size(), path);
                        return;
                    }
                } catch (Exception e) {
                    log.debug("Could not parse env file at {}: {}", path, e.getMessage());
                }
            }
        }
    }
}
