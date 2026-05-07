package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroRole;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroContentDataServiceTest {

    @Mock
    private HeroRepository heroRepository;

    @Test
    void normalizeOfficialImportForStorageRebuildsCellsByPrimaryRoleOrder() {
        when(heroRepository.findAllWithRolesAndAttributes()).thenReturn(List.of(
                hero(1L, "Hayate", "ADL"),
                hero(2L, "Marja", "MID"),
                hero(3L, "Toro", "SUP"),
                hero(4L, "Billow", "JGL")
        ));

        HeroContentDataService service = new HeroContentDataService(heroRepository);
        Map<String, Object> contentData = new LinkedHashMap<>();
        contentData.put("columns", List.of(
                Map.of("label", "MID"),
                Map.of("label", "SUP"),
                Map.of("label", "ADL"),
                Map.of("label", "DSL"),
                Map.of("label", "JGL")
        ));
        contentData.put("rows", List.of(
                row("S", "#e74c3c", List.of(
                        List.of(Map.of("name", "Hayate")),
                        List.of(Map.of("name", "Toro")),
                        List.of(),
                        List.of(Map.of("name", "Marja")),
                        List.of()
                )),
                row("A", "#9b59b6", List.of(
                        List.of(Map.of("name", "Billow")),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ))
        ));

        Map<?, ?> normalized = (Map<?, ?>) service.normalizeOfficialImportForStorage(contentData);
        List<?> columns = (List<?>) normalized.get("columns");
        List<?> rows = (List<?>) normalized.get("rows");
        List<String> labels = columns.stream()
                .map(column -> String.valueOf(((Map<?, ?>) column).get("label")))
                .toList();

        assertThat(labels).containsExactly("DSL", "JGL", "MID", "ADL", "SUP");

        Map<?, ?> sRow = (Map<?, ?>) rows.get(0);
        Map<?, ?> aRow = (Map<?, ?>) rows.get(1);

        assertThat(sRow.get("label")).isEqualTo("S");
        assertThat(sRow.get("color")).isEqualTo("#e74c3c");
        assertThat(heroIds((List<?>) ((List<?>) sRow.get("cells")).get(0))).isEmpty();
        assertThat(heroIds((List<?>) ((List<?>) sRow.get("cells")).get(1))).isEmpty();
        assertThat(heroIds((List<?>) ((List<?>) sRow.get("cells")).get(2))).containsExactly(2L);
        assertThat(heroIds((List<?>) ((List<?>) sRow.get("cells")).get(3))).containsExactly(1L);
        assertThat(heroIds((List<?>) ((List<?>) sRow.get("cells")).get(4))).containsExactly(3L);

        assertThat(aRow.get("label")).isEqualTo("A");
        assertThat(aRow.get("color")).isEqualTo("#9b59b6");
        assertThat(heroIds((List<?>) ((List<?>) aRow.get("cells")).get(0))).isEmpty();
        assertThat(heroIds((List<?>) ((List<?>) aRow.get("cells")).get(1))).containsExactly(4L);
        assertThat(heroIds((List<?>) ((List<?>) aRow.get("cells")).get(2))).isEmpty();
        assertThat(heroIds((List<?>) ((List<?>) aRow.get("cells")).get(3))).isEmpty();
        assertThat(heroIds((List<?>) ((List<?>) aRow.get("cells")).get(4))).isEmpty();
    }

    private Map<String, Object> row(String label, String color, List<List<Map<String, Object>>> cells) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("color", color);
        row.put("cells", cells);
        return row;
    }

    private Hero hero(Long id, String name, String primaryRoleCode) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(name.toLowerCase());
        if (primaryRoleCode != null) {
            HeroRole role = new HeroRole();
            role.setId(id);
            role.setCode(primaryRoleCode);
            role.setName(primaryRoleCode);
            hero.setPrimaryRole(role);
        }
        return hero;
    }

    private List<Long> heroIds(List<?> cells) {
        return cells.stream()
                .map(value -> (Map<?, ?>) value)
                .map(value -> value.get("heroId"))
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }
}
