package com.dotcms.contenttype.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.transform.ContentletTransformer;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * default implementation
 * @author jsanca
 */
public class StoryBlockAPIImpl implements StoryBlockAPI {

    @CloseDBIfOpened
    @Override
    public StoryBlockReferenceResult refreshReferences(final Contentlet contentlet) {

        final MutableBoolean refreshed = new MutableBoolean(false);
        if (null != contentlet) {

            // todo: may be we can store a flag such as has story block to avoid the hit to the fields when there is not any block editor
            contentlet.getContentType().fields(StoryBlockField.class) // todo, this method filters but creates a new list in memory
                    .forEach(field -> {

                final Object storyBlockValue = contentlet.get(field.variable());
                if (null != storyBlockValue) {

                    final StoryBlockReferenceResult result = this.refreshStoryBlockValueReferences(storyBlockValue);
                    if (result.isRefreshed()) { // the story block value has been changed and has been overridden

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
    public  StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue) {

        boolean refreshed = false;
        try {

            final LinkedHashMap blockEditorMap = this.toMap(storyBlockValue);

            final List contentsMap = (List) blockEditorMap.get(CONTENT_KEY);

            for (final Object contentMapObject : contentsMap) {

                if (null != contentMapObject) {

                    final Map contentMap = (Map) contentMapObject;
                    final Object type = contentMap.get(TYPE_KEY);
                    if (allowedTypes.contains(type)) {

                        refreshed |= refreshStoryBlockMap(contentMap);
                    }
                }
            }

            if (refreshed) {

                return new StoryBlockReferenceResult(true, toJson(blockEditorMap)); // has changed and the now json is returned
            }
        } catch (final Exception e) {

            Logger.debug(StoryBlockAPIImpl.class, e.getMessage());
            throw new RuntimeException(e);
        }

        return new StoryBlockReferenceResult(false, storyBlockValue); // return the original value and value didn't change
    }

    private boolean refreshStoryBlockMap(final Map contentMap) throws DotDataException, DotSecurityException {

        boolean refreshed  = false;
        final Map attrsMap = (Map) contentMap.get(ATTRS_KEY);
        if (null != attrsMap) {

            final Map dataMap = (Map) attrsMap.get(DATA_KEY);
            if (null != dataMap) {

                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                final String inode      = (String) dataMap.get(INODE_KEY);
                final long languageId = ConversionUtils.toLong(dataMap.get(LANGUAGE_ID_KEY), ()-> APILocator.getLanguageAPI().getDefaultLanguage().getId());
                if (null != identifier && null != inode) {

                    final Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
                    if (null != versionInfo && versionInfo.isPresent() && null != versionInfo.get().getLiveInode()  &&
                            !inode.equals(versionInfo.get().getLiveInode())) {

                        // the inode stored on the json does not match with any top inode, so the information stored is old and need refresh
                        this.refreshBlockEditorDataMap(dataMap, versionInfo.get().getLiveInode());
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

        final ImmutableList.Builder<String> contentletIdentifierList = new ImmutableList.Builder<>();

        contentlet.getContentType().fields(StoryBlockField.class).forEach(field -> {

            contentletIdentifierList.addAll(this.getDependencies(contentlet.get(field.variable())));
        });

        return contentletIdentifierList.build();
    }

    @CloseDBIfOpened
    @Override
    public  List<String> getDependencies (final Object storyBlockValue) {

        final ImmutableList.Builder<String> contentletIdentifierList = new ImmutableList.Builder<>();

        try {

            final LinkedHashMap blockEditorMap = this.toMap(storyBlockValue);
            final List contentsMap = (List) blockEditorMap.get(CONTENT_KEY);

            for (final Object contentMapObject : contentsMap) {

                if (null != contentMapObject) {

                    final Map contentMap = (Map) contentMapObject;
                    final Object type = contentMap.get(TYPE_KEY);
                    if (allowedTypes.contains(type)) {

                        this.addDependencies(contentletIdentifierList, contentMap);
                    }
                }
            }
        } catch (final Exception e) {

            Logger.debug(StoryBlockAPIImpl.class, e.getMessage());
            throw new RuntimeException(e);
        }

        return contentletIdentifierList.build();
    }

    @Override
    public Object addContentlet(final Object storyBlockValue, final Contentlet contentlet) {

        try {

            final Map storyBlockValueMap = toMap(storyBlockValue);
            this.addContentlet(storyBlockValueMap, contentlet);
            return toJson(storyBlockValueMap);
        } catch (JsonProcessingException e) {

            Logger.debug(StoryBlockAPIImpl.class, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map addContentlet(final Map storyBlockValueMap, final Contentlet contentlet) {

        if (storyBlockValueMap.containsKey(StoryBlockAPI.CONTENT_KEY)) {

            final List contentList = (List)storyBlockValueMap.get(StoryBlockAPI.CONTENT_KEY);
            final Map dataMap   = new LinkedHashMap();
            final List<Field> fields = contentlet.getContentType().fields();

            dataMap.put("hostName", contentlet.getHost());
            dataMap.put("modDate", contentlet.getModDate());
            dataMap.put("title", contentlet.getTitle());
            dataMap.put("contentTypeIcon", contentlet.getContentType().icon());
            dataMap.put("baseType", contentlet.getContentType().baseType().getAlternateName());
            dataMap.put("inode", contentlet.getInode());
            dataMap.put("archived", Try.of(()->contentlet.isArchived()).getOrElse(false));
            dataMap.put("working",  Try.of(()->contentlet.isWorking()).getOrElse(false));
            dataMap.put("locked",   Try.of(()->contentlet.isLocked()).getOrElse(false));
            dataMap.put("stInode",  contentlet.getContentType().inode());
            dataMap.put("contentType",  contentlet.getContentType().variable());
            dataMap.put("live",   Try.of(()->contentlet.isLive()).getOrElse(false));
            dataMap.put("owner",  contentlet.getOwner());
            dataMap.put("identifier", contentlet.getIdentifier());
            dataMap.put("languageId", contentlet.getLanguageId());
            dataMap.put("hasLiveVersion", Try.of(()->contentlet.hasLiveVersion()).getOrElse(false));
            dataMap.put("folder", contentlet.getFolder());
            dataMap.put("sortOrder", contentlet.getSortOrder());
            dataMap.put("modUser", contentlet.getModUser());

            for (final Field field : fields) {

                dataMap.put(field.variable(), contentlet.get(field.variable()));
            }

            final Map attrsMap   = new LinkedHashMap();
            attrsMap.put(StoryBlockAPI.DATA_KEY, dataMap);
            final Map contentMap = new LinkedHashMap();
            contentMap.put(StoryBlockAPI.ATTRS_KEY, attrsMap);
            contentMap.put(StoryBlockAPI.TYPE_KEY, "dotContent");
            contentList.add(contentMap);
        }

        return storyBlockValueMap;
    }

    private static void addDependencies(final ImmutableList.Builder<String> contentletIdentifierList,
                                        final Map contentMap) {

        final Map attrsMap = (Map) contentMap.get(ATTRS_KEY);
        if (null != attrsMap) {

            final Map dataMap = (Map) attrsMap.get(DATA_KEY);
            if (null != dataMap) {

                final String identifier = (String) dataMap.get(IDENTIFIER_KEY);
                contentletIdentifierList.add(identifier);
            }
        }
    }

    private LinkedHashMap toMap(final Object blockEditorValue) throws JsonProcessingException {

        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                .readValue(Try.of(() -> blockEditorValue.toString())
                        .getOrElse(StringPool.BLANK), LinkedHashMap.class);
    }

    private String toJson (final Object blockEditorMap) throws JsonProcessingException {

        return ContentletJsonHelper.INSTANCE.get().objectMapper()
                .writeValueAsString(blockEditorMap);
    }

    private void refreshBlockEditorDataMap(final Map dataMap, final String liveINode) throws DotDataException, DotSecurityException {

        final Contentlet contentlet = APILocator.getContentletAPI().find(
                liveINode, APILocator.systemUser(), false);
        final Set contentFieldNames = dataMap.keySet();
        for (final Object contentFieldName : contentFieldNames) {

            final Object value = contentlet.get(contentFieldName.toString());
            if (null != value) {

                dataMap.put(contentFieldName, value);
            }
        }
    }
}
