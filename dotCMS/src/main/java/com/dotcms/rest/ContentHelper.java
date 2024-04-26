package com.dotcms.rest;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategy;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategyResolver;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 * Encapsulate helper method for the {@link com.dotcms.rest.ContentResource}
 * @author jsanca
 */
public class ContentHelper {

    private final MapToContentletPopulator mapToContentletPopulator;
    private final IdentifierAPI identifierAPI;
    private final BaseTypeToContentTypeStrategyResolver baseTypeToContentTypeStrategyResolver =
            BaseTypeToContentTypeStrategyResolver.getInstance();

    private static class SingletonHolder {
        private static final ContentHelper INSTANCE = new ContentHelper();
    }

    public static ContentHelper getInstance() {
        return ContentHelper.SingletonHolder.INSTANCE;
    }

    private ContentHelper() {
        this(  APILocator.getIdentifierAPI(),
                MapToContentletPopulator.INSTANCE);
    }

    @VisibleForTesting
    public ContentHelper(final IdentifierAPI identifierAPI,
                            final MapToContentletPopulator mapToContentletPopulator) {

        this.identifierAPI            = identifierAPI;
        this.mapToContentletPopulator = mapToContentletPopulator;
    }

    /**
     * Populate the contentlet from the map will all logic inside.
     * @param contentlet      {@link Contentlet}
     * @param stringObjectMap Map
     * @return Contentlet
     */
    public Contentlet populateContentletFromMap(final Contentlet contentlet,
                                                final Map<String, Object> stringObjectMap) {

        return this.mapToContentletPopulator.populate(contentlet, stringObjectMap);
    }


    /**
     * If the contentletMap does not have any content type assigned and a base type is set, tries to figure out the content type using the base type
     * @param contentletMap {@link Map}
     * @param user {@link User}
     */
    public void checkOrSetContentType(final Map<String, Object> contentletMap, final User user) {

        this.checkOrSetContentType(contentletMap, user, Collections.emptyList());
    }

    /**
     * If the contentletMap does not have any content type assigned and a base type is set, tries to figure out the content type using the base type
     * @param contentletMap {@link Map}
     * @param user {@link User}
     */
    public void checkOrSetContentType(final Map<String, Object> contentletMap, final User user, final List<File> binaryFiles) {

        if (!this.hasContentType(contentletMap) && contentletMap.containsKey(Contentlet.BASE_TYPE_KEY)) {

            final String baseType = contentletMap.get(Contentlet.BASE_TYPE_KEY).toString();
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (UtilMethods.isSet(baseType) && null != request) {

                this.tryToSetContentType(contentletMap, user, baseType, request, binaryFiles);
            }
        }
    }

    private boolean hasContentType (final Map<String, Object> contentletMap) {
        return  contentletMap.containsKey(Contentlet.CONTENT_TYPE_KEY)    ||
                contentletMap.containsKey(Contentlet.STRUCTURE_INODE_KEY) ||
                contentletMap.containsKey(Contentlet.STRUCTURE_NAME_KEY);
    }


    private void tryToSetContentType(final Map<String, Object> contentletMap,
                                     final User user, final String baseType, final HttpServletRequest request,
                                     final List<File> binaryFiles) {

        final Host host = Try.of(()-> WebAPILocator.getHostWebAPI().getCurrentHost(request)).getOrNull();
        final BaseContentType baseContentType = BaseContentType.getBaseContentType(baseType);
        final Optional<BaseTypeToContentTypeStrategy> typeStrategy =
                this.baseTypeToContentTypeStrategyResolver.get(baseContentType);

        if (null != host && typeStrategy.isPresent()) {

            final String sessionId = request!=null && request.getSession(false)!=null? request.getSession().getId() : null;
            final Optional<ContentType> contentTypeOpt = typeStrategy.get().apply(baseContentType,
                    Map.of("user", user, "host", host,
                            "contentletMap", contentletMap, "binaryFiles", binaryFiles,
                            "accessingList", Arrays.asList(user.getUserId(),
                                    APILocator.getTempFileAPI().getRequestFingerprint(request), sessionId)));

            if (contentTypeOpt.isPresent()) {

                Logger.debug(this, ()-> "For the base type: " + baseType + " resolved the content type: "
                        + contentTypeOpt.get().variable());
                contentletMap.put(Contentlet.CONTENT_TYPE_KEY, contentTypeOpt.get().variable());
            } else{
                final String errorMsg = Try.of(() -> LanguageUtil.get(user.getLocale(),
                        "contentType.not.resolved.baseType", user.getUserId(), baseType)).getOrElse("Content-Type could not be resolved based on base type");
                throw new DotContentletValidationException(errorMsg);
            }
        }
    }

    /**
     * Serves as an Entry point to the DotTransformerBuilder
     * @See DotTransformerBuilder
     * @param contentlet {@link Contentlet} original contentlet to hydrate, won't be modified.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes, otherwise will the same instance
     */
    public Contentlet hydrateContentlet(final Contentlet contentlet) {
        return new DotTransformerBuilder().contentResourceOptions(false).content(contentlet).build().hydrate().get(0);
    } // hydrateContentlet.

    /**
     * Gets if possible the url associated to this asset contentlet
     * @param contentlet {@link Contentlet}
     * @return String the url, null if can not get
     */
    public String getUrl (final Contentlet contentlet) {

        return this.getUrl(contentlet.getMap().get( ContentletForm.IDENTIFIER_KEY ));
    } // getUrl.


    /**
     * Gets if possible the url associated to this asset identifier
     * @param identifierObj {@link Object}
     * @return String the url, null if can not get
     */
    public String getUrl ( final Object identifierObj) {

        String url = null;
        if ( identifierObj != null ) {
            try {

                final Identifier identifier = this.identifierAPI.find(  (String) identifierObj );
                url = ( UtilMethods.isSet( identifier ) && UtilMethods.isSet( identifier.getId() ) )?
                        identifier.getURI():null;
            } catch ( DotDataException e ) {
                Logger.error( this.getClass(), "Unable to get Identifier with id [" + identifierObj + "]. Could not get the url", e );
            }
        }

        return url;
    } // getUrl.


} // E:O:F:ContentHelper.
