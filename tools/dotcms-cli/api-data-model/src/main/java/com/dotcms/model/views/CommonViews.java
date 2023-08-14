/**
 * This is a utility class that enables us to handle different JSON views with Jackson.
 * <p>
 * JsonView lets you serialize different views differently. Your class can have different views to
 * limit or to permit what to serialize for different use cases.
 */
package com.dotcms.model.views;

public class CommonViews {

    /**
     * This interface represents the "External" view for our JSON serialization.
     */
    public interface ExternalView {

    }

    /**
     * This interface represents the "Internal" view for our JSON serialization. It extends from the
     * "External" view, meaning it will include everything from the "External" view and possibly
     * more.
     */
    public interface InternalView extends ExternalView {

    }

}