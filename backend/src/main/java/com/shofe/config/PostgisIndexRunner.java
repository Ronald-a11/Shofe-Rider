package com.shofe.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Enables PostGIS and keeps a geography point next to the lat/lng columns.
 * Nearby matching uses a haversine filter so the API still runs if the extension
 * is unavailable. The GIST index is there for a later ST_DWithin query.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PostgisIndexRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Override
    public void run(ApplicationArguments args) {
        if (datasourceUrl == null || !datasourceUrl.contains("postgresql")) {
            log.info("PostGIS setup skipped (not on PostgreSQL)");
            return;
        }
        try {
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS postgis");
            jdbc.execute("ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS geog geography(Point,4326)");
            sync();
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_driver_geog ON driver_profiles USING GIST (geog)");
            log.info("PostGIS geography column is ready on driver_profiles");
        } catch (RuntimeException ex) {
            log.warn("PostGIS setup skipped: {}", ex.getMessage());
        }
    }

    public void sync() {
        jdbc.execute("""
                UPDATE driver_profiles
                SET geog = ST_SetSRID(ST_MakePoint(last_lng, last_lat), 4326)::geography
                WHERE last_lat IS NOT NULL AND last_lng IS NOT NULL
                """);
    }
}
