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
     * Updates all Contentlets referenced in every {@link com.dotcms.contenttype.model.field.StoryBlockField} of the
     * specified Contentlet with the latest field values and properties of the version the page mode of the current
     * request selects (live or working). This allows the Story Block field to reflect the appropriate values of the
     * Contentlets in it without having to manually publish the parent Contentlet.
     * <p>The referenced Contentlets are read on behalf of the user of the current request, or the anonymous user
     * when there is none. See {@link #refreshReferences(Contentlet, User, boolean)} for what a reference that user
     * cannot read becomes.</p>
     *
     * @param contentlet The Contentlet containing the Story Block field(s).
     *
     * @return The {@link StoryBlockReferenceResult} object containing the final result of the refreshing process.
     */
    StoryBlockReferenceResult refreshReferences(final Contentlet contentlet);

    /**
     * Takes the actual JSON value of a given {@link com.dotcms.contenttype.model.field.StoryBlockField} and refreshes
     * any Contentlet references in it, if applicable. The referenced Contentlets are read on behalf of the user of the
     * current request, or the anonymous user when there is none; use
     * {@link #refreshStoryBlockValueReferences(Object, String, User, boolean)} to read them for another user.
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
     * Checks the Contentlets referenced in every Story Block field of the given Contentlet against
     * the READ permission of the user of the current request (the logged-in user, or the
     * anonymous user when nobody is logged in), without refreshing them from the Contentlets they
     * point to. For each reference:
     * <ul>
     *     <li>If the user can read the version the page mode selects (live or working), the data
     *     stored with the reference is kept. Content embedded in that data (related Contentlets,
     *     references in its own Story Block fields) is kept only where the user can read it too.</li>
     *     <li>Otherwise, or when the page mode selects no version, the reference stays in the
     *     document with only the keys that identify it ({@code identifier} and
     *     {@code languageId}).</li>
     * </ul>
     * This is the counterpart of {@link #refreshReferences(Contentlet)} for reads that do not
     * refresh references, such as cached reads when {@code REFRESH_BLOCK_EDITOR_REFERENCES} is
     * disabled.
     * <p>Nothing is changed when there is no current request (no reader to check), inside a
     * database transaction, or when the Content Type has no Story Block fields.</p>
     *
     * @param contentlet The Contentlet containing the Story Block field(s). It is modified in place,
     *                   so it must not be an instance shared with other readers (such as a cached
     *                   one).
     *
     * @return The {@link StoryBlockReferenceResult} with the given Contentlet; it is marked as
     * refreshed when any Story Block field changed.
     */
    StoryBlockReferenceResult limitReferencesToReader(final Contentlet contentlet);

    /**
     * Same as {@link #limitReferencesToReader(Contentlet)} for the given reader, and applied
     * whether or not there is a request or an open transaction. Meant for returning a Contentlet
     * just saved to the user who saved it: the saved Contentlet holds every reference with the
     * data stored with it.
     *
     * @param contentlet           The Contentlet whose Story Block fields are processed, modified
     *                             in place.
     * @param user                 The reader.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account.
     *
     * @return The {@link StoryBlockReferenceResult} with the given Contentlet.
     */
    StoryBlockReferenceResult limitReferencesToReader(final Contentlet contentlet, final User user,
                                                      final boolean respectFrontendRoles);

    /**
     * Returns a copy of a Story Block value in which every Contentlet reference, at any level of
     * the document, keeps only the keys that identify it ({@code identifier} and
     * {@code languageId}). The result is the same for every user, so it can be kept where
     * different users will read it, such as code cached for a page. A value that is not a Story
     * Block document is returned as it is.
     *
     * @param storyBlockValue The Story Block value, as a JSON String or a Map. It is not modified.
     *
     * @return The reduced copy, of the same kind as the given value (JSON String or Map).
     */
    Object withReferenceKeysOnly(final Object storyBlockValue);

    /**
     * Fills back the references of a submitted Story Block value that arrive with only the keys
     * that identify them ({@code identifier} and {@code languageId}), taking the data stored with
     * the same reference (same identifier and language) in the value currently saved. An editor
     * gets references reduced like that when it cannot read them, and sends them back as it got
     * them; without this, saving would replace the data stored with them for every reader.
     * <p>
     * References that arrive with data are kept as submitted, and a reduced reference with no
     * stored data to take stays reduced. Nothing is read for any user: the data only comes from
     * the saved value, and readers still get it according to their permissions.
     *
     * @param submittedValue The Story Block value being saved, as a JSON String or a Map. It is
     *                       not modified.
     * @param storedValue    Supplies the Story Block value currently saved for the same field, or
     *                       {@code null} if there is none. Only called when a submitted reference
     *                       carries nothing but its keys.
     *
     * @return The submitted value, or, when any reference was filled back, a copy of it as a JSON
     * String.
     */
    Object restoreReferenceData(final Object submittedValue, final Supplier<Object> storedValue);

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
