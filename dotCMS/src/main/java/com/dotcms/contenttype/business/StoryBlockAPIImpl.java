package com.dotcms.contenttype.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation class for the {@link StoryBlockAPI}.
 *
 * @author Jonathan Sanchez
 * @since Oct 19th, 2022
 */
public class StoryBlockAPIImpl implements StoryBlockAPI {

    private static final Lazy<Integer> MAX_RECURSION_LEVEL = Lazy.of(() -> Config.getIntProperty("STORY_BLOCK_MAX_RECURSION_LEVEL", 2));

    @Override
    @CloseDBIfOpened
    public StoryBlockReferenceResult refreshReferences(final Contentlet contentlet) {
        final MutableBoolean refreshed = new MutableBoolean(false);
        if (null != contentlet && null != contentlet.getContentType() &&
                contentlet.getContentType().hasStoryBlockFields()) {

            if (ExceptionUtil.isMethodCallCountGteThan(
                    "com.dotcms.contenttype.business.StoryBlockAPIImpl.refreshReferences", MAX_RECURSION_LEVEL.get())) {
                Logger.debug(this, () -> "The StoryBlockAPIImpl.refreshReferences method has been called more than 2 times" +
                        " in the same thread. This could be a sign of a circular reference in the Story Block field. Do not refreshing anything at this point");
                return new StoryBlockReferenceResult(false, contentlet);
            }

            contentlet.getContentType().fields(StoryBlockField.class)
                    .forEach(field -> {

                        final Object storyBlockValue = contentlet.get(field.variable());
                        if (null != storyBlockValue && JsonUtil.isValidJSON(storyBlockValue.toString())) {
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

    @CloseDBIfOpened
    @Override
    public StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue, final String parentContentletIdentifier) {
        boolean refreshed = false;
        try {
            final LinkedHashMap<String, Object> blockEditorMap = this.toMap(storyBlockValue);
            final Object contentsMap = blockEditorMap.get(CONTENT_KEY);
            if(!UtilMethods.isSet(contentsMap) || !(contentsMap instanceof List)) {
                return new StoryBlockReferenceResult(true, storyBlockValue);
            }
            for (final Map<String, Object> contentMap : (List<Map<String, Object>>) contentsMap) {
                if (UtilMethods.isSet(contentMap)) {
                    final String type = contentMap.get(TYPE_KEY).toString();
                    if (allowedTypes.contains(type)) { // if somebody adds a story block to itself, we don't want to refresh it

                        refreshed |= this.refreshStoryBlockMap(contentMap, parentContentletIdentifier);
                    }
                }
            }
            if (refreshed) {
                return new StoryBlockReferenceResult(true, this.toJson(blockEditorMap));
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when refreshing Story Block Contentlet " +
                                                          "references: %s", e.getMessage());
            Logger.warnAndDebug(StoryBlockAPIImpl.class, errorMsg,e);
            throw new DotRuntimeException(errorMsg, e);
        }
        // Return the original value in case no data was refreshed
        return new StoryBlockReferenceResult(false, storyBlockValue);
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
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User accessing the API does not have the required permissions to do so.
     */
    private boolean refreshStoryBlockMap(final Map<String, Object> contentMap, final String parentContentletIdentifier) throws DotDataException, DotSecurityException {
        boolean refreshed  = false;
        final Map<String, Object> attrsMap = (Map) contentMap.get(ATTRS_KEY);
        if (UtilMethods.isSet(attrsMap)) {
            final Map<String, Object> dataMap = (Map) attrsMap.get(DATA_KEY);
            if (UtilMethods.isSet(dataMap)) {
                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                final long languageId = ConversionUtils.toLong(dataMap.get(LANGUAGE_ID_KEY), ()-> APILocator.getLanguageAPI().getDefaultLanguage().getId());
                if (UtilMethods.isSet(identifier)) {
                    if (!identifier.equals(parentContentletIdentifier)) { // if somebody adds a story block to itself, we don't want to refresh it
                        final Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
                        if (versionInfo.isPresent()) {
                            this.refreshBlockEditorDataMap(dataMap, versionInfo.get().getLiveInode());
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

    @CloseDBIfOpened
    @Override
    public List<String> getDependencies(final Contentlet contentlet) {
        final ImmutableList.Builder<String> contentletIdList = new ImmutableList.Builder<>();
        contentlet.getContentType().fields(StoryBlockField.class).forEach(field -> 

            contentletIdList.addAll(this.getDependencies(contentlet.get(field.variable())))

        );
        return contentletIdList.build();
    }

    @CloseDBIfOpened
    @Override
    public List<String> getDependencies(final Object storyBlockValue) {
        final ImmutableList.Builder<String> contentletIdList = new ImmutableList.Builder<>();

        try {

            if (null != storyBlockValue && JsonUtil.isValidJSON(storyBlockValue.toString())) {
                final Map<String, Object> blockEditorMap = this.toMap(storyBlockValue);
                Object contentsMap = blockEditorMap.getOrDefault(CONTENT_KEY, List.of());
                if(!(contentsMap instanceof List)) {
                    return List.of();
                }
                
                for (final Map<String, Object> contentMapObject : (List<Map<String, Object>>) contentsMap) {
                    if (UtilMethods.isSet(contentMapObject)) {
                        final String type = (String) contentMapObject.get(TYPE_KEY);
                        if (type !=null && allowedTypes.contains(type)) {
                            addDependencies(contentletIdList, contentMapObject);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when retrieving Contentlet references from Story" +
                                                          " Block field: %s", e.getMessage());
            Logger.warnAndDebug(StoryBlockAPIImpl.class, errorMsg,e);
            
        }
        return contentletIdList.build();
    }

    @Override
    public Object addContentlet(final Object storyBlockValue, final Contentlet contentlet) {
        try {
            final Map<String, Object> storyBlockValueMap = this.toMap(storyBlockValue);
            this.addContentlet(storyBlockValueMap, contentlet);
            return this.toJson(storyBlockValueMap);
        } catch (final JsonProcessingException e) {
            final String errorMsg = String.format("An error occurred when adding Contentlet '%s' to the Story Block " +
                                                          "field: %s", contentlet.getIdentifier(), e.getMessage());
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
    private Map<String, Object> addContentlet(final Map<String, Object> storyBlockValueMap, final Contentlet contentlet) {
        if (storyBlockValueMap.containsKey(StoryBlockAPI.CONTENT_KEY)) {
            final List<Map<String, Object>> contentList = (List) storyBlockValueMap.get(StoryBlockAPI.CONTENT_KEY);
            final Map<String, Object> dataMap = new LinkedHashMap<>();
            final List<Field> fields = contentlet.getContentType().fields();

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

            for (final Field field : fields) {
                dataMap.put(field.variable(), contentlet.get(field.variable()));
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
     * Finds Identifiers in the specified data map in order to list the referenced Contentlets in a Story Block field.
     *
     * @param contentletIdList The list of Contentlet Identifiers referenced by the Story Block field.
     * @param contentMap       The Story Block data map.
     */
    private static void addDependencies(final ImmutableList.Builder<String> contentletIdList,
                                        final Map contentMap) {
        final Map<String, Map<String, Object>> attrsMap = (Map) contentMap.get(ATTRS_KEY);
        if (UtilMethods.isSet(attrsMap)) {
            final Map<String, Object> dataMap = attrsMap.get(DATA_KEY);
            if (UtilMethods.isSet(dataMap)) {
                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                contentletIdList.add(identifier);
            }
        }
    }

    @Override
    public LinkedHashMap<String, Object> toMap(final Object blockEditorValue) throws JsonProcessingException {
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .readValue(Try.of(blockEditorValue::toString)
                                          .getOrElse(StringPool.BLANK), LinkedHashMap.class);
    }

    @Override
    public String toJson (final Map<String, Object> blockEditorMap) throws JsonProcessingException {
        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                       .writeValueAsString(blockEditorMap);
    }

    /**
     * Refreshes the data map from a referenced Contentlet in the Story Block field with the latest data specified by
     * the live Inode.
     *
     * @param dataMap   The Map containing the Contentlet's properties.
     * @param liveInode The live Inode of the Contentlet whose properties will be copied over to the previous data map.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User accessing the API does not have the required permissions to do so.
     */
    private void refreshBlockEditorDataMap(Map<String, Object> dataMap, final String liveInode) {
        final Optional<Contentlet> contentlet = APILocator.getContentletAPI().findInDb(liveInode);
        try {
            if (contentlet.isPresent()) {
                dataMap.putAll(refreshContentlet(contentlet.get()));
            }
        } catch (JsonProcessingException e) {
            Logger.error(this, "Failed to load attrs", e);
        }
    }

    /**
     * Refreshes the Contentlet data using the Contentlet from the API call to the DB
     *
     * @param contentlet
     * @return
     * @throws JsonProcessingException
     */
    private Map<String, Object> refreshContentlet(final Contentlet contentlet) throws JsonProcessingException {
        final Map<String, Object> dataMap = new LinkedHashMap<>();
        final List<Field> fields = contentlet.getContentType().fields();

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

        for (final Field field : fields) {
            final Object value = contentlet.get(field.variable());
            if (null == value) {
                continue;
            }
            if (field instanceof StoryBlockField) {
                dataMap.put(field.variable(), toMap(value));
            } else {
                dataMap.put(field.variable(), value);
            }
        }

        return dataMap;
    }

}
