package com.dotmarketing.portlets.htmlpageasset.model;

import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;

public interface IHTMLPage extends Versionable,Permissionable {
    
    String getIdentifier();
    void setIdentifier(String identifier);
   
    long getCacheTTL();
    void setCacheTTL(long cacheTTL);

    String getSeoKeywords();
    void setSeoKeywords(String seoKeywords);

    String getSeoDescription();
    void setSeoDescription(String seoDescription);

    String getURI(Folder folder) throws DotStateException,DotDataException;
    String getURI() throws DotStateException, DotDataException;
    
    boolean isHttpsRequired();
    void setHttpsRequired(boolean httpsRequired);
    
    String getInode();
    void setInode(String inode);
    
    String getMetadata();
    void setMetadata(String metadata);
    
    String getPageUrl();
    void setPageUrl(String pageUrl);
    
    String getRedirect();
    void setRedirect(String redirect);

    String getTemplateId();
    void setTemplateId(String templateId);
    
    String getFriendlyName();
    void setFriendlyName(String friendlyName);
    
    String getTitle();
    void setTitle(String title);
    
    boolean isShowOnMenu();
    void setShowOnMenu(boolean showOnMenu);

    Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException;
    
    boolean isContent();
    int getMenuOrder();
}
