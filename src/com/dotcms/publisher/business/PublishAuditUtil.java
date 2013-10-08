package com.dotcms.publisher.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.io.StringWriter;

public class PublishAuditUtil {

    private static PublishAuditUtil instance = null;

    public String getTitle ( String assetType, String id ) {
        StringWriter sw = new StringWriter();

        try {

            User user = APILocator.getUserAPI().getSystemUser();

            if ( "contentlet".equals( assetType ) || "host".equals( assetType ) ) {
                sw.append( APILocator.getContentletAPI().findContentletByIdentifier( id, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false ).getTitle() );
            } else if ( "folder".equals( assetType ) ) {
                sw.append( APILocator.getFolderAPI().find( id, user, false ).getName() );
            } else if ( "osgi".equals( assetType ) ) {
                sw.append( id );
            } else if ( "user".equals( assetType ) ) {
                sw.append( id.replace( "user_", "" ) );
            } else if ( "structure".equals( assetType ) ) {
                sw.append( APILocator.getStructureAPI().find( id, user ).getName() );
            } else if ( "template".equals( assetType ) ) {
                sw.append( APILocator.getTemplateAPI().findWorkingTemplate( id, user, false ).getTitle() );
            } else if ( "containers".equals( assetType ) ) {
                sw.append( APILocator.getContainerAPI().getWorkingContainerById( id, user, false ).getTitle() );
            } else if ( "htmlpage".equals( assetType ) ) {
                sw.append( APILocator.getHTMLPageAPI().loadWorkingPageById( id, user, false ).getTitle() );
            } else if ( "category".equals( assetType ) ) {
                sw.append( APILocator.getCategoryAPI().find( id, user, false ).getCategoryName() );
            } else if ( "links".equals( assetType ) ) {
                sw.append( APILocator.getMenuLinkAPI().findWorkingLinkById( id, user, false ).getTitle() );
            } else {
                sw.append( assetType );
            }
        } catch ( Exception e ) {
            Logger.debug( this.getClass(), "unable to get title for asset " + assetType + " " + id );
            sw.append( assetType );
        }
        return sw.toString();
    }

    /**
     * Searches and returns for a given Identifier a Contentlet using the default language
     *
     * @param identifier
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public Contentlet findContentletByIdentifier ( String identifier ) throws DotSecurityException, DotDataException {

        User user = APILocator.getUserAPI().getSystemUser();

        //Get the Contentlet for the default language
        return APILocator.getContentletAPI().findContentletByIdentifier( identifier, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false );
    }

    public static PublishAuditUtil getInstance () {
        if ( instance == null ) {
            instance = new PublishAuditUtil();
        }
        return instance;
    }

}
