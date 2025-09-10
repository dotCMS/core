package com.dotmarketing.portlets.contentlet.transform.strategy;

/**
 * This Enum summarises the list of "commands" that can be interpreted by the transforming classes.
 * These options allow developers to tell dotCMS what specific groups of data attributes or single
 * attributes must be included when transforming a Contentlet into a data Map, and how they need
 * to be included.
 *
 * @author Fabrizzio Araya
 * @since Jun 11th, 2020
 */
public enum TransformOptions {

    /**
     * Instructs the Strategy to include common stuff present in all content.
     */
    COMMON_PROPS(true),
    /**
     * Instructs the Strategy to include fields of type constant.
     */
    CONSTANTS(true),
    /**
     * Instructs the Strategy to include information such as: 'live', 'working', 'archived',
     * 'hasLiveVersion', etc.
     */
    VERSION_INFO(true),
    /**
     * Instructs the Strategy to include extra language properties such as: 'languageCode',
     * 'country', 'isoCode', etc.
     */
    LANGUAGE_PROPS(true),
    /**
     * Instructs the Strategy to transform and include binaries. The absence of this will cause the
     * binaries to still make it into the final contentlet.
     */
    BINARIES(true),
    /**
     * Instructs the Strategy to simply prevent the binaries from making it into the final resulting
     * map.
     */
    FILTER_BINARIES(true),
    /**
     * Instructs the Strategy to render only the category name: (key=name)
     */
    CATEGORIES_NAME(true),
    /**
     * Instructs the Strategy to render much more Categories info.
     */
    CATEGORIES_INFO(true),
    /**
     * Instructs the Strategy to render much more Tags info
     */
    TAGS(true),
    /**
     * Instructs the Strategy to include alias properties such as: 'live' and 'isLive'.
     */
    USE_ALIAS(true),
    /**
     * Instructs the Strategy to include MetaData for File Assets.
     */
    LOAD_META,
    /**
     * Instructs the Strategy to emulate the old IdentifierToMapTransformer.
     */
    IDENTIFIER_VIEW,
    /**
     * Instructs the Strategy to emulate the old IdentifierToMapTransformer.
     */
    LANGUAGE_VIEW,
    /**
     * Instructs the Strategy to emulate the old BinaryToMapTransformer.
     */
    BINARIES_VIEW,
    /**
     * Instructs the Strategy to include Key/Value data.
     */
    KEY_VALUE_VIEW,
    /**
     * Instructs the Strategy to emulate the Category To MapTransformer.
     */
    CATEGORIES_VIEW,
    /**
     * Instructs the Strategy to include File Asset data.
     */
    FILEASSET_VIEW,
    /**
     * Instructs the Strategy to include Site data.
     */
    SITE_VIEW,
    /**
     * Instructs the Strategy to include Story Block data
     */
    STORY_BLOCK_VIEW,
    AVOID_MAP_SUFFIX_FOR_VIEWS,
    /**
     * Instructs the Strategy to velocity-render the render-able fields
     */
    RENDER_FIELDS, //This triggers a Strategy that will render the fields explicitly
    JSON_VIEW,
    DATETIME_FIELDS_TO_TIMESTAMP,

    /**
     * Instructs the Strategy to skip the rendering of the widget code
     * I hate to introduce this but seems like the safest way to avoid breaking backward compatibility
     * This options controls the Strategy that gets fired by the Widget Content Type which by default will render the widget code
     */
    SKIP_WIDGET_CODE_RENDERING,
    /** Instructs the Strategy to include specific properties that are displayed in the History
     * tab of the Content Editor page. */
    HISTORY_VIEW,
    /** Instructs the Strategy to clear all existing data in the Contentlet Map before applying a
     * specific Strategy. */
    CLEAR_EXISTING_DATA;

    // -----------------------------------------------------------------------------------------
    // Plug additional Transform Options to manipulate the outcome as a particular type of view
    // -----------------------------------------------------------------------------------------

    private boolean defaultProperty;

    /**
     * Property Option Marker constructor
     * @param defaultProperty
     */
    TransformOptions(final boolean defaultProperty) {
        this.defaultProperty = defaultProperty;
    }

    /**
     * Default constructor
     */
    TransformOptions() {
    }

    /**
     * is Property accessor
     * @return
     */
    public boolean isDefaultProperty() {
        return defaultProperty;
    }

}
