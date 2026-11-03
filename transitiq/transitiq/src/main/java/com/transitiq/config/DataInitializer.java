package com.transitiq.config;

import com.transitiq.repository.TransitRepository;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Loads sample-data.sql once, only when the database has no bus stops yet. */
@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedDemoData(TransitRepository repo, DataSource ds) {
        return args -> {
            if (repo.countStops() > 0) return;
            try (Connection c = ds.getConnection()) {
                ScriptUtils.executeSqlScript(c, new ClassPathResource("sample-data.sql"));
            }
            log.info("Empty database detected: loaded demo dataset ({} stops, {} routes, {} connections)",
                    repo.countStops(), repo.countRoutes(), repo.countConnections());
        };
    }
}
