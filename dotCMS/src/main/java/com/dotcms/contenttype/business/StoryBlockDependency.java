package com.dotcms.contenttype.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Represents a dependency reference found within a Story Block field.
 * Contains the identifier and language ID of the referenced contentlet.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableStoryBlockDependency.class)
@JsonDeserialize(as = ImmutableStoryBlockDependency.class)
public interface StoryBlockDependency {

    /**
     * The identifier of the referenced contentlet (image, video, or content).
     *
     * @return The contentlet identifier.
     */
    @JsonProperty("identifier")
    String identifier();

    /**
     * The language ID of the referenced contentlet.
     * This is the actual language of the dependency as stored in the Story Block field,
     * which may differ from the parent contentlet's language.
     *
     * @return The language ID of the dependency.
     */
    @JsonProperty("languageId")
    long languageId();

    /**
     * Creates a new builder for StoryBlockDependency.
     *
     * @return A new builder instance.
     */
    static ImmutableStoryBlockDependency.Builder builder() {
        return ImmutableStoryBlockDependency.builder();
    }

    /**
     * Creates a StoryBlockDependency from identifier and languageId.
     *
     * @param identifier The contentlet identifier.
     * @param languageId The language ID.
     * @return A new StoryBlockDependency instance.
     */
    static StoryBlockDependency of(final String identifier, final long languageId) {
        return ImmutableStoryBlockDependency.builder()
                .identifier(identifier)
                .languageId(languageId)
                .build();
    }
}
