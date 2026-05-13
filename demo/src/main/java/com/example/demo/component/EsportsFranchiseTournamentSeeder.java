package com.example.demo.component;

import com.example.demo.service.EsportsTournamentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EsportsFranchiseTournamentSeeder implements CommandLineRunner {

    private final EsportsTournamentService esportsTournamentService;

    public EsportsFranchiseTournamentSeeder(EsportsTournamentService esportsTournamentService) {
        this.esportsTournamentService = esportsTournamentService;
    }

    @Override
    public void run(String... args) {
        esportsTournamentService.seedDefaultsIfMissing();
    }
}
