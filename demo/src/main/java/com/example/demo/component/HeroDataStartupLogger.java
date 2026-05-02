package com.example.demo.component;

import com.example.demo.repository.HeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class HeroDataStartupLogger implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HeroDataStartupLogger.class);

    private final HeroRepository heroRepository;

    public HeroDataStartupLogger(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Override
    public void run(String... args) {
        long heroCount = heroRepository.count();
        log.info("Hero database count: {}", heroCount);
        if (heroCount == 0) {
            log.warn("Hero database is empty. Please run sql/seed_heroes.sql to seed hero data.");
        }
    }
}
