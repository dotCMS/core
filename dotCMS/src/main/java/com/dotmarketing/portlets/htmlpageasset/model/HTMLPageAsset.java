package com.dotmarketing.portlets.htmlpageasset.model;

import java.util.Map;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class represents an HTML Page as a {@link Contentlet}, which is the new 
 * version of Content Pages in dotCMS.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Aug 28, 2014
 *
 */
public class HTMLPageAsset extends Contentlet implements IHTMLPage {

    private static final long serialVersionUID = -4775734788059690797L;

    @Override
    public long getCacheTTL() {
        return Long.parseLong(getStringProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD));
    }

    @Override
    public void setCacheTTL(long cacheTTL) {
        setStringProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD, Long.toString(cacheTTL));
    }

    @Override
    public String getSeoKeywords() {
        return getStringProperty(HTMLPageAssetAPI.SEO_KEYWORDS_FIELD);
    }

    @Override
    public void setSeoKeywords(String seoKeywords) {
        setStringProperty(HTMLPageAssetAPI.SEO_KEYWORDS_FIELD, seoKeywords);
    }

    @Override
    public String getSeoDescription() {
        return getStringProperty(HTMLPageAssetAPI.SEO_DESCRIPTION_FIELD);
    }

    @Override
    public void setSeoDescription(String seoDescription) {
        setStringProperty(HTMLPageAssetAPI.SEO_DESCRIPTION_FIELD, seoDescription);
    }

    @Override
    public String getURI(Folder folder) throws DotStateException, DotDataException {
        return APILocator.getIdentifierAPI().find(folder.getIdentifier()).getURI()
                +getPageUrl();
    }

    @Override
    public String getURI() throws DotStateException, DotDataException {
        return APILocator.getIdentifierAPI().find(getIdentifier()).getURI();
    }

    @Override
    public boolean isHttpsRequired() {
        return getStringProperty(HTMLPageAssetAPI.HTTPS_REQUIRED_FIELD)!=null 
                && getStringProperty(HTMLPageAssetAPI.HTTPS_REQUIRED_FIELD).equals("true");
    }

    @Override
    public void setHttpsRequired(boolean httpsRequired) {
        setStringProperty(HTMLPageAssetAPI.HTTPS_REQUIRED_FIELD, httpsRequired ? "true" : "");
    }

    @Override
    public String getMetadata() {
        return getStringProperty(HTMLPageAssetAPI.PAGE_METADATA_FIELD);
    }

    @Override
    public void setMetadata(String metadata) {
        setStringProperty(HTMLPageAssetAPI.PAGE_METADATA_FIELD, metadata);
    }

    @Override
    public String getPageUrl() {
        return getStringProperty(HTMLPageAssetAPI.URL_FIELD);
    }

    @Override
    public void setPageUrl(String pageUrl) {

        setStringProperty(HTMLPageAssetAPI.URL_FIELD, pageUrl);
    }

    @Override
    public String getRedirect() {
        return getStringProperty(HTMLPageAssetAPI.REDIRECT_URL_FIELD);
    }

    @Override
    public void setRedirect(String redirect) {
        setStringProperty(HTMLPageAssetAPI.REDIRECT_URL_FIELD, redirect);
    }

    @Override
    public String getTemplateId() {
        return getStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD);
    }

    @Override
    public void setTemplateId(String templateId) {
        setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, templateId);
    }

    @Override
    public String getFriendlyName() {
        return getStringProperty(HTMLPageAssetAPI.FRIENDLY_NAME_FIELD);
    }

    @Override
    public void setFriendlyName(String friendlyName) {
        setStringProperty(HTMLPageAssetAPI.FRIENDLY_NAME_FIELD, friendlyName);
    }
    
    @Override
    public Map<String, Object> getMap() throws DotRuntimeException {        
        Map<String, Object> map = super.getMap();
        
        map.put("type", "htmlpage");
        
        map.put("title", getTitle());
        map.put("friendlyName", getFriendlyName());
        
        try {
            map.put("live", map.containsKey("live") ? map.get("live") : isLive());
            map.put("working", map.containsKey("working") ? map.get("working") : isWorking());
            map.put("deleted", map.containsKey("deleted") ? map.get("deleted") : isArchived());
            map.put("locked", map.containsKey("locked") ? map.get("locked") : isLocked());
        }
        catch(Exception ex) {
            throw new DotRuntimeException(ex.getMessage(),ex);
        }
        map.put("modDate", getModDate());
        map.put("modUser", getModUser());
        User modUser = null;
        try {
            modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
        } catch (NoSuchUserException e) {
            Logger.warn(this, "User " + this.getModUser() + " does not exist. Setting system as mod user.");
        } catch (DotDataException | DotSecurityException e1) {
            Logger.warn(this, "There was an issue when pulling " + this.getModUser() + " from DB. Continuing as system user.");
        } catch (Exception e2) {
            Logger.warn(this, "There was an unexpected problem with pulling user " + this.getModUser() + " from DB. Continuing as system user.");
        } 
        if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
            map.put("modUserName", modUser.getFullName());
        else
            map.put("modUserName", UserAPI.SYSTEM_USER_ID);
        
        map.put("metadata", getMetadata());
        map.put("httpsRequired", isHttpsRequired());
        map.put("redirect", getRedirect());

        /*
         For HTMLPages the URL should be get from the Identifier, it is a mistake to get
         the URL of a HTMLPage directly from the contentlet as the pages have multilanguage support,
         the same URL should be shared between all the page languages.
         */
        Object identifierObj = get( IDENTIFIER_KEY );
        if ( identifierObj != null ) {

            String identifierId = (String) identifierObj;
            try {

                Identifier identifier = APILocator.getIdentifierAPI().find( identifierId );
                if ( UtilMethods.isSet( identifier ) && UtilMethods.isSet( identifier.getId() ) ) {
                    map.put( "pageUrl", identifier.getAssetName() );
                }
            } catch ( DotDataException e ) {
                Logger.error( this.getClass(), "Unable to get Identifier with id [" + identifierId + "].", e );
            }
        } else {
            map.put( "pageUrl", getPageUrl() );
        }

        try {
            map.put("pageURI", this.getURI());
        } catch (Exception e) {
            Logger.debug(this, "Could not get URI : ", e);
            Logger.warn(this, String.format("Could not get URI for %s", this));
        }

        return map;
    }

    @Override
    public void setTitle(String title) {
        setStringProperty(HTMLPageAssetAPI.TITLE_FIELD, title);
    }    
    


    @Override
    public boolean isContent() {
        return true;
    }

    @Override
    public boolean isShowOnMenu() {
        String value=getStringProperty(HTMLPageAssetAPI.SHOW_ON_MENU_FIELD);
        return value!=null && value.contains("true");
    }

    @Override
    public void setShowOnMenu(boolean showOnMenu) {
        setStringProperty(HTMLPageAssetAPI.SHOW_ON_MENU_FIELD, showOnMenu ? "true" : "");
    }

    @Override
    public int getMenuOrder() {
        return (int)getSortOrder();
    }

}
