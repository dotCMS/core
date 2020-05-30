package com.dotmarketing.portlets.contentlet.transform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.ContentHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is deprecated as it has proven to lack flexibility
 * @deprecated As of 5.3.1 instead use {@link DotTransformerImpl}
 */
@Deprecated
public class ContentletToMapTransformer {

    private static final String NA = "N/A";

    //This set contains all the properties that we want to prevent from making it into the final contentlet or transformed map.
    static final Set<String> privateInternalProperties = ImmutableSet
            .of(Contentlet.NULL_PROPERTIES, 
                Contentlet.DISABLE_WORKFLOW,
                    Contentlet.DONT_VALIDATE_ME,
                    Contentlet.LAST_REVIEW_KEY,
                    Contentlet.REVIEW_INTERNAL_KEY, 
                    Contentlet.DISABLED_WYSIWYG_KEY,
                    Contentlet.DOT_NAME_KEY
            );

    private final ContentHelper contentHelper;
    private final UserAPI userAPI;
    private final List<Contentlet> contentlets;

    /**
    *
    *
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
        return contentlets.stream().map(this::copy).map(this::transform).collect(CollectionsUtils.toImmutableList());
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
        properties.put(Contentlet.HAS_TITLE_IMAGE_KEY, contentlet.getTitleImage().isPresent() );
        properties.put(Contentlet.BASE_TYPE_KEY, type != null ? type.baseType().name() : NA);
        try {
            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser()
                , true);
            properties.put(Contentlet.HOST_NAME, host != null ? host.getHostname() : NA );
        } catch (DotDataException | DotSecurityException e) {
            Logger.warn(this, "Unable to set property: " + Contentlet.HOST_NAME, e);
        }

        if (!properties.containsKey(HTMLPageAssetAPI.URL_FIELD)) {
            final String url = contentHelper.getUrl(contentlet);
            if (null != url) {
                properties.put(HTMLPageAssetAPI.URL_FIELD, url);
            }
        }
        addConstants(contentlet);
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
        return contentlets.stream().map(this::copy).map(newContentlet -> {
            newContentlet.getMap().putAll(transform(newContentlet));
            return clean(newContentlet);
        }).collect(Collectors.toList());
    }

    /**
     * To avoid caching issues we work on a copy
     * @param contentlet input contentlet
     * @return a copy contentlet
     */
    private Contentlet copy(final Contentlet contentlet) {
        final Contentlet newContentlet = new Contentlet();
        if (null != contentlet && null != contentlet.getMap()) {
            newContentlet.getMap().putAll(contentlet.getMap());
        }
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
    private void addConstants(final Contentlet contentlet){
        try {
            contentlet.getContentType().fields(ConstantField.class)
            .stream()
            .filter(f->f.values()!=null)
            .forEach(f-> contentlet.getMap().put(f.variable(), f.values()));
        } catch (Exception e) {
            Logger.warnAndDebug(getClass(),"Error Populating Constant Field: "  + e.getMessage(), e);
        }
    }
    
    /**
     * Use this method to add any additional property
     * @param contentlet Same contentlet with any additional property added
     */
    private void setAdditionalProperties(final Contentlet contentlet){
        try {
            final User modUser = userAPI.loadUserById(contentlet.getModUser());
            contentlet.getMap().put("modUserName", null != modUser ? modUser.getFullName() : NA );
            contentlet.getMap().put(Contentlet.WORKING_KEY, contentlet.isWorking());
            contentlet.getMap().put(Contentlet.LIVE_KEY, contentlet.isLive());
            contentlet.getMap().put(Contentlet.ARCHIVED_KEY, contentlet.isArchived());
            contentlet.getMap().put(Contentlet.LOCKED_KEY, contentlet.isLocked());

            final String urlMap = APILocator.getContentletAPI().getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), true);
            contentlet.getMap().put(ESMappingConstants.URL_MAP, urlMap);

        } catch (Exception e) {
            Logger.error(getClass(),"Error calculating modUser", e);
        }
    }

}