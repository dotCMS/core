package com.dotcms.rest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Encapsulate helper method for the {@link com.dotcms.rest.ContentResource}
 * @author jsanca
 */
class ContentHelper {

    private final IdentifierAPI identifierAPI;

    private static class SingletonHolder {
        private static final ContentHelper INSTANCE = new ContentHelper();
    }

    public static ContentHelper getInstance() {
        return ContentHelper.SingletonHolder.INSTANCE;
    }

    private ContentHelper() {
        this(  APILocator.getIdentifierAPI() );
    }

    @VisibleForTesting
    protected ContentHelper(final IdentifierAPI identifierAPI) {
        this.identifierAPI = identifierAPI;
    }

    /**
     * Adds needed things that are not coming by default from the api to the contentlet.
     * If there is anything new to add, returns copy with the new attributes inside, otherwise returns the same instance.
     * @param contentlet {@link Contentlet} original contentlet to hydrate, won't be modified.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes, otherwise will the same instance
     */
    public Contentlet hydrateContentLet (final Contentlet contentlet) {

        Contentlet newContentlet = contentlet;
        if (null != contentlet && !contentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD)) {

            final String url = this.getUrl(contentlet);
            if (null != url) {
                // making a copy to avoid issues on modifying cache objects.
                newContentlet = new Contentlet();
                newContentlet.getMap().putAll(contentlet.getMap());
                newContentlet.getMap().put(HTMLPageAssetAPI.URL_FIELD, url);
            }
        }

        return newContentlet;
    } // hydrateContentLet.

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
                        identifier.getAssetName():null;
            } catch ( DotDataException e ) {
                Logger.error( this.getClass(), "Unable to get Identifier with id [" + identifierObj + "]. Could not get the url", e );
            }
        }

        return url;
    } // getUrl.


} // E:O:F:ContentHelper.
