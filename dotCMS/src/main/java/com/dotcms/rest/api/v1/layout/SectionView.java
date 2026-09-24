package com.dotcms.rest.api.v1.layout;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A navigation section as the Tools portlet reads it: the stored section plus one localized
 * title per tool id, aligned index by index with {@code portletIds}.
 *
 * @param id            the section id
 * @param name          the section name, unique among sections
 * @param icon          the icon name (the section's stored description)
 * @param tabOrder      the position in the navigation; only the order matters, not the value
 * @param portletIds    the tool ids in the section, in stored order
 * @param portletTitles one localized title per entry of {@code portletIds}, same order
 * @author hassandotcms
 */
@Schema(description = "A navigation section (layout) with its ordered tools and their localized titles")
public record SectionView(
        @Schema(description = "Section id", example = "2df9f117-b140-44bf-93d7-5b10a36fb7f9") String id,
        @Schema(description = "Section name, unique among sections", example = "Marketing") String name,
        @Schema(description = "Icon name shown next to the section", example = "campaign") String icon,
        @Schema(description = "Position in the navigation; only the relative order is meaningful", example = "3") int tabOrder,
        @Schema(description = "Tool (portlet) ids in the section, in stored order") List<String> portletIds,
        @Schema(description = "Localized title of each tool, aligned with portletIds") List<String> portletTitles) {
}
