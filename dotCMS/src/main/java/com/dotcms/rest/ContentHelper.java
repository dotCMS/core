package com.dotcms.rest;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Map;

/**
 * Encapsulate helper method for the {@link com.dotcms.rest.ContentResource}
 * @author jsanca
 */
public class ContentHelper {

    private final MapToContentletPopulator mapToContentletPopulator;
    private final IdentifierAPI identifierAPI;

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
     * Serves as an Entry point to the ContentletToMapTransformer
     * @See ContentletToMapTransformer
     * @param contentlet {@link Contentlet} original contentlet to hydrate, won't be modified.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes, otherwise will the same instance
     */
    public Contentlet hydrateContentlet(final Contentlet contentlet) {
       return new ContentletToMapTransformer(contentlet).hydrate().get(0);
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
