package com.dotmarketing.portlets.htmlpageasset.model;

import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;

public interface IHTMLPage {
   
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

    public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException;
        
}
