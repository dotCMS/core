package com.dotcms.rest.tag;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.ErrorEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Form for creating a single tag via v2 API
 */
public class SingleTagForm extends Validated {

    @NotNull(message = "Tag name is required")
    @Size(min = 1, max = 255, message = "Tag name must be between 1 and 255 characters")
    private final String name;

    @Nullable
    private final String siteId;

    @Nullable
    private final String ownerId;

    @Nullable
    private final Boolean persona;

    @JsonCreator
    public SingleTagForm(
            @JsonProperty("name") final String name,
            @JsonProperty("siteId") final String siteId,
            @JsonProperty("ownerId") final String ownerId,
            @JsonProperty("persona") final Boolean persona) {
        this.name = name;
        this.siteId = siteId;
        this.ownerId = ownerId;
        this.persona = persona;
    }

    public String getName() {
        return name;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Boolean getPersona() {
        return persona;
    }
    
    @Override
    public void checkValid() {
        // First run Bean Validation
        super.checkValid();
        
        // Then add custom business rules
        if (name != null) {
            // Check for commas
            if (name.contains(",")) {
                final List<ErrorEntity> errors = List.of(
                    new ErrorEntity(null, "Tag name cannot contain commas", "name")
                );
                throw new com.dotcms.rest.exception.BadRequestException(null, errors, "Tag name cannot contain commas");
            }
            
            // Check for blank/whitespace only
            if (name.trim().isEmpty()) {
                final List<ErrorEntity> errors = List.of(
                    new ErrorEntity(null, "Tag name cannot be blank", "name")
                );
                throw new com.dotcms.rest.exception.BadRequestException(null, errors, "Tag name cannot be blank");
            }
        }
    }
}