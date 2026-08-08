package com.dotcms.contenttype.business;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

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
    @CloseDBIfOpened
    public StoryBlockReferenceResult refreshReferences(final Contentlet contentlet) {
        final MutableBoolean refreshed = new MutableBoolean(false);
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
                if (null != request) {
                    request.setAttribute(CURRENT_DEPTH_ATTR, currentDepth);
                }
                return new StoryBlockReferenceResult(false, contentlet);
            }

            contentType.fields(StoryBlockField.class)
                    .forEach(field -> {

                        final Object storyBlockValue = contentlet.get(field.variable());

                        if (null != storyBlockValue) {
                            final StoryBlockReferenceResult result =
                                    this.refreshStoryBlockValueReferences(storyBlockValue, contentlet.getIdentifier());
                            if (result.isRefreshed()) {
                                refreshed.setTrue();
                                contentlet.setProperty(field.variable(), result.getValue());
                            }
                        }

                    });
        }
        return new StoryBlockReferenceResult(refreshed.booleanValue(), contentlet);
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

    @RequestCost(Price.BLOCK_EDITOR_HYDRATION)
    @CloseDBIfOpened
    @Override
    @SuppressWarnings("unchecked")
    public StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue, final String parentContentletIdentifier) {
        boolean refreshed;
        if (null != storyBlockValue && isJsonObject(storyBlockValue.toString())) {
            try {
                final LinkedHashMap<String, Object> blockEditorMap = this.toMap(storyBlockValue);
                final Object contentsMap = blockEditorMap.get(CONTENT_KEY);
                if (!UtilMethods.isSet(contentsMap) || !(contentsMap instanceof List)) {
                    return new StoryBlockReferenceResult(true, storyBlockValue);
                }
                refreshed = isRefreshed(parentContentletIdentifier, (List<Map<String, Object>>) contentsMap);
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

    private boolean isRefreshed(final String parentContentletIdentifier,
                                final List<Map<String, Object>> contentsMap) {

        boolean refreshed = false;
        for (final Map<String, Object> contentMap : contentsMap) {
            if (UtilMethods.isSet(contentMap)) {
                // Isolate per-block failures so that one bad nested reference
                // does not prevent the rest of the Story Block from refreshing.
                try {
                    final String type = contentMap.get(TYPE_KEY).toString();
                    if (allowedTypes.contains(type)) { // if somebody adds a story block to itself, we don't want to refresh it

                        refreshed |= this.refreshStoryBlockMap(contentMap, parentContentletIdentifier);
                    } else {
                        final Object nestedContent = contentMap.get(CONTENT_KEY);
                        if (nestedContent instanceof List) {
                            refreshed |= this.isRefreshed(parentContentletIdentifier, (List<Map<String, Object>>) nestedContent);
                        }
                    }
                } catch (final Exception e) {
                    Logger.warnAndDebug(StoryBlockAPIImpl.class, String.format(
                            "Skipping Story Block child while refreshing parent '%s': %s",
                            parentContentletIdentifier, ExceptionUtil.getErrorMessage(e)), e);
                }
            }
        }
        return refreshed;
    }

    /**
     * Takes the current data map of the referenced Contentlet in the Story Block field and checks whether it matches
     * its latest live version or not. If it doesn't, then it means it must be refreshed with the data map from the
     * latest version and the content map will be updated accordingly.
     *
     * @param contentMap The Map containing the different properties of the referenced Contentlet.
     *
     * @return If the referenced Contentlet <b>IS NOT</b> the latest live version, then its data map will be refreshed
     * and {@code true} will be returned.
     */
    @SuppressWarnings("unchecked")
    private boolean refreshStoryBlockMap(final Map<String, Object> contentMap, final String parentContentletIdentifier) {
        boolean refreshed  = false;
        final Map<String, Object> attrsMap = (Map<String, Object>) contentMap.get(ATTRS_KEY);
        if (UtilMethods.isSet(attrsMap)) {
            final Object dataValue = attrsMap.get(DATA_KEY);
            if (UtilMethods.isSet(dataValue)) {
                Map<String, Object> dataMap;

                // Handle case where data is a JSON string instead of a Map
                if (dataValue instanceof String) {
                    try {
                        dataMap = this.toMap(dataValue);
                    } catch (JsonProcessingException e) {
                        Logger.warnAndDebug(this.getClass(), String.format(
                            "Failed to parse data field as JSON for parent contentlet '%s': %s",
                            parentContentletIdentifier, ExceptionUtil.getErrorMessage(e)), e);
                        return false;
                    }
                } else if (dataValue instanceof Map) {
                    dataMap = (Map<String, Object>) dataValue;
                } else {
                    Logger.warn(this.getClass(), String.format(
                        "Unexpected data type '%s' for data field in parent contentlet '%s'",
                        dataValue.getClass().getName(), parentContentletIdentifier));
                    return false;
                }

                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                final long languageId = ConversionUtils.toLong(dataMap.get(LANGUAGE_ID_KEY), ()-> APILocator.getLanguageAPI().getDefaultLanguage().getId());
                if (UtilMethods.isSet(identifier)) {
                    // if somebody adds a story block to itself, we don't want to refresh it
                    if (!identifier.equals(parentContentletIdentifier)) {
                        final Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
                        if (versionInfo.isPresent()) {
                            final String chosenInode = this.getInodeBasedOnPageMode(versionInfo.get());
                            this.refreshBlockEditorDataMap(dataMap, chosenInode);
                            refreshed = true;
                        }
                    } else {
                        refreshed = true;
                    }
                }
            }
        }
        return refreshed;
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
     * Loads the common properties of a {@link Contentlet} into the specified data map.
     *
     * @param contentlet The Contentlet whose properties will be loaded.
     * @param dataMap    The Map where the properties will be stored.
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
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .readValue(Try.of(blockEditorValue::toString)
                                          .getOrElse(StringPool.BLANK), LinkedHashMap.class);
    }

    @Override
    public String toJson(final Map<String, Object> blockEditorMap) throws JsonProcessingException {
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .writeValueAsString(blockEditorMap);
    }

    /**
     * Refreshes the data map from a referenced Contentlet in the Story Block field with the latest
     * data specified by the live Inode.
     *
     * @param dataMap The Map containing the Contentlet's properties.
     * @param inode   The live Inode of the Contentlet whose properties will be copied over to the
     *                previous data map.
     */
    private void refreshBlockEditorDataMap(final Map<String, Object> dataMap, final String inode) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        // If 'true', it means that the parent Block Editor is being processed, and not its
        // potential child contents
        final boolean isCurrentDepthEmpty = request.getAttribute(CURRENT_DEPTH_ATTR) == null;
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
            final Contentlet fattyContentlet = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), DONT_RESPECT_FRONT_END_ROLES, true);

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
                    return;
                }
                try {
                    this.addContentletRelationships(fattyContentlet, currentDepth);
                    final Map<String, Object> updatedDataMap = this.refreshContentlet(fattyContentlet);
                    this.excludeNonExistingProperties(dataMap, updatedDataMap);
                    dataMap.putAll(updatedDataMap);
                } finally {
                    processing.remove(fattyIdentifier);
                }
            }
        } catch (final JsonProcessingException e) {
            Logger.error(this, String.format("An error occurred when transforming JSON data in contentlet with Inode " +
                    "'%s': %s", inode, ExceptionUtil.getErrorMessage(e)), e);
        } catch (final DotDataException | DotSecurityException e) {
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
    }

    /**
     * Adds the relationships of the specified {@link Contentlet} object, if required.
     *
     * @param contentlet The Contentlet that may contain Relationship fields.
     */
    private void addContentletRelationships(final Contentlet contentlet, final int depth) {
        final HttpServletRequest httpRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final PageMode currentPageMode = PageMode.get(httpRequest);

        ContentUtils.addRelationships(contentlet, APILocator.systemUser(), currentPageMode, contentlet.getLanguageId(), depth);
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
     * @param contentlet The {@link Contentlet} with the latest properties.
     *
     * @return The updated data map with the latest properties.
     *
     * @throws JsonProcessingException An error occurred when processing property with JSON data.
     */
    private Map<String, Object> refreshContentlet(final Contentlet contentlet)
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
                        final Object rawValue = contentlet.get(field.variable() + "_raw");
                        final String rawStr = rawValue != null ? rawValue.toString() : null;
                        if (rawStr != null && isJsonObject(rawStr)) {
                            dataMap.put(field.variable(), this.toMap(rawValue));
                        } else if (isJsonObject(value.toString())) {
                            dataMap.put(field.variable(), this.toMap(value));
                        }
                    } else {
                        dataMap.putIfAbsent(field.variable(),
                                this.refreshNestedStoryBlockValues(value, contentlet.getIdentifier(),
                                        MAX_NESTED_STORY_BLOCK_REFRESH_DEPTH));
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
     *
     * @return A refreshed value preserving the same logical structure.
     *
     * @throws JsonProcessingException If Story Block JSON cannot be transformed while refreshing.
     */
    @SuppressWarnings("unchecked")
    private Object refreshNestedStoryBlockValues(final Object value, final String parentContentletIdentifier,
            final int remainingDepth)
            throws JsonProcessingException {
        if (remainingDepth <= 0) {
            return value;
        }

        if (value instanceof Map) {
            final Map<String, Object> valueMap = (Map<String, Object>) value;
            if (this.isStoryBlockMap(valueMap)) {
                // refreshStoryBlockValueReferences currently processes JSON values, so Story Block maps
                // must be normalized to JSON before refresh and parsed back afterwards.
                final StoryBlockReferenceResult refreshedValue =
                        this.refreshStoryBlockValueReferences(this.toJson(valueMap), parentContentletIdentifier);
                return refreshedValue.isRefreshed() ? this.toMap(refreshedValue.getValue()) : valueMap;
            }
            Map<String, Object> refreshedMap = null;
            for (final Map.Entry<String, Object> entry : valueMap.entrySet()) {
                final Object nestedValue = entry.getValue();
                final Object refreshedNestedValue = this.refreshNestedStoryBlockValues(nestedValue,
                        parentContentletIdentifier, remainingDepth - 1);

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
                        remainingDepth - 1);

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
                this.refreshStoryBlockValueReferences(value, parentContentletIdentifier);
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
     *
     * @param contentlet the contentlet object that contains the data
     * @param field the field for which the file link will be generated
     * @return an Optional containing the file link as a String if generated successfully,
     *         or an empty Optional if an error occurs or the link cannot be generated
     */
    private static Optional<String> getFileLink(final Contentlet contentlet, final Field field, final HttpServletRequest request) {
        String fileLink = null;
        try {
            fileLink = new ResourceLink.ResourceLinkBuilder().getFileLink(request, APILocator.systemUser(), contentlet, field.variable());
        }catch (Exception e){
            Logger.error(StoryBlockAPIImpl.class, "Error getting file link for field: " + field.variable(), e);
        }
        return Optional.ofNullable(fileLink);
    }

}
