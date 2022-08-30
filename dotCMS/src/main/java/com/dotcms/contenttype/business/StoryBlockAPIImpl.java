package com.dotcms.contenttype.business;

import com.dotcms.content.business.json.ContentletJsonHelper;
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

    @Override
    public Tuple2<Boolean, Contentlet> refreshReferences(final Contentlet contentlet) {

        final MutableBoolean refreshed = new MutableBoolean(false);
        if (null != contentlet) {

            contentlet.getContentType().fields(StoryBlockField.class).forEach(field -> {

                final Object storyBlockValue = contentlet.get(field.variable());
                if (null != storyBlockValue) {

                    final Tuple2<Boolean, Object> result = this.refreshStoryBlockValueReferences(storyBlockValue);
                    if (result._1()) { // the story block value has been changed and has been overridden

                        refreshed.setTrue();
                        contentlet.setProperty(field.variable(), result._2());
                    }
                }
            });
        }

        return Tuple.of(refreshed.booleanValue(), contentlet);
    }
    @Override
    public  Tuple2<Boolean, Object> refreshStoryBlockValueReferences(final Object storyBlockValue) {

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

                return Tuple.of(true, toJson(blockEditorMap)); // has changed and the now json is returned
            }
        } catch (final Exception e) {

            Logger.debug(ContentletTransformer.class, e.getMessage());
        }

        return Tuple.of(false, storyBlockValue); // return the original value and value didn't change
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

    @Override
    public List<String> getDependencies(final Contentlet contentlet) {

        final ImmutableList.Builder<String> contentletIdentifierList = new ImmutableList.Builder<>();

        contentlet.getContentType().fields(StoryBlockField.class).forEach(field -> {

            contentletIdentifierList.addAll(this.getDependencies(contentlet.get(field.variable())));
        });

        return contentletIdentifierList.build();
    }

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

            Logger.debug(ContentletTransformer.class, e.getMessage());
        }

        return contentletIdentifierList.build();
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
