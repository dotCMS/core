package com.dotcms.contenttype.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * Api to handle dependencies, references and so for the StoryBlock (content editor)
 * @author jsanca
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
     * Encapsulates the allowed types for contentlets on the story block
     */
    Set<String> allowedTypes = new ImmutableSet.Builder<String>().add("dotContent","dotImage").build();

    /**
     * Analyzed all {@link com.dotcms.contenttype.model.field.StoryBlockField} fields, refreshing all contentlet which
     * inode is different to the live inode on the system.
     * @param contentlet {@link Contentlet} to refresh
     * @return Contentlet content refreshed
     */
    StoryBlockReferenceResult refreshReferences(final Contentlet contentlet);

    /**
     * Refresh the story block references for a story block json (The argument storyBlockValue will be converted to string and parse as json)
     * @param storyBlockValue Object
     * @return Tuple2 boolean if there was something to refresh, the object is the new object refreshed; if not anything to refresh return the same object sent as an argument
     */
    StoryBlockReferenceResult refreshStoryBlockValueReferences(final Object storyBlockValue);

    /**
     * For each {@link com.dotcms.contenttype.model.field.StoryBlockField} field, retrieve contentlet ids referrer on the
     * story block json.
     * @param contentlet {@link Contentlet}
     * @return List of identifier (empty list if not any contentlet)
     */
    List<String> getDependencies (final Contentlet contentlet);

    /**
     * Get the dependencies for a story block json /Users/jsanca/gitsources/new-core2/core/dotCMS/src/main/java/com/dotcms/contenttype/business/StoryBlockAPI.java
     * @param storyBlockValue Object
     * @return List of contentlets on the story block referrer
     */
    List<String> getDependencies (final Object storyBlockValue);

    /**
     * Adds a contentlet to the story block value
     * @param storyBlockValue {@link Object}
     * @param contentlet {@link Contentlet}
     * @return Object
     */
    Object addContentlet(final Object storyBlockValue, final Contentlet contentlet);

}
