package com.dotmarketing.portlets.htmlpageasset.model;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.util.Config;

public class HTMLPageAsset extends Contentlet implements IHTMLPage {
    private static final long serialVersionUID = -4775734788059690797L;

    @Override
    public long getCacheTTL() {
        return getLongProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD);
    }

    @Override
    public void setCacheTTL(long cacheTTL) {
        setLongProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD, cacheTTL);
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
                +getPageUrl()+"."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION","html");
    }

    @Override
    public String getURI() throws DotStateException, DotDataException {
        return APILocator.getIdentifierAPI().find(getIdentifier()).getURI();
    }

    @Override
    public boolean isHttpsRequired() {
        return getBoolProperty(HTMLPageAssetAPI.HTTPS_REQUIRED_FIELD);
    }

    @Override
    public void setHttpsRequired(boolean httpsRequired) {
        setBoolProperty(HTMLPageAssetAPI.HTTPS_REQUIRED_FIELD, httpsRequired);
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
    
}
