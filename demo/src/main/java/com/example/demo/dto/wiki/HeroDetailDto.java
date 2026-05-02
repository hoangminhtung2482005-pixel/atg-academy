package com.example.demo.dto.wiki;

import com.example.demo.entity.Guide;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroMatchup;
import com.example.demo.entity.HeroSkill;

import java.time.LocalDateTime;
import java.util.List;

public record HeroDetailDto(
        Long id,
        String slug,
        String name,
        String title,
        String avatarUrl,
        String heroClass,
        List<String> classes,
        List<String> laneRoles,
        List<String> attributes,
        String difficulty,
        String description,
        String lore,
        String portraitUrl,
        String bannerUrl,
        List<HeroSkillDto> skills,
        List<HeroMatchupDto> matchups,
        List<RelatedGuideDto> relatedGuides
) {
    public static HeroDetailDto from(
            Hero hero,
            List<HeroSkill> skills,
            List<HeroMatchup> matchups,
            List<Guide> relatedGuides
    ) {
        if (hero == null) {
            return null;
        }

        return new HeroDetailDto(
                hero.getId(),
                hero.getSlug(),
                hero.getName(),
                hero.getTitle(),
                hero.getAvatarUrl(),
                HeroSummaryDto.primaryClass(hero),
                HeroSummaryDto.classes(hero),
                HeroSummaryDto.laneRoles(hero),
                HeroSummaryDto.attributes(hero),
                hero.getDifficulty(),
                hero.getDescription(),
                hero.getLore(),
                hero.getPortraitUrl(),
                hero.getBannerUrl(),
                skills == null ? List.of() : skills.stream().map(HeroSkillDto::from).toList(),
                matchups == null ? List.of() : matchups.stream().map(HeroMatchupDto::from).toList(),
                relatedGuides == null ? List.of() : relatedGuides.stream().map(RelatedGuideDto::from).toList()
        );
    }

    public record RelatedGuideDto(
            Long id,
            String title,
            String coverImageUrl,
            String category,
            String lane,
            String excerpt,
            Integer viewCount,
            Integer readingTimeMinutes,
            LocalDateTime publishedAt
    ) {
        public static RelatedGuideDto from(Guide guide) {
            if (guide == null) {
                return null;
            }

            return new RelatedGuideDto(
                    guide.getId(),
                    guide.getTitle(),
                    guide.getCoverImageUrl(),
                    guide.getCategory(),
                    guide.getLane(),
                    guide.getExcerpt(),
                    guide.getViewCount(),
                    guide.getReadingTimeMinutes(),
                    guide.getPublishedAt()
            );
        }
    }
}
