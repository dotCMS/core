package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.ContentHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ContentletToMapTransformer {

    private static final String NA = "N/A";

    //This set contains all the properties that we want to prevent from making it into the final contentlet or transformed map.
    static final Set<String> privateInternalProperties = ImmutableSet
            .of(Contentlet.NULL_PROPERTIES, Contentlet.DISABLE_WORKFLOW,
                    Contentlet.DONT_VALIDATE_ME, Contentlet.LAST_REVIEW_KEY,
                    Contentlet.REVIEW_INTERNAL_KEY, Contentlet.DISABLED_WYSIWYG_KEY, Contentlet.DOT_NAME_KEY
            );

    private ContentHelper contentHelper;
    private UserAPI userAPI;
    private final List<Contentlet> contentlets;

    /**
     * Bulk transform constructor
     * @param contentlets input
     */
    public ContentletToMapTransformer(final List<Contentlet> contentlets) {
        this(contentlets,ContentHelper.getInstance(),APILocator.getUserAPI());
    }

    /**
     * Convenience constructor
     * @param contentlets input
     */
    public ContentletToMapTransformer(final Contentlet... contentlets) {
        this(Arrays.asList(contentlets),ContentHelper.getInstance(),APILocator.getUserAPI());
    }

    /**
     * Main constructor provides access to set the required APIs
     * @param contentlets input
     * @param contentHelper helper
     * @param userAPI userAPI
     */
    @VisibleForTesting
    ContentletToMapTransformer(final List<Contentlet> contentlets, final ContentHelper contentHelper, final UserAPI userAPI) {
        this.contentHelper = contentHelper;
        this.userAPI = userAPI;
        this.contentlets = contentlets;
    }

    /**
     * If desired we can do bulk transformation over a collection. So That's why we have a toMaps
     * @return List of transformed Maps
     */
    public List<Map<String, Object>> toMaps() {
       final List<Map<String, Object>> transformed = new ArrayList<>(contentlets.size());
        for(final Contentlet contentlet:contentlets){
            transformed.add(
               transform(copy(contentlet))
            );
        }
        return transformed;
    }

    /**
     * This is the main method where individual transformation takes place.
     * @param contentlet input contentlet
     * @return Map holding the transformed properties
     */
    private Map<String, Object> transform(final Contentlet contentlet) {
        final ContentType type = contentlet.getContentType();
        final Map<String, Object> properties = contentlet.getMap();
        properties.put(Contentlet.CONTENT_TYPE_KEY , type != null ? type.variable() : NA );
        properties.put(Contentlet.TITTLE_KEY, contentlet.getTitle());
        final Optional<Field> titleImageOptional = contentlet.getTitleImage();
        properties.put(ESMappingConstants.TITLE_IMAGE,
            titleImageOptional.isPresent() ?
            titleImageOptional.get() : NA
        );

        if (!properties.containsKey(HTMLPageAssetAPI.URL_FIELD)) {
            final String url = contentHelper.getUrl(contentlet);
            if (null != url) {
                properties.put(HTMLPageAssetAPI.URL_FIELD, url);
            }
        }

        setAdditionalProperties(contentlet);
        clean(contentlet);
        return contentlet.getMap();
    }

    /**
     * Adds needed things that are not included by default from the api to the contentlet.
     * If there is anything new to add, returns copy with the new attributes inside, otherwise returns the same instance.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes
     */
    public List<Contentlet> hydrate() {
        final List<Contentlet> hydrated = new ArrayList<>(contentlets.size());
        for(Contentlet contentlet:contentlets){
          final Contentlet newContentlet = copy(contentlet);
          newContentlet.getMap().putAll(transform(newContentlet));
          hydrated.add(clean(newContentlet));
        }
        return hydrated;
    }

    /**
     * To avoid caching issues we work on a copy
     * @param contentlet input contentlet
     * @return a copy contentlet
     */
    private Contentlet copy(final Contentlet contentlet) {
        final Contentlet newContentlet = new Contentlet();
        newContentlet.getMap().putAll(contentlet.getMap());
        return newContentlet;
    }

    /**
     * Removes all private internal properties from the contentlet
     * @param contentlet input contentlet
     * @return the same contentlet without the private properties
     */
    private Contentlet clean(final Contentlet contentlet) {
        for (final String propertyName : privateInternalProperties) {
            contentlet.getMap().remove(propertyName);
        }
        return contentlet;
    }

    /**
     * Use this method to add any additional property
     * @param contentlet Same contentlet with any additional property added
     */
    private void setAdditionalProperties(final Contentlet contentlet){
        try {
            final User modUser = userAPI.loadUserById(contentlet.getModUser());
            contentlet.getMap().put("modUserName", null != modUser ? modUser.getFullName() : NA );
        } catch (Exception e) {
            Logger.error(getClass(),"Error calculating modUser", e);
        }
    }

}
