package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Form for bulk contentlet deletion by identifier. Each identifier's language siblings
 * are resolved and permanently destroyed (bypasses trash).
 *
 * @author hassandotcms
 */
@Schema(description = "List of contentlet identifiers to permanently destroy")
public class DeleteContentletsForm extends Validated {

    @JsonProperty("identifiers")
    @Schema(
            description = "List of contentlet identifiers. All language siblings of each "
                    + "identifier will be permanently destroyed.",
            example = "[\"abc123\", \"def456\", \"ghi789\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final List<String> identifiers;

    @JsonCreator
    public DeleteContentletsForm(
            @JsonProperty("identifiers") final List<String> identifiers) {
        this.identifiers = identifiers;
        checkValid();
    }

    @Override
    public void checkValid() {
        super.checkValid();
        if (identifiers == null || identifiers.isEmpty()) {
            throw new BadRequestException("identifiers must not be empty");
        }
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }
}
