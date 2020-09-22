package com.dotmarketing.portlets.contentlet.transform.strategy;

/**
 * Enum that summarises the list of "commands" that can be interpreted by the transforming classes
 */
public enum TransformOptions {

    COMMON_PROPS(true), // Common stuff present in all content.
    CONSTANTS(true), //This instructs strategies to include fields of type constant.
    VERSION_INFO(true), //This will include stuff like (live,working,archived, hasLiveVersion)
    LANGUAGE_PROPS(true), // Instructs the DefaultStrategy to include extra language props such as (languageCode, country, isoCode)
    BINARIES(true),   //This instructs strategies to transform and include binaries. The absence of this will cause the binaries to still make it into the final contentlet.
    FILTER_BINARIES(true), //This one is used to simply prevent the binaries from making it into the final resulting map.
    CATEGORIES_NAME(true), //This instructs to render only the category name (key=name)
    CATEGORIES_INFO(true), //This instructs rendering much more info.
    USE_ALIAS(true),  //This will include stuff like ('live' and 'isLive')

    LOAD_META, // If this is on meta data will be included for FileAssets.
    //Plug additional stuff to manipulate the outcome as a particular type of view.
    IDENTIFIER_VIEW, //This instructs the transformer to emulate the old IdentifierToMapTransformer.
    LANGUAGE_VIEW, //This instructs the transformer to emulate the old IdentifierToMapTransformer.
    BINARIES_VIEW, //This Emulates the old BinaryToMapTransformer.
    KEY_VALUE_VIEW,
    CATEGORIES_VIEW; //This emulates the Category To MapTransformer.

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
