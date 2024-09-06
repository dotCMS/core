package com.dotcms.contenttype.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This API allows you to interact with information related to Story Block fields in a given Contentlet. For example, it
 * allows you to handle Contentlet dependencies and references inside such a field.
 *
 * @author Jonathan Sanchez
 * @since Oct 19th, 2022
 */
public interface StoryBlockAPI {

    String CONTENT_KEY = "content";
    String TYPE_KEY = "type";
    String ATTRS_KEY = "attrs";
    String DATA_KEY = "data";
    String IDENTIFIER_KEY = "identifier";
    String INODE_KEY = "inode";
    String LANGUAGE_ID_KEY = "languageId";

    /**
     * Contains the types of Contentlets that can be added to a Story Block field
     */
    Set<String> allowedTypes = new ImmutableSet.Builder<String>().add("dotContent", "dotImage", "dotVideo").build();

    /**
     * Updates all Contentlets referenced in every {@link com.dotcms.contenttype.model.field.StoryBlockField}  of the
     * specified Contentlet with the latest field values and properties from their respective live version. This allows
     * the Story Block field to reflect the appropriate values of the Contentlets in it without having to manually
     * publish the parent Contentlet.
     *
     * @param contentlet The Contentlet containing the Story Block field(s).
     *
     * @return The {@link StoryBlockReferenceResult} object containing the final result of the refreshing process.
     * Encapsulates the allowed types for contentlets on the story block
     */

    StoryBlockReferenceResult refreshReferences(final Contentlet contentlet);

    /**
     * Takes the actual JSON value of a given {@link com.dotcms.contenttype.model.field.StoryBlockField} and refreshes
     * any Contentlet references in it, if applicable.
     *
     * @param storyBlockValue The actual value of the Story Block field.
     * @param parentContentletIdentifier String identifier of the parent contentlet
     *
     * @return An instance of {@link StoryBlockReferenceResult} containing the result of the refresh process.
     */
    StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue, final String parentContentletIdentifier);

    /**
     * Traverses every {@link com.dotcms.contenttype.model.field.StoryBlockField} in the specified Contentlet, and
     * returns the list with the Identifiers that are being referenced in such a field.
     *
     * @param contentlet The {@link Contentlet} whose Story Block references must be retrieved.
     *
     * @return The {@link List} with the Identifiers of the Contentlets that are being referenced in every Story
     * Block fields.
     */
    List<String> getDependencies (final Contentlet contentlet);

    /**
     * Returns the list with the Identifiers that are being referenced in the specified Story Block value.
     *
     * @param storyBlockValue The value of the Story Block -- usually in the form of a JSON String.
     *
     * @return The {@link List} with the Identifiers of the Contentlets that are being referenced in the Story Block
     * field.
     */
    List<String> getDependencies (final Object storyBlockValue);

    /**
     * Adds a Contentlet to the specified Story Block field.
     *
     * @param storyBlockValue The Story Block field -- usually as a JSON String.
     * @param contentlet      The {@link Contentlet} object being added.
     *
     * @return The value of the Story Block field, usually in the form of a JSON String.
     */
    Object addContentlet(final Object storyBlockValue, final Contentlet contentlet);

    /**
     * Takes the actual value of the Story Block field in the form of JSON and transforms it into a Linked Map.
     *
     * @param blockEditorValue The value of the Story Block field as JSON.
     *
     * @return The Story Block field as a {@link LinkedHashMap}.
     *
     * @throws JsonProcessingException An error occurred when processing the JSON data.
     */
    LinkedHashMap<String, Object> toMap(final Object blockEditorValue) throws JsonProcessingException;

    /**
     * Takes the Map containing the properties of a specific Story Block field and transforms it into JSON data as a
     * String value.
     *
     * @param blockEditorMap The Map with the Contentlet properties.
     *
     * @return The Story Block field as a JSON String.
     *
     * @throws JsonProcessingException An error occurred when transforming the Map into JSON data.
     */
    String toJson (final Map<String, Object> blockEditorMap) throws JsonProcessingException;

}
