package com.dotcms.publisher.business;

import com.dotcms.publisher.util.PusheableAsset;
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

/**
 * Utility methods for common operations related to the Push Publish process.
 * 
 * @author Will Ezell
 * @version 1.0
 * @since Mar 13, 2013
 *
 */
public class PublishAuditUtil {

    private static PublishAuditUtil instance = null;

	/**
	 * Returns the title of an asset. This title depends on the type of asset,
	 * since it depends on the context.
	 * 
	 * @param assetType
	 *            - Type of asset: Contentlet, folder, template, etc.
	 * @param id
	 *            - The Identifier of the asset.
	 * @return The human-readable name of the asset.
	 */
    public String getTitle ( String assetType, String id ) {
        StringWriter sw = new StringWriter();

        try {

            User user = APILocator.getUserAPI().getSystemUser();

            if ( PusheableAsset.CONTENTLET.getType().equals( assetType ) || PusheableAsset.SITE.getType().equals( assetType ) ) {
                sw.append( findContentletByIdentifier(id).getTitle() );
            } else if ( PusheableAsset.FOLDER.getType().equals( assetType ) ) {
                sw.append( APILocator.getFolderAPI().find( id, user, false ).getName() );
            } else if ( PusheableAsset.OSGI.getType().equals( assetType ) ) {
                sw.append( id );
            } else if ( PusheableAsset.USER.getType().equals( assetType ) ) {
                sw.append( id.replace( "user_", "" ) );
            } else if ( PusheableAsset.CONTENT_TYPE.getType().equals( assetType ) ) {
                sw.append( APILocator.getStructureAPI().find( id, user ).getName() );
            } else if ( PusheableAsset.TEMPLATE.getType().equals( assetType ) ) {
                sw.append( APILocator.getTemplateAPI().findWorkingTemplate( id, user, false ).getTitle() );
            } else if ( PusheableAsset.CONTAINER.getType().equals( assetType ) ) {
                sw.append( APILocator.getContainerAPI().getWorkingContainerById( id, user, false ).getTitle() );
            } else if ( PusheableAsset.HTMLPAGE.getType().equals( assetType ) ) {
                sw.append( APILocator.getHTMLPageAPI().loadWorkingPageById( id, user, false ).getTitle() );
            } else if ( PusheableAsset.CATEGORY.getType().equals( assetType ) ) {
                sw.append( APILocator.getCategoryAPI().find( id, user, false ).getCategoryName() );
            } else if ( PusheableAsset.LINK.getType().equals( assetType ) ) {
                sw.append( APILocator.getMenuLinkAPI().findWorkingLinkById( id, user, false ).getTitle() );
            } else if (PusheableAsset.LANGUAGE.getType().equals(assetType)) {
            	Language language = APILocator.getLanguageAPI().getLanguage(id);
                sw.append(language.getLanguage() + " - " + language.getCountry());
            } else if (PusheableAsset.RULE.getType().equals(assetType)) {
                sw.append(APILocator.getRulesAPI().getRuleById(id, user, false).getName());
            } else {
                sw.append( assetType );
            }
        } catch ( Exception e ) {
            Logger.debug( this.getClass(), "Unable to get title for asset " + assetType + " " + id );
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

	/**
	 * Returns a single instance of this class.
	 * 
	 * @return An instance of this class.
	 */
    public static PublishAuditUtil getInstance () {
        if ( instance == null ) {
            instance = new PublishAuditUtil();
        }
        return instance;
    }

}
