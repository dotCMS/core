package com.dotcms.contenttype.business;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * Implementation class for the {@link StoryBlockAPI}.
 *
 * @author Jonathan Sanchez
 * @since Oct 19th, 2022
 */
public class StoryBlockAPIImpl implements StoryBlockAPI {

    private static final String DEFAULT_MAX_RECURSION_LEVEL = "2";
    /**
     * This request attribute keeps track of the current level of related content that is being
     * processed. This is the main flag that keeps contents from loading infinite levels of
     * associated contentlets.
     */
    private static final String CURRENT_DEPTH_ATTR = "CURRENT_DEPTH";
    /**
     * Maximum map-nesting depth allowed when {@link #refreshNestedStoryBlockValues} searches a
     * hydrated relationship payload for embedded Story Block fields.
     * <p>
     * The relationship depth is always capped at {@code 0–3} by {@link #getInitialDepthValue()}.
     * For the deepest case (depth 3) a Story Block string sits at nesting level
     * {@code field value → level-1 contentlet map → level-2 contentlet map → level-3 Story Block string},
     * so 4 levels of traversal are sufficient. Using a larger value would allow unbounded recursion
     * on malformed or adversarially crafted payloads.
     * </p>
     */
    private static final int MAX_NESTED_STORY_BLOCK_REFRESH_DEPTH = 4;
    /**
     * Thread-local set of contentlet identifiers currently being processed by
     * {@link #refreshBlockEditorDataMap}. Used to detect and break circular reference chains
     * (e.g. A → B → A) that would otherwise cause a {@link StackOverflowError}.
     */
    private static final ThreadLocal<Set<String>> REFRESH_PROCESSING_IDENTIFIERS =
            ThreadLocal.withInitial(HashSet::new);
    private static final Lazy<String> MAX_RELATIONSHIP_DEPTH = Lazy.of(() -> Config.getStringProperty(
            "STORY_BLOCK_MAX_RELATIONSHIP_DEPTH", DEFAULT_MAX_RECURSION_LEVEL));
    /**
     * Minimum interval between two {@link #toMapOrPassthrough} WARN entries for the same value
     * context, so legacy non-JSON values (e.g. converted WYSIWYG fields) stay visible in the logs
     * without flooding them on every read.
     */
    private static final int UNPARSEABLE_VALUE_WARN_INTERVAL_MILLIS = 30_000;
    /** The keys of a content node's data map that identify the referenced Contentlet. */
    private static final Set<String> REFERENCE_KEYS = Set.of(IDENTIFIER_KEY, LANGUAGE_ID_KEY);
    /** Suffix of the companion property holding a Story Block field's document as stored. */
    private static final String RAW_SUFFIX = "_raw";

    /**
     * This method hydrates all the Story Block -- a.k.a. Block Editor -- fields within the
     * specified contentlet, adhering to the following rules:
     *
     * <h5>Relationship Loading:</h5>
     * Relationships within the {@code contentlet} are loaded based on the DEPTH parameter specified
     * in the request, just like it works in the Content REST Endpoint. If no depth is set, the
     * default value is null.
     *
     * <h5>Story Block Hydration:</h5>
     * All first-level {@link Contentlet}s with Story Block fields within the specified
     * {@code contentlet} are fully hydrated. However, nested Story Blocks within those Story Blocks
     * are not hydrated, only their IDs are loaded.
     *
     * <h5>Depth Reduction for Relationships:</h5>
     * For relationships in Story Blocks at any level, the depth is reduced by 1 at each nested
     * level. For example, if the depth at the current level is 2, it becomes 0 for the nested
     * level. Similarly, a depth of 3 at the current level becomes 1 at the next level. This
     * calculation is based on what the Content REST Endpoint does when handling relationships. For
     * more details on how this specific logic currently works, please refer to
     * {@link com.dotcms.rest.ContentResource#addRelatedContentToJsonArray(HttpServletRequest,
     * HttpServletResponse, String, User, int, boolean, Contentlet, Set, long, boolean, Field,
     * boolean, boolean, boolean)}
     *
     * <h5>Example Scenario:</h5>
     * Consider the following setup: A Content Type has a Relationship field that relates to itself
     * and to another Contentlet with a Story Block field. You have 6 contentlets: A, B, C, D, E,
     * and F, related like this:
     * <ul>
     *     <li>Content A: Related to Content B, with Content C added to the Block Editor field.</li>
     *     <li>Content B: Related to Content D, with Content E added to the Block Editor field.</li>
     *     <li>Content C: Related to Content F.</li>
     * </ul>
     * If you call this method with Content A, and set a depth of 3 in the current request:
     * <ul>
     *     <li>Content B: Will be loaded as a related contentlet of A with a depth of 3.</li>
     *     <li>Content C: Will be loaded as a Story Block contentlet of A with a depth of 1. This
     *     means F (related to C) will not be loaded.</li>
     *     <li>Content D: Will be loaded as a related contentlet of B with a depth of 1. This
     *     means that any further content related to D will not be loaded.</li>
     *     <li>Content E: Will not be hydrated; only its ID will be returned.</li>
     * </ul>
     *
     * @param contentlet The Contentlet containing the Story Block field(s).
     *
     * @return The {@link StoryBlockReferenceResult} object containing the refreshed
     * {@link Contentlet} with the appropriate hydrated data.
     */
    @Override
    public StoryBlockReferenceResult refreshReferences(final Contentlet contentlet) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        return this.refreshReferences(contentlet, this.resolveRequestUser(request),
                PageMode.get(request).respectAnonPerms);
    }

    /**
     * Refreshes the Story Block references of the given Contentlet on behalf of the given user.
     * See {@link StoryBlockAPI#refreshReferences(Contentlet, User, boolean)}.
     *
     * @param contentlet           The Contentlet containing the Story Block field(s).
     * @param user                 The user on whose behalf the referenced Contentlets are read.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account.
     *
     * @return The {@link StoryBlockReferenceResult} with the refreshed Contentlet.
     */
    @Override
    @CloseDBIfOpened
    public StoryBlockReferenceResult refreshReferences(final Contentlet contentlet, final User user,
                                                       final boolean respectFrontendRoles) {
        final boolean inTransaction = DbConnectionFactory.inTransaction();
        final ContentType contentType = null != contentlet ? contentlet.getContentType() : null;
        if (!inTransaction && null != contentlet && null != contentType &&
                contentType.hasStoryBlockFields()) {

            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            final int initialDepthValue = this.getCurrentDepthValue(request);
            // The current depth level must ALWAYS be handled and set at the very beginning, even
            // when the current HTTP Request object is null; i.e., hasn't been set yet
            final boolean setCurrentDepthValue = null == request || request.getAttribute(CURRENT_DEPTH_ATTR) != null;

            if (setCurrentDepthValue) {
                final Integer currentDepth = this.decreaseDepthValue(initialDepthValue);
                if (null == request) {
                    // No request to read on behalf of: the stored values are left as they are
                    return new StoryBlockReferenceResult(false, contentlet);
                }
                request.setAttribute(CURRENT_DEPTH_ATTR, currentDepth);
                // Loaded while another Story Block is being refreshed: its references are not
                // refreshed again, but the reader's permissions still apply to them
                return this.processStoryBlockFields(contentlet, user, respectFrontendRoles, false);
            }

            return this.processStoryBlockFields(contentlet, user, respectFrontendRoles, true);
        }
        return new StoryBlockReferenceResult(false, contentlet);
    }

    @Override
    @CloseDBIfOpened
    public StoryBlockReferenceResult limitReferencesToReader(final Contentlet contentlet) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final ContentType contentType = null != contentlet ? contentlet.getContentType() : null;
        if (null == request || DbConnectionFactory.inTransaction() || null == contentType
                || !contentType.hasStoryBlockFields()) {
            return new StoryBlockReferenceResult(false, contentlet);
        }
        return this.processStoryBlockFields(contentlet, this.resolveRequestUser(request),
                PageMode.get(request).respectAnonPerms, false);
    }

    /**
     * Processes the references in every Story Block field of a Contentlet for the given user, and
     * writes the result to the field and to its raw companion ({@code <field>_raw}), which holds
     * the same document as stored. A field whose references cannot be processed gets every
     * reference reduced to the keys that identify it.
     *
     * @param contentlet           The Contentlet, modified in place.
     * @param user                 The user on whose behalf the references are read.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account.
     * @param refresh              If {@code true}, references are refreshed from the Contentlets
     *                             they point to; otherwise only the user's READ permission on them
     *                             is checked, and the stored data is kept for those that pass.
     *
     * @return The {@link StoryBlockReferenceResult} with the given Contentlet.
     */
    private StoryBlockReferenceResult processStoryBlockFields(final Contentlet contentlet, final User user,
                                                              final boolean respectFrontendRoles,
                                                              final boolean refresh) {
        final MutableBoolean processed = new MutableBoolean(false);
        contentlet.getContentType().fields(StoryBlockField.class).forEach(field -> {
            final String rawVariable = field.variable() + RAW_SUFFIX;
            final Object storyBlockValue = contentlet.get(field.variable());
            final Object rawValue = contentlet.get(rawVariable);
            if (null != storyBlockValue) {
                final Object result = this.processFieldValue(storyBlockValue, contentlet.getIdentifier(),
                        user, respectFrontendRoles, refresh);
                if (result != storyBlockValue) {
                    processed.setTrue();
                    contentlet.setProperty(field.variable(), result);
                }
                if (null != rawValue) {
                    contentlet.setProperty(rawVariable, rawValue.equals(storyBlockValue) ? result
                            : this.processFieldValue(rawValue, contentlet.getIdentifier(), user,
                                    respectFrontendRoles, refresh));
                }
            } else if (null != rawValue) {
                contentlet.setProperty(rawVariable, this.processFieldValue(rawValue,
                        contentlet.getIdentifier(), user, respectFrontendRoles, refresh));
            }
        });
        return new StoryBlockReferenceResult(processed.booleanValue(), contentlet);
    }

    /**
     * Processes the references of one Story Block value. Never throws: if the value cannot be
     * processed, every reference in it is reduced to its keys, and a value that is not a Story
     * Block document is returned as it is.
     *
     * @return The processed value, or the given one when nothing changed.
     */
    private Object processFieldValue(final Object storyBlockValue, final String parentContentletIdentifier,
                                     final User user, final boolean respectFrontendRoles, final boolean refresh) {
        try {
            final StoryBlockReferenceResult result = this.processStoryBlockValue(storyBlockValue,
                    parentContentletIdentifier, user, respectFrontendRoles, refresh);
            return result.isRefreshed() ? result.getValue() : storyBlockValue;
        } catch (final Exception e) {
            Logger.warnAndDebug(StoryBlockAPIImpl.class, String.format(
                    "Story Block references of contentlet '%s' could not be processed; they are returned " +
                    "without their data: %s", parentContentletIdentifier, ExceptionUtil.getErrorMessage(e)), e);
            return this.reduceEmbeddedContent(storyBlockValue, false);
        }
    }

    /**
     * Reduces the data map of a Story Block content node to the keys that identify the referenced
     * Contentlet ({@code identifier} and {@code languageId}).
     *
     * @param dataMap The data map of the content node, modified in place.
     */
    private void keepOnlyReferenceKeys(final Map<String, Object> dataMap) {
        dataMap.keySet().retainAll(REFERENCE_KEYS);
    }

    /**
     * Reduces every Contentlet reference in a Story Block document to its reference keys, at any
     * level of the document. Used for Story Block fields of a referenced Contentlet, whose own
     * references are not read again.
     *
     * @param storyBlockMap The Story Block document as a Map.
     *
     * @return The same document, with every reference's data map reduced.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> keepOnlyNestedReferenceKeys(final Map<String, Object> storyBlockMap) {
        final Object contents = storyBlockMap.get(CONTENT_KEY);
        if (contents instanceof List) {
            for (final Object content : (List<Object>) contents) {
                if (!(content instanceof Map)) {
                    continue;
                }
                final Map<String, Object> contentMap = (Map<String, Object>) content;
                final Object attrs = contentMap.get(ATTRS_KEY);
                if (allowedTypes.contains(String.valueOf(contentMap.get(TYPE_KEY))) && attrs instanceof Map
                        && null != ((Map<String, Object>) attrs).get(DATA_KEY)) {
                    final Object data = ((Map<String, Object>) attrs).get(DATA_KEY);
                    final Map<String, Object> reduced = new LinkedHashMap<>();
                    if (data instanceof Map) {
                        reduced.putAll((Map<String, Object>) data);
                        this.keepOnlyReferenceKeys(reduced);
                    }
                    ((Map<String, Object>) attrs).put(DATA_KEY, reduced);
                }
                this.keepOnlyNestedReferenceKeys(contentMap);
            }
        }
        return storyBlockMap;
    }

    /**
     * Resolves the user on whose behalf Story Block references are read when the caller did not
     * provide one: the user logged in the current request, or the anonymous user when there is no
     * request or no logged-in user.
     *
     * @param request The current {@link HttpServletRequest}, possibly {@code null}.
     *
     * @return The requesting user, never {@code null}.
     */
    private User resolveRequestUser(final HttpServletRequest request) {
        final User loggedInUser = null != request ? WebAPILocator.getUserWebAPI().getLoggedInUser(request) : null;
        return null != loggedInUser ? loggedInUser : APILocator.getUserAPI().getAnonymousUserNoThrow();
    }

    /**
     * Returns the current level of related content being handled by the API. At the very beginning,
     * the initial depth value is determined by the {@link #MAX_RELATIONSHIP_DEPTH} variable. After
     * that, it represents the depth level of potential Block Editor fields that might have been
     * added to a parent Block Editor.
     * <p>The value of the {@link #DEFAULT_MAX_RECURSION_LEVEL} will determine the maximum number
     * of levels of related/associated content that will be processed. DO NOT increase this value
     * without taking into consideration the potential consequences in terms of performance.</p>
     *
     * @param request The current instance of the {@link HttpServletRequest} object.
     *
     * @return The current depth level.
     */
    private int getCurrentDepthValue(final HttpServletRequest request) {
        return null != request && null != request.getAttribute(CURRENT_DEPTH_ATTR)
                ? (Integer) request.getAttribute(CURRENT_DEPTH_ATTR)
                : Integer.parseInt(MAX_RELATIONSHIP_DEPTH.get());
    }

    @Override
    public StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue,
                                                                      final String parentContentletIdentifier) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        return this.refreshStoryBlockValueReferences(storyBlockValue, parentContentletIdentifier,
                this.resolveRequestUser(request), PageMode.get(request).respectAnonPerms);
    }

    /**
     * Refreshes the Contentlet references of a Story Block value on behalf of the given user.
     * See {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String, User, boolean)}.
     *
     * @param storyBlockValue            The actual value of the Story Block field.
     * @param parentContentletIdentifier String identifier of the parent contentlet.
     * @param user                       The user on whose behalf the referenced Contentlets are read.
     * @param respectFrontendRoles       If {@code true}, front-end roles are taken into account.
     *
     * @return An instance of {@link StoryBlockReferenceResult} with the result of the refresh process.
     */
    @RequestCost(Price.BLOCK_EDITOR_HYDRATION)
    @CloseDBIfOpened
    @Override
    @SuppressWarnings("unchecked")
    public StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue,
                                                                      final String parentContentletIdentifier,
                                                                      final User user,
                                                                      final boolean respectFrontendRoles) {
        return this.processStoryBlockValue(storyBlockValue, parentContentletIdentifier, user,
                respectFrontendRoles, true);
    }

    /**
     * Processes the Contentlet references of a Story Block value for the given user. See
     * {@link #processReference(Map, String, User, boolean, boolean)} for what each reference
     * becomes.
     *
     * @param refresh If {@code true}, references are refreshed; otherwise only checked for READ.
     *
     * @return The result; its value is the processed document as JSON when anything changed.
     */
    @SuppressWarnings("unchecked")
    private StoryBlockReferenceResult processStoryBlockValue(final Object storyBlockValue,
                                                             final String parentContentletIdentifier,
                                                             final User user,
                                                             final boolean respectFrontendRoles,
                                                             final boolean refresh) {
        boolean refreshed;
        if (null != storyBlockValue && (storyBlockValue instanceof Map || isJsonObject(storyBlockValue.toString()))) {
            try {
                // A Map value is processed on a deep copy, never on the given instance
                final LinkedHashMap<String, Object> blockEditorMap = this.toMap(storyBlockValue instanceof Map
                        ? this.toJson((Map<String, Object>) storyBlockValue) : storyBlockValue);
                final Object contentsMap = blockEditorMap.get(CONTENT_KEY);
                if (!UtilMethods.isSet(contentsMap) || !(contentsMap instanceof List)) {
                    return new StoryBlockReferenceResult(true, storyBlockValue);
                }
                refreshed = this.processReferences(parentContentletIdentifier, (List<Map<String, Object>>) contentsMap,
                        user, respectFrontendRoles, refresh);
                if (refreshed) {
                    return new StoryBlockReferenceResult(true, this.toJson(blockEditorMap));
                }
            } catch (final Exception e) {
                final String errorMsg = String.format("An error occurred when refreshing Story Block Contentlet references in parent Content " +
                        "'%s': %s", parentContentletIdentifier, ExceptionUtil.getErrorMessage(e));
                Logger.warnAndDebug(StoryBlockAPIImpl.class, errorMsg, e);
                throw new DotRuntimeException(errorMsg, e);
            }
        }
        // Return the original value in case no data was refreshed
        return new StoryBlockReferenceResult(false, storyBlockValue);
    }

    @SuppressWarnings("unchecked")
    private boolean processReferences(final String parentContentletIdentifier,
                                      final List<Map<String, Object>> contentsMap,
                                      final User user, final boolean respectFrontendRoles,
                                      final boolean refresh) {

        boolean refreshed = false;
        for (final Map<String, Object> contentMap : contentsMap) {
            if (UtilMethods.isSet(contentMap)) {
                // Isolate per-block failures so that one bad nested reference
                // does not prevent the rest of the Story Block from refreshing.
                try {
                    if (allowedTypes.contains(String.valueOf(contentMap.get(TYPE_KEY)))) {
                        refreshed |= this.processReference(contentMap, parentContentletIdentifier,
                                user, respectFrontendRoles, refresh);
                    } else {
                        final Object nestedContent = contentMap.get(CONTENT_KEY);
                        if (nestedContent instanceof List) {
                            refreshed |= this.processReferences(parentContentletIdentifier,
                                    (List<Map<String, Object>>) nestedContent, user, respectFrontendRoles, refresh);
                        }
                    }
                } catch (final Exception e) {
                    // The reference was already reduced before anything was looked up, so the
                    // document is written back with it reduced rather than returned as stored
                    refreshed = true;
                    Logger.warnAndDebug(StoryBlockAPIImpl.class, String.format(
                            "Skipping Story Block child while refreshing parent '%s': %s",
                            parentContentletIdentifier, ExceptionUtil.getErrorMessage(e)), e);
                }
            }
        }
        return refreshed;
    }

    /**
     * Processes one Contentlet reference of a Story Block for the given user. Its data map is first
     * reduced to the keys that identify the referenced Contentlet ({@code identifier} and
     * {@code languageId}), before anything is looked up, and is only filled again when:
     * <ul>
     *     <li>{@code refresh} is {@code true} and the Contentlet selected by the page mode is read
     *     and refreshed for the user, or</li>
     *     <li>{@code refresh} is {@code false}, or the reference points to the Contentlet holding the
     *     Story Block, and the user has READ permission on that Contentlet. The data stored with
     *     the reference is then kept, and any content embedded in it is kept only if the user can
     *     read it too.</li>
     * </ul>
     * A reference without identifier and a reference with nothing to read (no version for the
     * page mode) keep only the keys. A data value that is not a JSON object is replaced by an
     * empty map.
     *
     * @param contentMap The content node of the reference, modified in place.
     *
     * @return {@code true} if the node holds a reference, so the document must be written back.
     */
    @SuppressWarnings("unchecked")
    private boolean processReference(final Map<String, Object> contentMap, final String parentContentletIdentifier,
                                     final User user, final boolean respectFrontendRoles, final boolean refresh)
            throws DotDataException {
        final Object attrs = contentMap.get(ATTRS_KEY);
        if (!(attrs instanceof Map)) {
            return false;
        }
        final Map<String, Object> attrsMap = (Map<String, Object>) attrs;
        final Object dataValue = attrsMap.get(DATA_KEY);
        if (!UtilMethods.isSet(dataValue)) {
            return false;
        }

        final Map<String, Object> storedData;
        if (dataValue instanceof Map) {
            storedData = new LinkedHashMap<>((Map<String, Object>) dataValue);
        } else if (dataValue instanceof String && isJsonObject((String) dataValue)) {
            try {
                storedData = this.toMap(dataValue);
            } catch (final JsonProcessingException e) {
                Logger.warnAndDebug(this.getClass(), String.format(
                        "Failed to parse data field as JSON for parent contentlet '%s': %s",
                        parentContentletIdentifier, ExceptionUtil.getErrorMessage(e)), e);
                attrsMap.put(DATA_KEY, new LinkedHashMap<>());
                return true;
            }
        } else {
            Logger.warn(this.getClass(), String.format(
                    "Unexpected data value of type '%s' in a Story Block of parent contentlet '%s'",
                    dataValue.getClass().getName(), parentContentletIdentifier));
            attrsMap.put(DATA_KEY, new LinkedHashMap<>());
            return true;
        }

        // Until the reference is read or checked for this user, it only carries the keys that
        // identify it; any failure from here on leaves it like that
        final Map<String, Object> referenceOnly = new LinkedHashMap<>(storedData);
        this.keepOnlyReferenceKeys(referenceOnly);
        attrsMap.put(DATA_KEY, referenceOnly);

        final Object identifier = storedData.get(IDENTIFIER_KEY);
        if (!(identifier instanceof String) || !UtilMethods.isSet((String) identifier)) {
            return true;
        }
        // A Story Block referencing its own Contentlet is not refreshed (it would recurse), but the
        // version referenced may be in another language, so READ is still checked
        final boolean selfReference = identifier.equals(parentContentletIdentifier);
        final long languageId = ConversionUtils.toLong(storedData.get(LANGUAGE_ID_KEY),
                () -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final Optional<ContentletVersionInfo> versionInfo =
                APILocator.getVersionableAPI().getContentletVersionInfo((String) identifier, languageId);
        final String chosenInode = versionInfo.map(this::getInodeBasedOnPageMode).orElse(null);
        if (!UtilMethods.isSet(chosenInode)) {
            return true;
        }

        if (refresh && !selfReference) {
            final Map<String, Object> refreshedData = new LinkedHashMap<>(storedData);
            if (this.refreshBlockEditorDataMap(refreshedData, chosenInode, user, respectFrontendRoles)) {
                attrsMap.put(DATA_KEY, refreshedData);
            }
        } else if (this.canRead(chosenInode, user, respectFrontendRoles)) {
            attrsMap.put(DATA_KEY, this.withReadableEmbeddedContent(storedData, (String) identifier,
                    user, respectFrontendRoles));
        }
        return true;
    }

    /**
     * Returns a copy of a value (Map, List, JSON String or scalar) in which every Story Block
     * document found at any level has its references reduced to the keys that identify them.
     */
    @SuppressWarnings("unchecked")
    private Object reduceStoryBlocksAtAnyLevel(final Object value) {
        if (value instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) value;
            if (this.isStoryBlockMap(map)) {
                return this.reduceEmbeddedContent(map, false);
            }
            final Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(key, this.reduceStoryBlocksAtAnyLevel(nested)));
            return copy;
        }
        if (value instanceof List) {
            final List<Object> copy = new ArrayList<>();
            ((List<Object>) value).forEach(item -> copy.add(this.reduceStoryBlocksAtAnyLevel(item)));
            return copy;
        }
        return value instanceof String ? this.reduceEmbeddedContent(value, false) : value;
    }

    /**
     * Tells whether the user has READ permission on the Contentlet with the given inode, without
     * refreshing anything from it.
     */
    private boolean canRead(final String inode, final User user, final boolean respectFrontendRoles) {
        try {
            final Contentlet stored = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false, true);
            return null != stored && APILocator.getPermissionAPI()
                    .doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles);
        } catch (final DotSecurityException e) {
            Logger.debug(this, () -> String.format("Could not check READ on contentlet with Inode '%s': %s",
                    inode, ExceptionUtil.getErrorMessage(e)));
            return false;
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when checking READ on contentlet with Inode " +
                    "'%s'; its Story Block data was omitted: %s", inode, ExceptionUtil.getErrorMessage(e)), e);
            return false;
        }
    }

    /**
     * Returns a copy of the data stored with a reference the user can read, in which the content
     * embedded in it is kept only where the user can read it too: references in Story Block
     * documents are processed in check-only mode, and related Contentlets the user cannot read keep
     * only the keys that identify them.
     *
     * @param storedData      The data stored with the reference.
     * @param ownerIdentifier The identifier of the referenced Contentlet.
     */
    private Map<String, Object> withReadableEmbeddedContent(final Map<String, Object> storedData,
                                                            final String ownerIdentifier, final User user,
                                                            final boolean respectFrontendRoles) {
        final Map<String, Object> data = new LinkedHashMap<>();
        storedData.forEach((key, value) -> data.put(key, REFERENCE_KEYS.contains(key) ? value
                : this.keepReadableEmbeddedContent(value, ownerIdentifier, user, respectFrontendRoles)));
        return data;
    }

    /**
     * Returns a copy of a stored value keeping only the embedded content the user can read. See
     * {@link #withReadableEmbeddedContent(Map, String, User, boolean)}. If the value cannot be
     * read, parsed, or a nested Story Block cannot be processed, the embedded content is reduced
     * instead; any other error is left to the caller, which reduces the whole reference and logs it.
     */
    @SuppressWarnings("unchecked")
    private Object keepReadableEmbeddedContent(final Object value, final String ownerIdentifier,
                                               final User user, final boolean respectFrontendRoles) {
        try {
            if (value instanceof Map) {
                final Map<String, Object> map = (Map<String, Object>) value;
                if (this.isStoryBlockMap(map)) {
                    return this.toMap(this.processStoryBlockValue(this.toJson(map), ownerIdentifier,
                            user, respectFrontendRoles, false).getValue());
                }
                if (map.containsKey(IDENTIFIER_KEY)) {
                    return this.keepReadableRelatedContent(List.of(map), user, respectFrontendRoles).get(0);
                }
                return value;
            }
            if (value instanceof String && isJsonObject((String) value)) {
                return this.isStoryBlockMap(this.toMap(value))
                        ? this.processStoryBlockValue(value, ownerIdentifier, user, respectFrontendRoles, false)
                                .getValue()
                        : value;
            }
            if (value instanceof Contentlet) {
                return this.keepReadableRelatedContent(List.of(value), user, respectFrontendRoles).get(0);
            }
            if (value instanceof List) {
                final List<Object> items = (List<Object>) value;
                final boolean relatedContent = items.stream().anyMatch(item -> item instanceof Contentlet
                        || (item instanceof Map && ((Map<String, Object>) item).containsKey(IDENTIFIER_KEY)));
                if (relatedContent) {
                    return this.keepReadableRelatedContent(items, user, respectFrontendRoles);
                }
                final List<Object> kept = new ArrayList<>();
                items.forEach(item -> kept.add(this.keepReadableEmbeddedContent(item, ownerIdentifier,
                        user, respectFrontendRoles)));
                return kept;
            }
            return value;
        } catch (final DotSecurityException e) {
            Logger.debug(this, () -> "Embedded content could not be checked; it is reduced: "
                    + ExceptionUtil.getErrorMessage(e));
            return this.reduceEmbeddedContent(value, true);
        } catch (final DotDataException | DotRuntimeException | JsonProcessingException e) {
            Logger.warn(this, String.format("Embedded content of contentlet '%s' could not be checked; " +
                    "it is reduced: %s", ownerIdentifier, ExceptionUtil.getErrorMessage(e)), e);
            return this.reduceEmbeddedContent(value, true);
        }
    }

    /**
     * Returns a copy of a list of related content (Maps with an identifier, or Contentlets) where
     * the items the user cannot read keep only the keys that identify them. READ is checked with a
     * single permission call for the whole list. Items the user can read keep their stored values,
     * with their own embedded content checked the same way.
     */
    @SuppressWarnings("unchecked")
    private List<Object> keepReadableRelatedContent(final List<Object> items, final User user,
                                                    final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        final List<Contentlet> candidates = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof Contentlet) {
                candidates.add((Contentlet) item);
            } else if (item instanceof Map && ((Map<String, Object>) item).get(IDENTIFIER_KEY) instanceof String) {
                final Map<String, Object> map = (Map<String, Object>) item;
                final long languageId = ConversionUtils.toLong(map.get(LANGUAGE_ID_KEY),
                        () -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
                final String inode = APILocator.getVersionableAPI()
                        .getContentletVersionInfo((String) map.get(IDENTIFIER_KEY), languageId)
                        .map(this::getInodeBasedOnPageMode).orElse(null);
                final Contentlet stored = UtilMethods.isSet(inode)
                        ? APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false, true) : null;
                if (null != stored) {
                    candidates.add(stored);
                }
            }
        }
        final Set<String> readable = new HashSet<>();
        if (!candidates.isEmpty()) {
            APILocator.getPermissionAPI()
                    .filterCollection(candidates, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user)
                    .forEach(contentlet -> readable.add(contentlet.getIdentifier()));
        }

        final List<Object> kept = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof Contentlet) {
                final Contentlet contentlet = (Contentlet) item;
                kept.add(readable.contains(contentlet.getIdentifier()) ? contentlet
                        : this.reduceEmbeddedContent(contentlet, true));
            } else if (item instanceof Map && ((Map<String, Object>) item).containsKey(IDENTIFIER_KEY)) {
                final Map<String, Object> map = (Map<String, Object>) item;
                final String identifier = String.valueOf(map.get(IDENTIFIER_KEY));
                kept.add(readable.contains(identifier)
                        ? this.withReadableEmbeddedContent(map, identifier, user, respectFrontendRoles)
                        : this.reduceEmbeddedContent(map, true));
            } else {
                kept.add(item);
            }
        }
        return kept;
    }

    /**
     * Reduces the content embedded in a stored value: every reference in a Story Block document
     * (as a Map or as JSON), and, when {@code includeRelatedContent} is {@code true}, every related
     * contentlet (a Map with an identifier, or a list of them) to the keys that identify it. Other
     * values are returned as they are. Never modifies the given value.
     */
    @SuppressWarnings("unchecked")
    private Object reduceEmbeddedContent(final Object value, final boolean includeRelatedContent) {
        try {
            if (value instanceof Map) {
                final Map<String, Object> map = (Map<String, Object>) value;
                if (this.isStoryBlockMap(map)) {
                    return this.keepOnlyNestedReferenceKeys(this.toMap(this.toJson(map)));
                }
                if (includeRelatedContent && map.containsKey(IDENTIFIER_KEY)) {
                    final Map<String, Object> reference = new LinkedHashMap<>(map);
                    this.keepOnlyReferenceKeys(reference);
                    return reference;
                }
                return value;
            }
            if (value instanceof String && isJsonObject((String) value)) {
                final Map<String, Object> map = this.toMap(value);
                return this.isStoryBlockMap(map) ? this.toJson(this.keepOnlyNestedReferenceKeys(map)) : value;
            }
            if (includeRelatedContent && value instanceof Contentlet) {
                return Map.of(IDENTIFIER_KEY, ((Contentlet) value).getIdentifier(),
                        LANGUAGE_ID_KEY, ((Contentlet) value).getLanguageId());
            }
            if (includeRelatedContent && value instanceof List) {
                final List<Object> reduced = new ArrayList<>();
                for (final Object item : (List<Object>) value) {
                    reduced.add(this.reduceEmbeddedContent(item, true));
                }
                return reduced;
            }
        } catch (final JsonProcessingException e) {
            Logger.debug(this, () -> "Embedded Story Block could not be reduced: " + ExceptionUtil.getErrorMessage(e));
            // Only Story Block documents reach this point: none of their content is returned
            return new LinkedHashMap<>();
        }
        return value;
    }

    /**
     * Returns the Inode of the Contentlet based on the current Page Mode. If the selected
     * {@link PageMode} present in the current HTTP Request is {@link PageMode#LIVE} or
     * {@link PageMode#ADMIN_MODE}, then the live Inode will be returned. Otherwise, the working
     * inode is returned.
     *
     * @param contentletVersionInfo The {@link ContentletVersionInfo} object containing the
     *                              Contentlet's Inodes.
     *
     * @return The Inode of the Contentlet based on the current Page Mode.
     */
    private String getInodeBasedOnPageMode(final ContentletVersionInfo contentletVersionInfo) {
        final PageMode currentPageMode = PageMode.get(HttpServletRequestThreadLocal.INSTANCE.getRequest());
        return currentPageMode == PageMode.LIVE || currentPageMode == PageMode.ADMIN_MODE
                ? contentletVersionInfo.getLiveInode()
                : contentletVersionInfo.getWorkingInode();
    }

    @CloseDBIfOpened
    @Override
    public List<StoryBlockDependency> getDependencies(final Contentlet contentlet) {
        final ImmutableList.Builder<StoryBlockDependency> dependencyList = new ImmutableList.Builder<>();
        contentlet.getContentType().fields(StoryBlockField.class).forEach(field ->

            dependencyList.addAll(this.getDependencies(contentlet.get(field.variable())))

        );
        return dependencyList.build();
    }

    @SuppressWarnings("unchecked")
    @CloseDBIfOpened
    @Override
    public List<StoryBlockDependency> getDependencies(final Object storyBlockValue) {
        final ImmutableList.Builder<StoryBlockDependency> dependencyList = new ImmutableList.Builder<>();

        try {

            if (null != storyBlockValue && isJsonObject(storyBlockValue.toString())) {
                final Map<String, Object> blockEditorMap = this.toMap(storyBlockValue);
                Object contentsMap = blockEditorMap.getOrDefault(CONTENT_KEY, List.of());
                if(!(contentsMap instanceof List)) {
                    return List.of();
                }

                // Recursively process all blocks, including nested ones
                processBlocksRecursively(dependencyList, (List<Map<String, Object>>) contentsMap);
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when retrieving Contentlet references from Story Block field: " +
                    "%s", ExceptionUtil.getErrorMessage(e));
            Logger.warnAndDebug(StoryBlockAPIImpl.class, errorMsg,e);

        }
        return dependencyList.build();
    }

    /**
     * Recursively processes blocks to find all dependencies, including those nested
     * within container blocks like lists, paragraphs, blockquotes, tables, etc.
     * This method traverses the entire block tree structure, ensuring that dependencies
     * (dotImage, dotContent, dotVideo) are found regardless of their nesting level.
     *
     * @param dependencyList The builder to collect contentlet dependencies
     * @param blocks         The list of blocks to process
     */
    private void processBlocksRecursively(final ImmutableList.Builder<StoryBlockDependency> dependencyList,
                                          final List<Map<String, Object>> blocks) {
        if (!UtilMethods.isSet(blocks)) {
            return;
        }

        for (final Map<String, Object> block : blocks) {
            if (!UtilMethods.isSet(block)) {
                continue;
            }

            final String type = (String) block.get(TYPE_KEY);

            if (type != null && allowedTypes.contains(type)) {
                addDependencies(dependencyList, block);
                continue;
            }

            // If this block has nested content, recurse into it, commonly listItem, bulletList, orderedList
            final Object nestedContent = block.get(CONTENT_KEY);
            if (nestedContent instanceof List) {
                processBlocksRecursively(dependencyList, (List<Map<String, Object>>) nestedContent);
            }
        }
    }

    @Override
    public Object addContentlet(final Object storyBlockValue, final Contentlet contentlet) {
        try {
            final Map<String, Object> storyBlockValueMap = this.toMap(storyBlockValue);
            this.addContentlet(storyBlockValueMap, contentlet);
            return this.toJson(storyBlockValueMap);
        } catch (final JsonProcessingException e) {
            final String errorMsg = String.format("An error occurred when adding Contentlet '%s' to the Story Block field: " +
                    "%s", contentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e));
            Logger.warnAndDebug(StoryBlockAPIImpl.class, errorMsg, e);
            return storyBlockValue;
        }
        
    }

    /**
     * Adds the specified {@link Contentlet} object to the Map representation of the Story Block field.
     *
     * @param storyBlockValueMap The Story Block field as a Map.
     * @param contentlet         The Contentlet being added to it.
     *
     * @return The Story Block field as a Map with the Contentlet in it.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> addContentlet(final Map<String, Object> storyBlockValueMap, final Contentlet contentlet) {
        if (storyBlockValueMap.containsKey(StoryBlockAPI.CONTENT_KEY)) {
            final List<Map<String, Object>> contentList = (List<Map<String, Object>>) storyBlockValueMap.get(StoryBlockAPI.CONTENT_KEY);
            final Map<String, Object> dataMap = new LinkedHashMap<>();
            final List<Field> fields = contentlet.getContentType().fields();
            this.loadCommonContentletProps(contentlet, dataMap);
            for (final Field field : fields) {
                dataMap.putIfAbsent(field.variable(), contentlet.get(field.variable()));
            }
            final Map<String, Map<String, Object>> attrsMap = new LinkedHashMap<>();
            attrsMap.put(StoryBlockAPI.DATA_KEY, dataMap);
            final Map<String, Object> contentMap = new LinkedHashMap<>();
            contentMap.put(StoryBlockAPI.ATTRS_KEY, attrsMap);
            contentMap.put(StoryBlockAPI.TYPE_KEY, "dotContent");
            contentList.add(contentMap);
        }
        return storyBlockValueMap;
    }

    /**
     * Copies the common properties of a Contentlet (title, inode, state flags, categories, tags,
     * file links...) into the data map of a Story Block content node.
     *
     * @param contentlet The Contentlet whose properties are copied.
     * @param dataMap    The data map of the content node.
     */
    private void loadCommonContentletProps(final Contentlet contentlet, final Map<String, Object> dataMap) {
        dataMap.put(Contentlet.HOST_NAME, contentlet.getHost());
        dataMap.put(Contentlet.MOD_DATE_KEY, contentlet.getModDate());
        dataMap.put(Contentlet.TITTLE_KEY, contentlet.getTitle());
        dataMap.put(Contentlet.CONTENT_TYPE_ICON, contentlet.getContentType().icon());
        dataMap.put(Contentlet.BASE_TYPE_KEY, contentlet.getContentType().baseType().getAlternateName());
        dataMap.put(Contentlet.INODE_KEY, contentlet.getInode());
        dataMap.put(Contentlet.ARCHIVED_KEY, Try.of(contentlet::isArchived).getOrElse(false));
        dataMap.put(Contentlet.WORKING_KEY, Try.of(contentlet::isWorking).getOrElse(false));
        dataMap.put(Contentlet.LOCKED_KEY, Try.of(contentlet::isLocked).getOrElse(false));
        dataMap.put(Contentlet.STRUCTURE_INODE_KEY,  contentlet.getContentType().inode());
        dataMap.put(Contentlet.CONTENT_TYPE_KEY,  contentlet.getContentType().variable());
        dataMap.put(Contentlet.LIVE_KEY, Try.of(contentlet::isLive).getOrElse(false));
        dataMap.put(Contentlet.OWNER_KEY, contentlet.getOwner());
        dataMap.put(Contentlet.IDENTIFIER_KEY, contentlet.getIdentifier());
        dataMap.put(Contentlet.LANGUAGEID_KEY, contentlet.getLanguageId());
        dataMap.put(Contentlet.HAS_LIVE_VERSION, Try.of(contentlet::hasLiveVersion).getOrElse(false));
        dataMap.put(Contentlet.FOLDER_KEY, contentlet.getFolder());
        dataMap.put(Contentlet.SORT_ORDER_KEY, contentlet.getSortOrder());
        dataMap.put(Contentlet.MOD_USER_KEY, contentlet.getModUser());
        contentlet.getTitleImage().ifPresentOrElse(field -> {
            dataMap.put(Contentlet.HAS_TITLE_IMAGE_KEY, true);
            dataMap.put(Contentlet.TITLE_IMAGE_KEY, field.variable());
        }, () -> {
            dataMap.put(Contentlet.HAS_TITLE_IMAGE_KEY, false);
            dataMap.put(Contentlet.TITLE_IMAGE_KEY, Contentlet.TITLE_IMAGE_NOT_FOUND);
        });
        this.loadCategoryFields(contentlet, dataMap);
        this.loadTagFields(contentlet, dataMap);
        //Transform fileAssets into url
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if(null != request) {
            contentlet.getContentType().fields(BinaryField.class).forEach(field ->
                    getFileLink(contentlet, field, request).ifPresent(
                            fileLink -> dataMap.put(field.variable(), fileLink))
            );
        }
    }

    private void loadTagFields(final Contentlet contentlet, final Map<String, Object> dataMap) {
        final List<Field> tagFields = contentlet.getContentType().fields(TagField.class);
        if (tagFields.isEmpty()) {
            return;
        }

        try {
            contentlet.setTags();
            for (final Field tagField : tagFields) {
                final Object value = contentlet.get(tagField.variable());
                if (null != value) {
                    dataMap.putIfAbsent(tagField.variable(), value);
                }
            }
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("An error occurred when loading Tags for Contentlet with ID '%s': %s",
                    contentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e)));
        }
    }

    private void loadCategoryFields(final Contentlet contentlet, final Map<String, Object> dataMap) {
        final List<Field> categoryFields = contentlet.getContentType().fields(CategoryField.class);
        if (categoryFields.isEmpty()) {
            return;
        }

        final User user = APILocator.systemUser();
        try {
            final var categoryAPI = APILocator.getCategoryAPI();
            final List<Category> categories = categoryAPI.getParents(contentlet, user, true);
            if (categories == null) {
                for (final Field categoryField : categoryFields) {
                    dataMap.put(categoryField.variable(), Map.of("categories", Collections.emptyList()));
                }
                return;
            }
            for (final Field categoryField : categoryFields) {
                final List<Map<String, Object>> childCategories = new ArrayList<>();
                final Category parentCategory = categoryAPI.find(categoryField.values(), user, true);
                if (parentCategory != null) {
                    for (final Category category : categories) {
                        if (categoryAPI.isParent(category, parentCategory, user, true)) {
                            childCategories.add(this.toCategoryMap(category));
                        }
                    }
                }
                dataMap.put(categoryField.variable(), Map.of("categories", childCategories));
            }
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(this, String.format("An error occurred when loading Categories for Contentlet with ID '%s': %s",
                    contentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e)));
        }
    }

    private Map<String, Object> toCategoryMap(final Category category) {
        final Map<String, Object> categoryMap = new LinkedHashMap<>();
        categoryMap.put("inode", category.getInode());
        categoryMap.put("active", category.isActive());
        categoryMap.put("name", category.getCategoryName());
        categoryMap.put("key", category.getKey());
        categoryMap.put("keywords", category.getKeywords());
        categoryMap.put("velocityVar", category.getCategoryVelocityVarName());
        return categoryMap;
    }

    /**
     * Extracts dependency information (identifier and languageId) from the specified data map in order to list the
     * referenced Contentlets in a Story Block field.
     *
     * @param dependencyList The list of Contentlet dependencies referenced by the Story Block field.
     * @param contentMap     The Story Block data map.
     */
    @SuppressWarnings("unchecked")
    private static void addDependencies(final ImmutableList.Builder<StoryBlockDependency> dependencyList,
                                        final Map contentMap) {
        final Map<String, Map<String, Object>> attrsMap = (Map) contentMap.get(ATTRS_KEY);
        if (UtilMethods.isSet(attrsMap)) {
            final Map<String, Object> dataMap = attrsMap.get(DATA_KEY);
            if (UtilMethods.isSet(dataMap)) {
                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                // Extract languageId from the stored data, falling back to default language if not present
                final long languageId = ConversionUtils.toLong(
                        dataMap.get(LANGUAGE_ID_KEY),
                        () -> APILocator.getLanguageAPI().getDefaultLanguage().getId()
                );
                dependencyList.add(StoryBlockDependency.of(identifier, languageId));
            }
        }
    }

    /**
     * Returns {@code true} when the supplied String is valid JSON whose root
     * token is an object. Story Block documents are always JSON objects, so
     * scalar JSON tokens (numbers, strings, booleans) and arrays must be
     * rejected here — otherwise {@link #toMap(Object)} fails to deserialize
     * them into a {@link LinkedHashMap} and the entire transformer pipeline
     * aborts (see issue surfaced via /api/content/_search).
     */
    private static boolean isJsonObject(final String value) {
        if (value == null) {
            return false;
        }
        final String trimmed = value.trim();
        return trimmed.startsWith("{") && JsonUtil.isValidJSON(trimmed);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LinkedHashMap<String, Object> toMap(final Object blockEditorValue) throws JsonProcessingException {
        if (blockEditorValue instanceof Map) {
            // Already hydrated into a Map -- e.g. by StoryBlockViewStrategy during page rendering.
            // Map.toString() produces Java map notation, not JSON, so it must never be re-parsed.
            // NOTE: this is a SHALLOW copy -- nested content lists/maps are shared with the
            // caller's value. Callers must not mutate nested structures of a Map-sourced result.
            return new LinkedHashMap<>((Map<String, Object>) blockEditorValue);
        }
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .readValue(Try.of(blockEditorValue::toString)
                                          .getOrElse(StringPool.BLANK), LinkedHashMap.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object toMapOrPassthrough(final Object blockEditorValue, final Supplier<String> valueContext) {
        if (blockEditorValue instanceof Map) {
            // Same shallow-copy semantics as toMap(Object).
            return new LinkedHashMap<>((Map<String, Object>) blockEditorValue);
        }
        if (null != blockEditorValue) {
            try {
                if (isJsonObject(blockEditorValue.toString())) {
                    return this.toMap(blockEditorValue);
                }
            } catch (final Exception e) {
                Logger.debug(this, () -> String.format("Unable to parse Story Block value (%s): %s",
                        valueContext.get(), ExceptionUtil.getErrorMessage(e)));
            }
            final String context = valueContext.get();
            Logger.warnEvery(StoryBlockAPIImpl.class, context, String.format(
                    "Unable to parse Story Block value as JSON (%s); returning the stored value unchanged",
                    context), UNPARSEABLE_VALUE_WARN_INTERVAL_MILLIS);
        }
        return blockEditorValue;
    }

    @Override
    public String toJson(final Map<String, Object> blockEditorMap) throws JsonProcessingException {
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .writeValueAsString(blockEditorMap);
    }

    /**
     * Refreshes the data map from a referenced Contentlet in the Story Block field with the data of
     * the version selected by the page mode.
     *
     * @param dataMap              The Map containing the Contentlet's properties.
     * @param inode                The Inode selected by the page mode (live or working) of the
     *                             Contentlet whose properties will be copied over to the previous
     *                             data map.
     * @param user                 The user on whose behalf the Contentlet is read.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account when
     *                             reading the Contentlet.
     *
     * @return {@code true} if the data map was refreshed with the Contentlet read for the user;
     * {@code false} if it could not be (no READ permission, nothing to read, a circular
     * reference, an error), in which case the caller must not use the data map.
     */
    private boolean refreshBlockEditorDataMap(final Map<String, Object> dataMap, final String inode,
                                              final User user, final boolean respectFrontendRoles) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        // If 'true', it means that the parent Block Editor is being processed, and not its
        // potential child contents
        final boolean isCurrentDepthEmpty = request.getAttribute(CURRENT_DEPTH_ATTR) == null;
        boolean hydrated = false;
        try {
            // If the current depth parameter is set, then it must be decreased in order to
            // account for the number of levels that will be processed for related contents,
            // including both associated Block Editor fields and Relationship fields
            final Integer currentDepth = isCurrentDepthEmpty ? this.getInitialDepthValue() :
                    this.decreaseDepthValue((Integer) request.getAttribute(CURRENT_DEPTH_ATTR));

            request.setAttribute(CURRENT_DEPTH_ATTR, currentDepth);

            // In this API, Block Editor fields must NEVER be automatically hydrated in order to
            // prevent infinite loops. Their specific hydration will be handled manually in
            // subsequent methods in this class
            final Contentlet fattyContentlet = APILocator.getContentletAPI().find(inode, user, respectFrontendRoles, true);

            if (null != fattyContentlet) {
                final String fattyIdentifier = fattyContentlet.getIdentifier();
                final Set<String> processing = REFRESH_PROCESSING_IDENTIFIERS.get();
                // Set.add() returns false when the identifier is already present, meaning a
                // circular reference chain (e.g. A → B → A) was detected that would cause a
                // StackOverflowError if left unchecked.
                if (!processing.add(fattyIdentifier)) {
                    Logger.warn(this, String.format(
                            "Circular Story Block reference detected for contentlet '%s'; " +
                            "skipping re-entrant refresh to prevent StackOverflowError.",
                            fattyIdentifier));
                    return false;
                }
                try {
                    // The Contentlet returned by the API may be the shared cached instance, so the
                    // related content of this user is added to a copy of it
                    final Contentlet referenced = new Contentlet(fattyContentlet);
                    this.addContentletRelationships(referenced, currentDepth, user);
                    final Map<String, Object> updatedDataMap = this.refreshContentlet(referenced, user,
                            respectFrontendRoles);
                    this.excludeNonExistingProperties(dataMap, updatedDataMap);
                    dataMap.putAll(updatedDataMap);
                    hydrated = true;
                } finally {
                    processing.remove(fattyIdentifier);
                }
            }
        } catch (final JsonProcessingException e) {
            Logger.error(this, String.format("An error occurred when transforming JSON data in contentlet with Inode " +
                    "'%s': %s", inode, ExceptionUtil.getErrorMessage(e)), e);
        } catch (final DotSecurityException e) {
            Logger.debug(this, () -> String.format("Contentlet with Inode '%s' is not readable by the requesting " +
                    "user; its Story Block data was omitted", inode));
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when retrieving contentlet with Inode " +
                    "'%s': %s", inode, ExceptionUtil.getErrorMessage(e)), e);
        } finally {
            if (isCurrentDepthEmpty) {
                request.removeAttribute(CURRENT_DEPTH_ATTR);
                // Remove the thread-local set when exiting the top-level call to
                // prevent memory leaks in servlet-container thread pools.
                REFRESH_PROCESSING_IDENTIFIERS.remove();
            }
        }
        return hydrated;
    }

    /**
     * Adds the relationships of the specified {@link Contentlet} object, if required.
     *
     * @param contentlet The Contentlet that may contain Relationship fields.
     * @param depth      The number of levels of related content to load.
     * @param user       The user on whose behalf the related Contentlets are read.
     */
    private void addContentletRelationships(final Contentlet contentlet, final int depth, final User user) {
        final HttpServletRequest httpRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final PageMode currentPageMode = PageMode.get(httpRequest);

        ContentUtils.addRelationships(contentlet, user, currentPageMode, contentlet.getLanguageId(), depth);
    }

    /**
     * Decreases the DEPTH value base on the following rule:
     * <ul>
     *     <li>If the current value is 2, reduce it to 0.</li>
     *     <li>If the current value is 3, reduce it to 1.</li>
     * </ul>
     * This calculation is extremely important as it's part of the approach that keeps the Block
     * Editor and/or Relationship fields from loading infinite levels of nested Contentlets.
     *
     * @param depthValue The current depth value
     *
     * @return The new depth value.
     */
    private int decreaseDepthValue(final int depthValue) {
        if (depthValue == 2) {
            return 0;
        }

        if (depthValue == 3) {
            return 1;
        }

        return depthValue;
    }

    /**
     * Checks the current {@link HttpServletRequest} object for the existence of the initial depth
     * value specified via the {@link WebKeys#HTMLPAGE_DEPTH} attribute. If it's not found, then the
     * default {@link #MAX_RELATIONSHIP_DEPTH} value is used. A value lower than 0 o greater than 3
     * is NOT valid, so it will fall back to 0.
     *
     * @return The initial depth value.
     */
    private int getInitialDepthValue() {
        final HttpServletRequest httpRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        String value;

        if (null != httpRequest && null != httpRequest.getAttribute(WebKeys.HTMLPAGE_DEPTH))  {
            value = (String) httpRequest.getAttribute(WebKeys.HTMLPAGE_DEPTH);
        } else {
            value = MAX_RELATIONSHIP_DEPTH.get();
        }

        int depth = ConversionUtils.toInt(value, 0);
        return depth < 0 || depth > 3 ? 0 : depth;
    }

    /**
     * Takes the Contentlet properties that exist in the Block Editor field that <b>no longer exist
     * in the latest version of the Contentlet.</b>
     * <p>For instance, if a Contentlet with a Checkbox Field is added to a Block Editor, and later
     * on such a field is unchecked, its value will still be present in the Block Editor's JSON
     * data even though it's NOT present in the latest {@link Contentlet} object. This makes sure
     * both data maps have the same properties.</p>
     *
     * @param dataMap        The Map with the Contentlet's properties from the Block Editor field.
     * @param updatedDataMap The Map with the Contentlet's properties from the latest version in the
     *                       database/cache.
     */
    private void excludeNonExistingProperties(final Map<String, Object> dataMap, final Map<String, Object> updatedDataMap) {
        if (dataMap.size() > updatedDataMap.size()) {
            final Set<String> additionalKeysInDataMap = new HashSet<>(dataMap.keySet());
            additionalKeysInDataMap.removeAll(updatedDataMap.keySet());
            dataMap.entrySet().removeIf(entry ->
                    additionalKeysInDataMap.contains(entry.getKey()));
        }
    }

    /**
     * Refreshes the Contentlet data using the Contentlet from the API call to the DB.
     *
     * @param contentlet           The {@link Contentlet} with the latest properties.
     * @param user                 The user on whose behalf nested references are resolved.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account when
     *                             resolving nested references.
     *
     * @return The updated data map with the latest properties.
     *
     * @throws JsonProcessingException An error occurred when processing property with JSON data.
     */
    private Map<String, Object> refreshContentlet(final Contentlet contentlet, final User user,
                                                  final boolean respectFrontendRoles)
            throws JsonProcessingException {
        final Map<String, Object> dataMap = new LinkedHashMap<>();
        final List<Field> fields = contentlet.getContentType().fields();
        this.loadCommonContentletProps(contentlet, dataMap);
        for (final Field field : fields) {
            final Object value = contentlet.get(field.variable());
            if (null != value) {
                // Isolate per-field failures so that one malformed field on a
                // nested contentlet does not abort hydration of the rest.
                try {
                    if (field instanceof StoryBlockField) {
                        // At this depth, if the Contentlet inside the Block Editor also has a Block
                        // Editor field, we'll return the raw JSON data of any potential Contentlets it
                        // is referencing. This will prevent infinite recursion problems.
                        // Prefer the _raw companion field when it contains valid JSON; otherwise fall
                        // back to the story block value itself. If neither is valid JSON (e.g. a test
                        // or misconfigured field whose default value is plain text), skip the field
                        // entirely so the rest of the data map is still populated correctly.
                        // The Contentlets referenced at this depth are not refreshed again, but the
                        // reader's READ permission still decides whether their stored data is kept.
                        final Object rawValue = contentlet.get(field.variable() + RAW_SUFFIX);
                        final String rawStr = rawValue != null ? rawValue.toString() : null;
                        if (rawStr != null && isJsonObject(rawStr)) {
                            dataMap.put(field.variable(), this.toMap(this.processStoryBlockValue(rawValue,
                                    contentlet.getIdentifier(), user, respectFrontendRoles, false).getValue()));
                        } else if (isJsonObject(value.toString())) {
                            dataMap.put(field.variable(), this.toMap(this.processStoryBlockValue(value,
                                    contentlet.getIdentifier(), user, respectFrontendRoles, false).getValue()));
                        }
                    } else {
                        dataMap.putIfAbsent(field.variable(),
                                this.refreshNestedStoryBlockValues(value, contentlet.getIdentifier(),
                                        MAX_NESTED_STORY_BLOCK_REFRESH_DEPTH, user, respectFrontendRoles));
                    }
                } catch (final Exception e) {
                    Logger.warnAndDebug(StoryBlockAPIImpl.class, String.format(
                            "Skipping field '%s' while hydrating contentlet '%s': %s",
                            field.variable(), contentlet.getIdentifier(),
                            ExceptionUtil.getErrorMessage(e)), e);
                    // Fall back to the raw value so the field still appears in the response.
                    dataMap.putIfAbsent(field.variable(), value);
                }
            }
        }
        return dataMap;
    }

    /**
     * Recursively traverses nested values coming from hydrated relationship payloads and refreshes
     * Story Block values found at any level.
     *
     * @param value The current value to inspect (Map, List, String, or scalar).
     * @param parentContentletIdentifier The parent contentlet identifier used to prevent self-refresh loops.
     * @param remainingDepth Remaining recursive traversal depth allowed.
     * @param user The user on whose behalf nested references are read.
     * @param respectFrontendRoles If {@code true}, front-end roles are taken into account when
     *                             reading nested references.
     *
     * @return A refreshed value preserving the same logical structure.
     *
     * @throws JsonProcessingException If Story Block JSON cannot be transformed while refreshing.
     */
    @SuppressWarnings("unchecked")
    private Object refreshNestedStoryBlockValues(final Object value, final String parentContentletIdentifier,
            final int remainingDepth, final User user, final boolean respectFrontendRoles)
            throws JsonProcessingException {
        if (remainingDepth <= 0) {
            // Values below this depth are not refreshed, so any Story Block found in them keeps
            // only the keys that identify its references
            return this.reduceStoryBlocksAtAnyLevel(value);
        }

        if (value instanceof Map) {
            final Map<String, Object> valueMap = (Map<String, Object>) value;
            if (this.isStoryBlockMap(valueMap)) {
                // refreshStoryBlockValueReferences currently processes JSON values, so Story Block maps
                // must be normalized to JSON before refresh and parsed back afterwards.
                final StoryBlockReferenceResult refreshedValue =
                        this.refreshStoryBlockValueReferences(this.toJson(valueMap), parentContentletIdentifier,
                                user, respectFrontendRoles);
                return refreshedValue.isRefreshed() ? this.toMap(refreshedValue.getValue()) : valueMap;
            }
            Map<String, Object> refreshedMap = null;
            for (final Map.Entry<String, Object> entry : valueMap.entrySet()) {
                final Object nestedValue = entry.getValue();
                final Object refreshedNestedValue = this.refreshNestedStoryBlockValues(nestedValue,
                        parentContentletIdentifier, remainingDepth - 1, user, respectFrontendRoles);

                if (refreshedMap != null) {
                    refreshedMap.put(entry.getKey(), refreshedNestedValue);
                } else if (refreshedNestedValue != nestedValue) {
                    refreshedMap = new LinkedHashMap<>(valueMap.size());
                    for (final Map.Entry<String, Object> existingEntry : valueMap.entrySet()) {
                        if (existingEntry.getKey().equals(entry.getKey())) {
                            break;
                        }
                        refreshedMap.put(existingEntry.getKey(), existingEntry.getValue());
                    }
                    refreshedMap.put(entry.getKey(), refreshedNestedValue);
                }
            }

            return refreshedMap != null ? refreshedMap : valueMap;
        }

        if (value instanceof List) {
            final List<Object> valueList = (List<Object>) value;
            List<Object> refreshedList = null;
            for (int i = 0; i < valueList.size(); i++) {
                final Object item = valueList.get(i);
                final Object refreshedItem = this.refreshNestedStoryBlockValues(item, parentContentletIdentifier,
                        remainingDepth - 1, user, respectFrontendRoles);

                if (refreshedList != null) {
                    refreshedList.add(refreshedItem);
                } else if (refreshedItem != item) {
                    refreshedList = new ArrayList<>(valueList.size());
                    refreshedList.addAll(valueList.subList(0, i));
                    refreshedList.add(refreshedItem);
                }
            }

            return refreshedList != null ? refreshedList : valueList;
        }

        if (!(value instanceof String)) {
            return value;
        }

        final StoryBlockReferenceResult result =
                this.refreshStoryBlockValueReferences(value, parentContentletIdentifier, user, respectFrontendRoles);
        return result.isRefreshed() ? result.getValue() : value;
    }

    /**
     * Determines whether a map matches the expected Story Block document structure.
     *
     * @param valueMap Candidate map.
     *
     * @return {@code true} when the map looks like a Story Block root document.
     */
    private boolean isStoryBlockMap(final Map<String, Object> valueMap) {
        return "doc".equals(valueMap.get(TYPE_KEY)) && valueMap.get(CONTENT_KEY) instanceof List;
    }

    /**
     * Generates a file link for the specified contentlet and field.
     * <p>
     * The link is built with the system user: by the time it is built, the Contentlet has already
     * been read on behalf of the requesting user, and the link only needs the Site the file
     * belongs to.
     *
     * @param contentlet the contentlet object that contains the data
     * @param field the field for which the file link will be generated
     * @param request the current request, used to build an absolute link
     * @return an Optional containing the file link as a String if generated successfully,
     *         or an empty Optional if an error occurs or the link cannot be generated
     */
    private static Optional<String> getFileLink(final Contentlet contentlet, final Field field,
                                                final HttpServletRequest request) {
        String fileLink = null;
        try {
            fileLink = new ResourceLink.ResourceLinkBuilder().getFileLink(request, APILocator.systemUser(), contentlet, field.variable());
        }catch (Exception e){
            Logger.error(StoryBlockAPIImpl.class, "Error getting file link for field: " + field.variable(), e);
        }
        return Optional.ofNullable(fileLink);
    }

}
