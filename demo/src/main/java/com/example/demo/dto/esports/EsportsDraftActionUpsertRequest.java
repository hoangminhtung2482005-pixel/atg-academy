package com.example.demo.dto.esports;

import java.util.List;

public record EsportsDraftActionUpsertRequest(
        List<EsportsDraftActionRequest> actions
) {
}
