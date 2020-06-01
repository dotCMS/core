package com.dotmarketing.portlets.contentlet.transform.strategy;

/**
 * Enum that summarises the list of "commands" that can be interpreted by the transforming classes
 */
public enum TransformOptions {

    COMMON_PROPS(true), // Common stuff present in all content
    CONSTANTS(true), //This instructs strategies to include fields of type constant
    VERSION_INFO(true), //This will include stuff like (live,working,archived, hasLiveVersion)
    LANGUAGE_PROPS(true), // Instructs the DefaultStrategy to include extra language props such as (languageCode, country, isoCode)
    IDENTIFIER_AS_MAP(true), //This instructs the transformer to emulate the old IdentifierToMapTransformer
    LOAD_META(true), // If this is on meta data will be included for FileAssets
    BINARIES(true),   //This instructs strategies to include binaries
    USE_ALIAS(true),  //This will include stuff like ('live' and 'isLive')

    //Plug additional stuff to manipulate the outcome as a particular type of view
    LANGUAGE_VIEW, //This instructs the transformer to emulate the old IdentifierToMapTransformer
    BINARIES_VIEW, //This Emulates the old BinaryToMapTransformer. If This one is included then INC_BINARIES is ignored
    CATEGORIES_VIEW; //This emulates the Category To MapTransformer

    private boolean property = false;

    /**
     * Property Option Marker constructor
     * @param property
     */
    TransformOptions(final boolean property) {
        this.property = property;
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
    public boolean isProperty() {
        return property;
    }
}
