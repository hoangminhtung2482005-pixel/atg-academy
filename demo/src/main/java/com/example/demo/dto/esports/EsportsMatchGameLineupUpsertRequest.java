package com.example.demo.dto.esports;

import java.util.List;

public record EsportsMatchGameLineupUpsertRequest(
        List<EsportsMatchGameLineupRequest> lineups
) {
}
