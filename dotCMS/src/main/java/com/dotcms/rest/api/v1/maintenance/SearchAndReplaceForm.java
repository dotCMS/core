package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * Form for database-wide search and replace operations.
 * Performs find/replace across text content in contentlets, containers,
 * templates, fields, and links.
 *
 * @author hassandotcms
 */
@Schema(description = "Database-wide search and replace parameters")
public class SearchAndReplaceForm extends Validated {

    @JsonProperty("searchString")
    @NotNull(message = "searchString is required")
    @Schema(
            description = "Text to search for across all content tables. Must not be empty.",
            example = "http://old-domain.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String searchString;

    @JsonProperty("replaceString")
    @NotNull(message = "replaceString is required")
    @Schema(
            description = "Replacement text. Can be empty to delete all occurrences of searchString.",
            example = "https://new-domain.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String replaceString;

    @JsonCreator
    public SearchAndReplaceForm(
            @JsonProperty("searchString") final String searchString,
            @JsonProperty("replaceString") final String replaceString) {
        this.searchString = searchString;
        this.replaceString = replaceString;
        checkValid();
    }

    @Override
    public void checkValid() {
        super.checkValid();
        if (!UtilMethods.isSet(searchString)) {
            throw new BadRequestException("searchString must not be empty");
        }
    }

    public String getSearchString() {
        return searchString;
    }

    public String getReplaceString() {
        return replaceString;
    }
}
