package com.example.demo.util;

import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

import java.util.List;

public final class EsportsDraftDefaults {

    public static final String DEFAULT_FORMAT_CODE = "AOV_STANDARD_18";
    public static final String DEFAULT_FORMAT_NAME = "AOV Standard 18 Phase";
    public static final int DEFAULT_TOTAL_STEPS = 18;

    private static final List<PhaseTemplate> DEFAULT_PHASES = List.of(
            new PhaseTemplate(1, BanPickTeamSide.BLUE, BanPickActionType.BAN),
            new PhaseTemplate(2, BanPickTeamSide.RED, BanPickActionType.BAN),
            new PhaseTemplate(3, BanPickTeamSide.BLUE, BanPickActionType.BAN),
            new PhaseTemplate(4, BanPickTeamSide.RED, BanPickActionType.BAN),
            new PhaseTemplate(5, BanPickTeamSide.BLUE, BanPickActionType.PICK),
            new PhaseTemplate(6, BanPickTeamSide.RED, BanPickActionType.PICK),
            new PhaseTemplate(7, BanPickTeamSide.RED, BanPickActionType.PICK),
            new PhaseTemplate(8, BanPickTeamSide.BLUE, BanPickActionType.PICK),
            new PhaseTemplate(9, BanPickTeamSide.BLUE, BanPickActionType.PICK),
            new PhaseTemplate(10, BanPickTeamSide.RED, BanPickActionType.PICK),
            new PhaseTemplate(11, BanPickTeamSide.RED, BanPickActionType.BAN),
            new PhaseTemplate(12, BanPickTeamSide.BLUE, BanPickActionType.BAN),
            new PhaseTemplate(13, BanPickTeamSide.RED, BanPickActionType.BAN),
            new PhaseTemplate(14, BanPickTeamSide.BLUE, BanPickActionType.BAN),
            new PhaseTemplate(15, BanPickTeamSide.RED, BanPickActionType.PICK),
            new PhaseTemplate(16, BanPickTeamSide.BLUE, BanPickActionType.PICK),
            new PhaseTemplate(17, BanPickTeamSide.BLUE, BanPickActionType.PICK),
            new PhaseTemplate(18, BanPickTeamSide.RED, BanPickActionType.PICK)
    );

    private EsportsDraftDefaults() {
    }

    public static List<PhaseTemplate> defaultPhaseTemplates() {
        return DEFAULT_PHASES;
    }

    public record PhaseTemplate(
            int stepNumber,
            BanPickTeamSide teamSide,
            BanPickActionType actionType
    ) {
    }
}
