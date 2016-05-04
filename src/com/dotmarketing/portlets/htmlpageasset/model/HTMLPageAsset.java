package com.dotmarketing.portlets.htmlpageasset.model;

import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

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
            map.put("live", isLive());
            map.put("working", isWorking());
            map.put("deleted", isArchived());
            map.put("locked", isLocked());
        }
        catch(Exception ex) {
            throw new DotRuntimeException(ex.getMessage(),ex);
        }
        map.put("modDate", getModDate());
        map.put("modUser", getModUser());
        User modUser = null;
        try {
            modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
        if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
            map.put("modUserName", modUser.getFullName());
        else
            map.put("modUserName", "unknown");
        
        map.put("metadata", getMetadata());
        map.put("pageUrl", getPageUrl());
        map.put("httpsRequired", isHttpsRequired());
        map.put("redirect", getRedirect());
        return map;
    }

    @Override
    public void setTitle(String title) {
        setStringProperty(HTMLPageAssetAPI.TITLE_FIELD, title);
    }    
    
    @Override
    public String getTitle() {
        return getStringProperty(HTMLPageAssetAPI.TITLE_FIELD);
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
