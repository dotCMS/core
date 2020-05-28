package com.dotmarketing.portlets.contentlet.transform.strategy;

public enum TransformOptions {
    INC_COMMON_PROPS, // Common stuff present in all content
    INC_CONSTANTS, //This instructs strategies to include fields of type constant
    INC_VERSION_INFO, //This will include stuff like (live,working,archived, hasLiveVersion)
    LANGUAGE_PROPS,
    LANGUAGE_AS_MAP, //This instructs the transformer to emulate the old IdentifierToMapTransformer
    IDENTIFIER_AS_MAP, //This instructs the transformer to emulate the old IdentifierToMapTransformer
    LOAD_META, // If this is on meta data will be included for FileAssets
    INC_BINARIES,   //This instructs strategies to include binaries
    BINARIES_AS_MAP, //This Emulates the old BinaryToMapTransformer. If This one is included then INC_BINARIES is ignored
    USE_ALIAS  //This will include stuff like ('live' and 'isLive')
}
