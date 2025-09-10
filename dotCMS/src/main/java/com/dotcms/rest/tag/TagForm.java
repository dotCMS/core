package com.dotcms.rest.tag;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Tag Form
 * Capture many Tags represented by RestTag and assign an owner to it
 */
public class TagForm extends Validated {

    @Nullable
    private final String ownerId;

    @NotNull
    private final Map<String,RestTag> tags;

    /**
     * Json Creator constructor
     * @param tags
     * @param ownerId
     */
    @JsonCreator
    public TagForm(@JsonProperty("tags")  final Map<String,RestTag> tags, @JsonProperty("ownerId")  final String ownerId) {
        this.tags = tags;
        this.ownerId = ownerId;
    }

    /**
     * tags map getter
     * @return
     */
    public Map<String, RestTag> getTags() {
        return tags;
    }

    /**
     * Tags Owner getter
     * @return
     */
    public String getOwnerId() {
        return ownerId;
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
