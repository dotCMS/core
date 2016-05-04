package com.dotmarketing.portlets.htmlpageasset.business;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface HTMLPageAssetAPI {
    
    static final String URL_FIELD="url";
    static final String URL_FIELD_NAME="Url";
    
    static final String HOST_FOLDER_FIELD="hostfolder";
    static final String HOST_FOLDER_FIELD_NAME="Host or Folder";
    
    static final String TITLE_FIELD="title";
    static final String TITLE_FIELD_NAME="Title";
    
    static final String FRIENDLY_NAME_FIELD="friendlyname";
    static final String FRIENDLY_NAME_FIELD_NAME="Friendly Name";
    
    static final String SORT_ORDER_FIELD = "sortOrder";
    static final String SORT_ORDER_FIELD_NAME = "Sort Order";
    
    static final String SHOW_ON_MENU_FIELD = "showOnMenu";
    static final String SHOW_ON_MENU_FIELD_NAME = "Show On Menu";
    
    static final String REDIRECT_URL_FIELD="redirecturl";
    static final String REDIRECT_URL_FIELD_NAME="Redirect URL";
    
    static final String HTTPS_REQUIRED_FIELD="httpsreq";
    static final String HTTPS_REQUIRED_FIELD_NAME="HTTPS Required";
    
    static final String CACHE_TTL_FIELD="cachettl";
    static final String CACHE_TTL_FIELD_NAME="Cache TTL";
    
    static final String SEO_DESCRIPTION_FIELD="seodescription";
    static final String SEO_DESCRIPTION_FIELD_NAME="SEO Description";
    
    static final String SEO_KEYWORDS_FIELD="seokeywords";
    static final String SEO_KEYWORDS_FIELD_NAME="SEO Keywords";
    
    static final String PAGE_METADATA_FIELD="pagemetadata";
    static final String PAGE_METADATA_FIELD_NAME="Page Metadata"; 
    
    static final String TEMPLATE_FIELD="template";
    static final String TEMPLATE_FIELD_NAME="Template";
    
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_NAME="HTMLPage Asset";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_DESCRIPTION="Default Structure for Pages";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME="htmlpageasset";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE="c541abb1-69b3-4bc5-8430-5e09e5239cc8";
    
    static final String ADVANCED_PROPERTIES_TAB="advancedtab";
    static final String ADVANCED_PROPERTIES_TAB_NAME="Advanced Properties";
    
    void createHTMLPageAssetBaseFields(Structure structure) throws DotDataException, DotStateException;
    
    Template getTemplate(IHTMLPage page, boolean preview) throws DotDataException, DotSecurityException;
    Host getParentHost(IHTMLPage page) throws DotDataException, DotStateException, DotSecurityException;

    HTMLPageAsset fromContentlet(Contentlet content);

    List<IHTMLPage> getLiveHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getWorkingHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getDeletedHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    Folder getParentFolder(IHTMLPage htmlPage) throws DotDataException, DotSecurityException;
    
    HTMLPageAsset migrateLegacyPage(HTMLPage legacyPage, User user, boolean respectFrontEndPermissions)  throws Exception;

    String getHostDefaultPageType(Host host);

    String getHostDefaultPageType(String hostId) throws DotDataException, DotSecurityException;

    boolean rename(HTMLPageAsset page, String newName, User user) throws DotDataException, DotSecurityException;

    boolean move(HTMLPageAsset page, Folder parent, User user)throws DotDataException, DotSecurityException;
    
    boolean move(HTMLPageAsset page, Host host, User user)throws DotDataException, DotSecurityException;

    List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern, boolean include, Date startDate, Date endDate);
}
