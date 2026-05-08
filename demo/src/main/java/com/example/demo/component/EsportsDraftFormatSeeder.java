package com.example.demo.component;

import com.example.demo.entity.EsportsDraftFormat;
import com.example.demo.entity.EsportsDraftPhaseRule;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.repository.EsportsDraftFormatRepository;
import com.example.demo.repository.EsportsDraftPhaseRuleRepository;
import com.example.demo.repository.EsportsMatchGameRepository;
import com.example.demo.util.EsportsDraftDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class EsportsDraftFormatSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EsportsDraftFormatSeeder.class);

    private final EsportsDraftFormatRepository esportsDraftFormatRepository;
    private final EsportsDraftPhaseRuleRepository esportsDraftPhaseRuleRepository;
    private final EsportsMatchGameRepository esportsMatchGameRepository;

    public EsportsDraftFormatSeeder(EsportsDraftFormatRepository esportsDraftFormatRepository,
                                    EsportsDraftPhaseRuleRepository esportsDraftPhaseRuleRepository,
                                    EsportsMatchGameRepository esportsMatchGameRepository) {
        this.esportsDraftFormatRepository = esportsDraftFormatRepository;
        this.esportsDraftPhaseRuleRepository = esportsDraftPhaseRuleRepository;
        this.esportsMatchGameRepository = esportsMatchGameRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        EsportsDraftFormat defaultFormat = upsertDefaultFormat();
        syncDefaultRules(defaultFormat);
        backfillGames(defaultFormat);
    }

    private EsportsDraftFormat upsertDefaultFormat() {
        EsportsDraftFormat format = esportsDraftFormatRepository.findByCode(EsportsDraftDefaults.DEFAULT_FORMAT_CODE)
                .orElseGet(EsportsDraftFormat::new);

        format.setCode(EsportsDraftDefaults.DEFAULT_FORMAT_CODE);
        format.setName(EsportsDraftDefaults.DEFAULT_FORMAT_NAME);
        format.setTotalSteps(EsportsDraftDefaults.DEFAULT_TOTAL_STEPS);
        format.setDefaultFormat(true);
        format.setActive(true);

        return esportsDraftFormatRepository.save(format);
    }

    private void syncDefaultRules(EsportsDraftFormat format) {
        List<EsportsDraftPhaseRule> existingRules = esportsDraftPhaseRuleRepository
                .findByFormatIdOrderByStepNumberAsc(format.getId());

        if (rulesMatch(existingRules)) {
            return;
        }

        if (!existingRules.isEmpty()) {
            esportsDraftPhaseRuleRepository.deleteAllInBatch(existingRules);
            esportsDraftPhaseRuleRepository.flush();
        }

        List<EsportsDraftPhaseRule> expectedRules = EsportsDraftDefaults.defaultPhaseTemplates().stream()
                .map(template -> {
                    EsportsDraftPhaseRule rule = new EsportsDraftPhaseRule();
                    rule.setFormat(format);
                    rule.setStepNumber(template.stepNumber());
                    rule.setTeamSide(template.teamSide());
                    rule.setActionType(template.actionType());
                    return rule;
                })
                .toList();

        esportsDraftPhaseRuleRepository.saveAll(expectedRules);
        log.info(">> [Esports Draft Seeder] Da dong bo {} phase rule mac dinh.", expectedRules.size());
    }

    private boolean rulesMatch(List<EsportsDraftPhaseRule> existingRules) {
        List<EsportsDraftDefaults.PhaseTemplate> expectedRules = EsportsDraftDefaults.defaultPhaseTemplates();
        if (existingRules.size() != expectedRules.size()) {
            return false;
        }

        for (int index = 0; index < expectedRules.size(); index++) {
            EsportsDraftPhaseRule existingRule = existingRules.get(index);
            EsportsDraftDefaults.PhaseTemplate expectedRule = expectedRules.get(index);
            if (!existingRule.getStepNumber().equals(expectedRule.stepNumber())
                    || existingRule.getTeamSide() != expectedRule.teamSide()
                    || existingRule.getActionType() != expectedRule.actionType()) {
                return false;
            }
        }
        return true;
    }

    private void backfillGames(EsportsDraftFormat defaultFormat) {
        List<EsportsMatchGame> gamesWithoutFormat = esportsMatchGameRepository.findAllByDraftFormatIsNull();
        if (gamesWithoutFormat.isEmpty()) {
            return;
        }

        gamesWithoutFormat.forEach(game -> game.setDraftFormat(defaultFormat));
        esportsMatchGameRepository.saveAll(gamesWithoutFormat);
        log.info(">> [Esports Draft Seeder] Da backfill draft_format_id cho {} game cu.", gamesWithoutFormat.size());
    }
}
