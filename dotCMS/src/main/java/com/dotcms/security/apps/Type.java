package com.dotcms.security.apps;

/**
 * This Enum provides the different available types of input parameters that can be used in the
 * definition of an App, via its YAML file.
 *
 * @author Fabrizzio Araya
 * @since Jan 27th, 2020
 */
public enum Type {

    STRING,
    BOOL,
    SELECT,
    BUTTON,
    GENERATED_STRING,
    HEADING,
    INFO,
    /**
     * A string value that must hold a valid JSON document. Saved secrets are persisted as
     * {@link #STRING} (see {@code AppsUtil.storedType}) so a rollback to a release without this
     * value can still read them; the type is reported from the app descriptor instead. The UI
     * renders a code editor, and both the UI and the backend reject values that can not be parsed
     * as JSON.
     */
    JSON

}
