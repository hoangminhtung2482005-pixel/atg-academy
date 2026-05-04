package com.example.demo.service;

import com.example.demo.dto.admin.AdminHeroAttributesUpdateRequest;
import com.example.demo.dto.admin.AdminHeroBasicUpdateRequest;
import com.example.demo.dto.admin.AdminHeroRolesUpdateRequest;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.entity.HeroClass;
import com.example.demo.entity.HeroRole;
import com.example.demo.repository.HeroAttributeRepository;
import com.example.demo.repository.HeroClassRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.HeroRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWikiHeroServiceTest {

    @Mock
    private HeroRepository heroRepository;

    @Mock
    private HeroRoleRepository heroRoleRepository;

    @Mock
    private HeroAttributeRepository heroAttributeRepository;

    @Mock
    private HeroClassRepository heroClassRepository;

    private AdminWikiHeroService service;

    @BeforeEach
    void setUp() {
        service = new AdminWikiHeroService(heroRepository, heroRoleRepository, heroAttributeRepository, heroClassRepository);
        lenient().when(heroClassRepository.findByNameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.of(heroClass(1L, invocation.getArgument(0))));
    }

    @Test
    void updateHeroRolesRejectsInvalidRoleCode() {
        Hero hero = hero(10L, "Florentino", "florentino");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));

        assertThatThrownBy(() -> service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(null, null, List.of("TOP"))))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateHeroRolesSupportsLegacyCodesAsPrimaryAndSubRoles() {
        Hero hero = hero(10L, "Florentino", "florentino");
        hero.getRoles().add(role(1L, "DSL", "Solo"));
        HeroRole jungle = role(2L, "JGL", "Jungle");
        HeroRole mid = role(3L, "MID", "Middle");

        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRoleRepository.findByCodeIn(Set.of("JGL", "MID"))).thenReturn(List.of(jungle, mid));
        when(heroRoleRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(jungle, mid));
        when(heroAttributeRepository.findAll()).thenReturn(List.of());

        service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(null, null, List.of("JGL", "MID", "JGL")));

        assertThat(hero.getPrimaryRole()).isEqualTo(jungle);
        assertThat(hero.getRoles())
                .extracting(HeroRole::getCode)
                .containsExactlyInAnyOrder("MID");
    }

    @Test
    void updateHeroRolesRequiresPrimaryRole() {
        Hero hero = hero(10L, "Florentino", "florentino");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));

        assertThatThrownBy(() -> service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(null, List.of(), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Primary role is required");
                });
    }

    @Test
    void updateHeroRolesRejectsDuplicateSubRoles() {
        Hero hero = hero(10L, "Billow", "billow");
        HeroRole jungle = role(2L, "JGL", "Jungle");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRoleRepository.findById(2L)).thenReturn(Optional.of(jungle));

        assertThatThrownBy(() -> service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(2L, List.of(1L, 1L), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Duplicate sub roles are not allowed");
                });
    }

    @Test
    void updateHeroRolesRejectsPrimaryRoleInsideSubRoles() {
        Hero hero = hero(10L, "Billow", "billow");
        HeroRole jungle = role(2L, "JGL", "Jungle");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRoleRepository.findById(2L)).thenReturn(Optional.of(jungle));

        assertThatThrownBy(() -> service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(2L, List.of(2L), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Primary role cannot be selected as sub role");
                });
    }

    @Test
    void updateHeroRolesRejectsMissingRoleId() {
        Hero hero = hero(10L, "Billow", "billow");
        HeroRole jungle = role(2L, "JGL", "Jungle");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRoleRepository.findById(2L)).thenReturn(Optional.of(jungle));
        when(heroRoleRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(2L, List.of(999L), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Role not found: 999");
                });
    }

    @Test
    void updateHeroRolesStoresPrimaryAndSubRolesById() {
        Hero hero = hero(10L, "Billow", "billow");
        hero.getRoles().add(role(3L, "MID", "Middle"));
        HeroRole solo = role(1L, "DSL", "Solo");
        HeroRole jungle = role(2L, "JGL", "Jungle");

        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRoleRepository.findById(2L)).thenReturn(Optional.of(jungle));
        when(heroRoleRepository.findAllById(List.of(1L))).thenReturn(List.of(solo));
        when(heroRoleRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(solo, jungle));
        when(heroAttributeRepository.findAll()).thenReturn(List.of());

        service.updateHeroRoles(10L, new AdminHeroRolesUpdateRequest(2L, List.of(1L), null));

        assertThat(hero.getPrimaryRole()).isEqualTo(jungle);
        assertThat(hero.getRoles())
                .extracting(HeroRole::getCode)
                .containsExactly("DSL");
    }

    @Test
    void updateHeroAttributesRejectsMissingAttribute() {
        Hero hero = hero(10L, "Florentino", "florentino");
        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroAttributeRepository.findByNameIn(Set.of("Unknown"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateHeroAttributes(10L, new AdminHeroAttributesUpdateRequest(List.of("Unknown"))))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateHeroBasicInfoRejectsDuplicateSlug() {
        Hero hero = hero(10L, "Florentino", "florentino");
        hero.setHeroClass("Đấu sĩ");
        Hero existing = hero(20L, "Other Florentino", "florentino-2");

        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroRepository.findBySlug("florentino-2")).thenReturn(Optional.of(existing));

        AdminHeroBasicUpdateRequest request = new AdminHeroBasicUpdateRequest(
                null,
                "Florentino 2",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.updateHeroBasicInfo(10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateHeroBasicInfoSupportsMultipleClasses() {
        Hero hero = hero(10L, "Marja", "marja");
        hero.setHeroClass("Pháp sư");
        HeroClass mage = heroClass(1L, "Pháp sư");
        HeroClass warrior = heroClass(2L, "Đấu sĩ");

        when(heroRepository.findByIdWithRolesAndAttributes(10L)).thenReturn(Optional.of(hero));
        when(heroClassRepository.findByNameIn(Set.of("Pháp sư", "Đấu sĩ"))).thenReturn(List.of(mage, warrior));
        when(heroRoleRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(heroAttributeRepository.findAll()).thenReturn(List.of());

        service.updateHeroBasicInfo(10L, new AdminHeroBasicUpdateRequest(
                null,
                null,
                null,
                List.of("Pháp sư", "Đấu sĩ"),
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(hero.getHeroClass()).isEqualTo("Pháp sư");
        assertThat(hero.getClasses())
                .extracting(HeroClass::resolveDisplayName)
                .containsExactlyInAnyOrder("Pháp sư", "Đấu sĩ");
    }

    @Test
    void deleteAttributeRejectsUsedAttribute() {
        HeroAttribute attribute = attribute(5L, "Cơ động");
        when(heroAttributeRepository.findById(5L)).thenReturn(Optional.of(attribute));
        when(heroAttributeRepository.countHeroUsage(5L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteAttribute(5L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("Không thể xóa đặc điểm");
                });
    }

    private Hero hero(Long id, String name, String slug) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(slug);
        return hero;
    }

    private HeroRole role(Long id, String code, String name) {
        HeroRole role = new HeroRole();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        return role;
    }

    private HeroAttribute attribute(Long id, String name) {
        HeroAttribute attribute = new HeroAttribute();
        attribute.setId(id);
        attribute.setName(name);
        return attribute;
    }

    private HeroClass heroClass(Long id, String name) {
        HeroClass heroClass = new HeroClass();
        heroClass.setId(id);
        heroClass.setName(name);
        heroClass.setDisplayName(name);
        return heroClass;
    }
}
