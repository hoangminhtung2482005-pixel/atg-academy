package com.example.demo.dto.banpick;

import java.util.List;

/**
 * Request to update the current user's strategy pool for the current room.
 * The pool is used to prioritize hero display order for the player only.
 * It does not lock or ban heroes.
 */
public record BanPickStrategyPoolRequest(
        List<Long> heroIds
) {
}
