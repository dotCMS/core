package com.dotcms.contenttype.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
    Set<String> allowedTypes = new ImmutableSet.Builder<String>().add("dotContent", "dotImage", "dotVideo", "dotAudio").build();

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
     * Same as {@link #refreshReferences(Contentlet)}, but the referenced Contentlets are looked up
     * on behalf of the given user instead of the user of the current request. A reference that
     * cannot be read and refreshed for that user is kept in the document with only the keys that
     * identify it ({@code identifier} and {@code languageId}) in its data map.
     * <p>The {@link com.dotmarketing.util.PageMode} of the current request still decides which
     * version of each referenced Contentlet is read (live or working) and how its related content
     * is loaded, so the given user should be the user of that request or one with the same
     * access.</p>
     *
     * @param contentlet           The Contentlet containing the Story Block field(s).
     * @param user                 The user on whose behalf the referenced Contentlets are read.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account when
     *                             reading the referenced Contentlets.
     *
     * @return The {@link StoryBlockReferenceResult} object containing the final result of the refreshing process.
     */
    StoryBlockReferenceResult refreshReferences(final Contentlet contentlet, final User user,
                                                final boolean respectFrontendRoles);

    /**
     * Applies the read permissions of the user of the current request (anonymous when there is
     * none) to the Contentlets referenced in every Story Block field of the given Contentlet,
     * without refreshing them: a reference that user can read keeps the data stored with it, and
     * any other reference keeps only the keys that identify it. Used where references are not
     * refreshed on read.
     *
     * @param contentlet The Contentlet containing the Story Block field(s). It is modified in place.
     *
     * @return The {@link StoryBlockReferenceResult} with the given Contentlet.
     */
    StoryBlockReferenceResult limitReferencesToReader(final Contentlet contentlet);

    /**
     * Same as {@link #refreshStoryBlockValueReferences(Object, String)}, but the referenced
     * Contentlets are looked up on behalf of the given user instead of the user of the current
     * request. A reference that cannot be read and refreshed for that user is kept in the document
     * with only the keys that identify it ({@code identifier} and {@code languageId}) in its data
     * map. As in {@link #refreshReferences(Contentlet, User, boolean)}, the page mode of the
     * current request still decides which version is read and how related content is loaded.
     *
     * @param storyBlockValue            The actual value of the Story Block field.
     * @param parentContentletIdentifier String identifier of the parent contentlet
     * @param user                       The user on whose behalf the referenced Contentlets are read.
     * @param respectFrontendRoles       If {@code true}, front-end roles are taken into account when
     *                                   reading the referenced Contentlets.
     *
     * @return An instance of {@link StoryBlockReferenceResult} containing the result of the refresh process.
     */
    StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue,
                                                               final String parentContentletIdentifier,
                                                               final User user,
                                                               final boolean respectFrontendRoles);

    /**
     * Traverses every {@link com.dotcms.contenttype.model.field.StoryBlockField} in the specified Contentlet, and
     * returns the list with the dependency information (identifier and languageId) for contentlets being referenced
     * in such fields.
     *
     * @param contentlet The {@link Contentlet} whose Story Block references must be retrieved.
     *
     * @return The {@link List} of {@link StoryBlockDependency} objects containing the identifier and languageId
     * of the Contentlets that are being referenced in every Story Block field.
     */
    List<StoryBlockDependency> getDependencies (final Contentlet contentlet);

    /**
     * Returns the list with the dependency information (identifier and languageId) for contentlets being referenced
     * in the specified Story Block value.
     *
     * @param storyBlockValue The value of the Story Block -- usually in the form of a JSON String.
     *
     * @return The {@link List} of {@link StoryBlockDependency} objects containing the identifier and languageId
     * of the Contentlets that are being referenced in the Story Block field.
     */
    List<StoryBlockDependency> getDependencies (final Object storyBlockValue);

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
     * Takes the actual value of the Story Block field and transforms it into a Linked Map. The value may arrive
     * in two shapes depending on the read path: a raw JSON String, or a Map when the field has already been
     * hydrated by the contentlet transformers (e.g. during page rendering). Consumers of a Block Editor value
     * should convert through this method rather than assuming one shape. Note that a Map input yields a
     * <b>shallow</b> copy: nested structures are shared with the input and must not be mutated.
     *
     * @param blockEditorValue The value of the Story Block field, either a JSON String or an already-hydrated Map.
     *
     * @return The Story Block field as a {@link LinkedHashMap}.
     *
     * @throws JsonProcessingException An error occurred when processing the JSON data.
     */
    LinkedHashMap<String, Object> toMap(final Object blockEditorValue) throws JsonProcessingException;

    /**
     * Tolerant variant of {@link #toMap(Object)} for read paths that must never fail or reshape
     * stored data. A Map or JSON-object String converts exactly like {@link #toMap(Object)}; any
     * other value — e.g. HTML from a WYSIWYG field later converted to Block Editor — is returned
     * unchanged, with a rate-limited WARN identifying the unparseable value. Use this from
     * consumers that render stored values (GraphQL, REST); use the strict {@link #toMap(Object)}
     * from transforms that require a valid Story Block document and must fail otherwise.
     *
     * @param blockEditorValue The value of the Story Block field, in any of the shapes described above.
     * @param valueContext     Describes where the value came from (contentlet identifier, field
     *                         variable, etc.); only evaluated when the WARN is logged.
     *
     * @return The Story Block field as a {@link LinkedHashMap}, or {@code blockEditorValue}
     * unchanged when it does not hold a Story Block JSON document.
     */
    Object toMapOrPassthrough(final Object blockEditorValue, final Supplier<String> valueContext);

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
