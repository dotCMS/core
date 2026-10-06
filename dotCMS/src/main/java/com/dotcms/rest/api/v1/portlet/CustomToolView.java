package com.dotcms.rest.api.v1.portlet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The editable configuration of a custom content tool, as the Tools portlet's Edit dialog needs
 * it to prefill. Base types and content types are stored as comma-separated text and returned
 * here as lists.
 *
 * @param portletId    the stored id, {@code c_} prefix included
 * @param portletName  the display name the admin gave the tool
 * @param baseTypes    base type names as stored (CONTENT, WIDGET, PERSONA, ...); empty when none
 * @param contentTypes content type variable names; empty when none
 * @param dataViewMode {@code list} or {@code card}, exactly as stored
 * @author hassandotcms
 */
@Schema(description = "Editable configuration of a custom content tool")
public record CustomToolView(
        @Schema(description = "Stored id including the c_ prefix", example = "c_press-releases")
        String portletId,
        @Schema(description = "Display name of the tool", example = "Press Releases")
        String portletName,
        @Schema(description = "Base type names as stored (CONTENT, WIDGET, PERSONA, FILEASSET, HTMLPAGE, "
                + "VANITY_URL, DOTASSET, FORM, KEY_VALUE); empty when none")
        List<String> baseTypes,
        @Schema(description = "Content type variable names; empty when none")
        List<String> contentTypes,
        @Schema(description = "list or card, as stored", example = "card")
        String dataViewMode) {
}
