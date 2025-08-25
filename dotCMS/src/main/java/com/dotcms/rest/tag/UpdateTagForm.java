package com.dotcms.rest.tag;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonDeserialize(builder = UpdateTagForm.Builder.class)
public class UpdateTagForm extends Validated {

    @NotNull
    public final String siteId;

    @NotNull
    public final String tagName;

    public final String tagId;

    public UpdateTagForm(final Builder builder) {
        this.siteId = builder.siteId;
        this.tagName = builder.tagName;
        this.tagId = builder.tagId;
    }

    public static final class Builder {

        @JsonProperty
        private String siteId;
        
        @JsonProperty("tagName")
        @JsonAlias({"name"})
        private String tagName;
        
        @JsonProperty
        private String tagId;

        public Builder() {
        }

        UpdateTagForm build(){
            return new UpdateTagForm(this);
        }

        Builder siteId(final String siteId) {
            this.siteId = siteId;
            return this;
        }

        Builder tagName(final String tagName) {
            this.tagName = tagName;
            return this;
        }

        Builder tagId(final String tagId) {
            this.tagId = tagId;
            return this;
        }

    }

    /**
     * Modern getter methods for v2 API compatibility.
     * These provide cleaner field names while maintaining backward compatibility.
     */
    public String getName() { 
        return tagName; 
    }
    
    public String getSiteId() { 
        return siteId; 
    }
    
    public String getTagId() { 
        return tagId; 
    }

    @Override
    public void checkValid() {
        // First run Bean Validation (@NotNull checks)
        super.checkValid();
        
        // Custom business rules (migrated from validateUpdateTag)
        if (tagName != null) {
            if (tagName.contains(",")) {
                throw new BadRequestException("Tag name cannot contain commas");
            }
            if (tagName.trim().isEmpty()) {
                throw new BadRequestException("Tag name cannot be blank");
            }
            if (tagName.length() > 255) {
                throw new BadRequestException("Tag name cannot exceed 255 characters");
            }
        }
    }

    /**
     * good old toString
     * @return
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }

}
