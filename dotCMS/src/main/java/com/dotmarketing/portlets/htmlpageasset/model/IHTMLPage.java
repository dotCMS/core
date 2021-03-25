package com.dotmarketing.portlets.htmlpageasset.model;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.storage.FileStorageAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.Serializable;
import java.util.Map;

import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * Represents an HTML page in dotCMS. As of version 3.1, HTML page are not 
 * considered as {@link WebAsset} objects, but as {@link Contentlet} objects.
 * 
 * @author Jorge Urdaneta
 * @version 1.1
 * @since 08-28-2014
 *
 */
public interface IHTMLPage extends Serializable, Versionable, Permissionable,
		Treeable, Ruleable {

    String getHost();
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

    /**
     * We no longer need to set or get Metadata as String
     * @deprecated
     *   Please use instead {@link com.dotcms.storage.FileMetadataAPI#getMetadata(Contentlet, Field)}
     *   or {@link Contentlet#getBinaryMetadata(Field)}
     * @return
     */
    @Deprecated
    String getMetadata();

    /**
     * We no longer need to set or get Metadata as String
     * @deprecated
     *   Please use instead {@link com.dotcms.storage.FileMetadataAPI#getMetadata(Contentlet, Field)}
     *   or {@link Contentlet#getBinaryMetadata(Field)}
     * @return
     */
    @Deprecated
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
    
    long getLanguageId();
    
}
