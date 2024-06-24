/**
 * This is a utility class that enables us to handle different JSON views with Jackson.
 * <p>
 * JsonView lets you serialize different views differently. Your class can have different views to
 * limit or to permit what to serialize for different use cases.
 */
package com.dotcms.model.views;


/***
 * These views can be applied directly in the ObjectMapper
 * By doing:
 *  <pre>
 *    @code  objectMapper.writerWithView(CommonViews.InternalView.class).writeValueAsString(contentType);
 *  </pre>
 * Typically, we try to keep only one single definition of the Object Mapper
 * when using the writerWithView it creates a new instance of the Mapper therefore the view needs to be set right before writing out the json
 * You can not create a global config of the map with these view and expect them to be there when requesting the global mapper
 * The Other way to use them is adding them to the RestEasyClient like this:
 * <pre>
 * @code
 *     ResponseEntityView<ContentType> updateContentTypes(@PathParam("idOrVar") final String idOrVar,
 *             @JsonView({CommonViews.SaveOrUpdate.class}) final ContentType contentType);
 * </pre>
 * The class that does the processing is
 * @see org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider
 * And even though the annotation allows for multiple views. Only the firstone is actaully used.
 * Another Thing to be mindful about is the fact that inheritance plays an important part when applying views.
 * This means that if you apply a view, everything else that can be assigned to that view will render too.
 */
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

    /**
     * The LanguageFileView interface defines the view used for the language file descriptor
     */
    public interface LanguageFileView {

    }

    /**
     * The LanguageReadView interface defines the view used for some server read operations
     */
    public interface LanguageReadView {

    }

    /**
     * The LanguageWriteView interface defines the view used for some server write operations
     */
    public interface LanguageWriteView {

    }

    /**
     * This interface represents the "Internal" view for our Content Type JSON serialization.
     */
    public interface ContentTypeInternalView {

    }

    /**
     * This interface represents the "External" view for our Content Type JSON serialization used
     * for creating or updating content types.
     */
    public interface ContentTypeExternalView {

    }

}