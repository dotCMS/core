package com.dotcms.publisher.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.io.StringWriter;
import java.util.List;

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
     * Searches and returns for a given Identifier a Contentlet giving priority to the default language
     *
     * @param identifier
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public Contentlet findContentletByIdentifier ( String identifier ) throws DotSecurityException, DotDataException {

        User user = APILocator.getUserAPI().getSystemUser();

        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( identifier );
        if ( contentletIdentifier == null ) {
            throw new DotContentletStateException( "Unable to find Contentle with Identifier [" + identifier + "]" );
        }

        //Getting the default language
        Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        /*
        We may added a contentlet that exist only in a NON default language so
        assuming the content will always exist in the default language is wrong.
         */
        List<Contentlet> allLanguages = APILocator.getContentletAPI().search( "+identifier:" + identifier, 0, 0, "moddate", user, false );

        /*
        For display purposes we are trying to return the contentlet with the default language, if
        nothing found for the default language just return the first one.
         */
        Contentlet foundContentlet = null;
        for ( Contentlet contentlet : allLanguages ) {
            if ( contentlet.getLanguageId() == defaultLanguage.getId() ) {
                foundContentlet = contentlet;
            }
        }
        if ( foundContentlet == null ) {
            foundContentlet = allLanguages.get( 0 );
        }

        return foundContentlet;
    }

    public static PublishAuditUtil getInstance () {
        if ( instance == null ) {
            instance = new PublishAuditUtil();
        }
        return instance;
    }

}
