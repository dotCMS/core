package com.dotmarketing.portlets.htmlpageasset.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageFactoryImpl;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HTMLPageAssetAPIImpl implements HTMLPageAssetAPI {

    public static final String DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD = "defaultHTMLPageAssetStructure";

    @Override
    public void createHTMLPageAssetBaseFields(Structure structure) throws DotDataException, DotStateException {
        if (structure == null || !InodeUtils.isSet(structure.getInode())) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that doesn't exist");
        }
        if (structure.getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that is not of htmlpage asset type");
        }
        
        Field field = new Field(TITLE_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, 1, "$velutil.mergeTemplate('/static/htmlpage_assets/title_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(TITLE_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, 2, "", "", "", true, false, true);
        field.setVelocityVarName(HOST_FOLDER_FIELD);
        FieldFactory.saveField(field);        
        
        field = new Field(URL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "", true, false, true);
        field.setVelocityVarName(URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(CACHE_TTL_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, 4, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')", "", "^[0-9]+$", true, false, true);
        field.setVelocityVarName(CACHE_TTL_FIELD);
        FieldFactory.saveField(field);
        
        
        field = new Field(TEMPLATE_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, false, true, 5, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(TEMPLATE_FIELD);
        FieldFactory.saveField(field);
        
        
        
        
        
        field = new Field(ADVANCED_PROPERTIES_TAB_NAME, Field.FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, structure, false, false, false, 6, "", "", "", false, false, false);
        field.setVelocityVarName(ADVANCED_PROPERTIES_TAB);
        FieldFactory.saveField(field);
        
        field = new Field(SHOW_ON_MENU_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 7, "|true", "false", "", true, false, false);
        field.setVelocityVarName(SHOW_ON_MENU_FIELD);
        FieldFactory.saveField(field);

        field = new Field(SORT_ORDER_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, true, false, true, 8, "", "0", "", true, false, true);
        field.setVelocityVarName(SORT_ORDER_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(FRIENDLY_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, false, true, 9, "", "", "", true, false, true);
        field.setVelocityVarName(FRIENDLY_NAME_FIELD);
        FieldFactory.saveField(field);
        

        
        field = new Field(REDIRECT_URL_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, false, true, true, 10, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/redirect_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(REDIRECT_URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(HTTPS_REQUIRED_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 11, "|true", "false", "", true, false, false);
        field.setVelocityVarName(HTTPS_REQUIRED_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 12, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_DESCRIPTION_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_KEYWORDS_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 13, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_KEYWORDS_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(PAGE_METADATA_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 14, "", "", "", true, false, true);
        field.setVelocityVarName(PAGE_METADATA_FIELD);
        FieldFactory.saveField(field);
                
    }

    @Override
    public Template getTemplate(IHTMLPage page, boolean preview) throws DotDataException, DotSecurityException {
        if (preview) 
            return APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
        else
            return APILocator.getTemplateAPI().findLiveTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
    }

    @Override
    public Host getParentHost(IHTMLPage page) throws DotDataException, DotStateException, DotSecurityException {
        return APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(page).getHostId(), APILocator.getUserAPI().getSystemUser(), false);
    }

    @Override
    public HTMLPageAsset fromContentlet(Contentlet con) {
        if (con == null || con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
            throw new DotStateException("Contentlet : " + con.getInode() + " is not a pageAsset");
        }

        HTMLPageAsset pa=new HTMLPageAsset();
        pa.setStructureInode(con.getStructureInode());
        try {
            APILocator.getContentletAPI().copyProperties((Contentlet) pa, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Page Copy Failed", e);
        }
        pa.setHost(con.getHost());
        if(UtilMethods.isSet(con.getFolder())){
            try{
                Identifier ident = APILocator.getIdentifierAPI().find(con);
                User systemUser = APILocator.getUserAPI().getSystemUser();
                Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
                Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUser, false);
                pa.setFolder(folder.getInode());
            }catch(Exception e){
                Logger.warn(this, "Unable to convert contentlet to page asset " + con, e);
            }
        }
        return pa;
    }

    @Override
    public List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        List<IHTMLPage> pages=new ArrayList<IHTMLPage>();
        
        String liveWorkingDeleted = (live) ? " +live:true " :  (deleted)  ?" +working:true +deleted:true " : " +working:true -deleted:true";

        for(Contentlet cont : APILocator.getContentletAPI().search(
        		

        		
        		liveWorkingDeleted + 
                    (parent instanceof Folder ? 
                            " +conFolder:"+((Folder)parent).getInode()
                         :  ((parent instanceof Host) ? 
                                 " +conFolder:SYSTEM_FOLDER +conHost:"+((Host)parent).getIdentifier() : ""))
                    +" +structureType:"+Structure.STRUCTURE_TYPE_HTMLPAGE, 
                -1, 0, "modDate asc", user, respectFrontEndRoles)) {
            pages.add(fromContentlet(cont));
        }
        return pages;
    }
    
    @Override
    public List<IHTMLPage> getLiveHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, true, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getWorkingHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getDeletedHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, true, user, respectFrontEndRoles);
    }

    @Override
    public Folder getParentFolder(IHTMLPage htmlPage) throws DotDataException, DotSecurityException {
        Identifier ident = APILocator.getIdentifierAPI().find(htmlPage.getIdentifier());
        if(ident.getParentPath().equals("/")) {
            return APILocator.getFolderAPI().findSystemFolder();
        }
        else {
            return APILocator.getFolderAPI().findFolderByPath(
                    ident.getParentPath(), APILocator.getHostAPI().find(
                            ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false), 
                            APILocator.getUserAPI().getSystemUser(), false);
        }
    }

    protected HTMLPageAsset copyLegacyData(HTMLPage legacyPage, User user, boolean respectFrontEndPermissions) throws DotStateException, DotDataException, DotSecurityException {
        Identifier legacyident=APILocator.getIdentifierAPI().find(legacyPage);
        HTMLPageAsset newpage=new HTMLPageAsset();
        newpage.setStructureInode(getHostDefaultPageType(legacyident.getHostId()));
        newpage.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        newpage.setTitle(legacyPage.getTitle());
        newpage.setFriendlyName(legacyPage.getFriendlyName());
        newpage.setHttpsRequired(legacyPage.isHttpsRequired());
        newpage.setTemplateId(legacyPage.getTemplateId());
        newpage.setSeoDescription(legacyPage.getSeoDescription());
        newpage.setSeoKeywords(legacyPage.getSeoKeywords());
        newpage.setInode(legacyPage.getInode());
        newpage.setIdentifier(legacyPage.getIdentifier());
        newpage.setHost(legacyident.getHostId());
        newpage.setFolder(APILocator.getFolderAPI().findFolderByPath(
                legacyident.getParentPath(), legacyident.getHostId(), user, respectFrontEndPermissions).getInode());
        newpage.setPageUrl(legacyPage.getPageUrl());
        newpage.setCacheTTL(legacyPage.getCacheTTL());
        newpage.setMetadata(legacyPage.getMetadata());
        newpage.setSortOrder(legacyPage.getSortOrder());
        newpage.setShowOnMenu(legacyPage.isShowOnMenu());
        newpage.setModUser(legacyPage.getModUser());
        newpage.setModDate(legacyPage.getModDate());
        return newpage;
    }
    
    @Override
    public HTMLPageAsset migrateLegacyPage(HTMLPage legacyPage, User user, boolean respectFrontEndPermissions) throws Exception {
        Identifier legacyident=APILocator.getIdentifierAPI().find(legacyPage);
        VersionInfo vInfo=APILocator.getVersionableAPI().getVersionInfo(legacyident.getId());
        
        List<HTMLPageAsset> versions=new ArrayList<HTMLPageAsset>(); 
        
        HTMLPage working=(HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(legacyident, user, respectFrontEndPermissions);
        HTMLPageAsset cworking=copyLegacyData(working, user, respectFrontEndPermissions), clive=null;
        if(vInfo.getLiveInode()!=null && !vInfo.getLiveInode().equals(vInfo.getWorkingInode())) {
            HTMLPage live=(HTMLPage) APILocator.getVersionableAPI().findLiveVersion(legacyident, user, respectFrontEndPermissions);
            clive=copyLegacyData(working, user, respectFrontEndPermissions);
        }
        
        List<Permission> perms=null;
        if(!APILocator.getPermissionAPI().isInheritingPermissions(legacyPage)) {
            perms = APILocator.getPermissionAPI().getPermissions(legacyPage, true, true, true);
        }
        
        List<MultiTree> multiTree = MultiTreeFactory.getMultiTree(working.getIdentifier());
        
        APILocator.getHTMLPageAPI().delete(working, user, respectFrontEndPermissions);
        PageServices.invalidate(working);
        HibernateUtil.getSession().clear();
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(legacyident.getId());
        
        if(clive!=null) {
            Contentlet cclive = APILocator.getContentletAPI().checkin(clive, user, respectFrontEndPermissions);
            APILocator.getContentletAPI().publish(cclive, user, respectFrontEndPermissions);
        }
        
        Contentlet ccworking = APILocator.getContentletAPI().checkin(cworking, user, respectFrontEndPermissions);
        
        if(vInfo.getLiveInode()!=null && vInfo.getWorkingInode().equals(ccworking.getInode())) {
            APILocator.getContentletAPI().publish(ccworking, user, respectFrontEndPermissions);
        }
        
        for(MultiTree mt : multiTree) {
            MultiTreeFactory.saveMultiTree(mt);
        }
        
        APILocator.getPermissionAPI().removePermissions(ccworking);
        if(perms!=null) {
            APILocator.getPermissionAPI().permissionIndividually(ccworking.getParentPermissionable(), ccworking, user, respectFrontEndPermissions);
            APILocator.getPermissionAPI().assignPermissions(perms, ccworking, user, respectFrontEndPermissions);
        }
        
        return fromContentlet(ccworking);
    }
    
    @Override
    public String getHostDefaultPageType(String hostId) throws DotDataException, DotSecurityException {
        return getHostDefaultPageType(APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false));
    }
    
    @Override
    public String getHostDefaultPageType(Host host) {
        Field ff=host.getStructure().getField(DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD);
        if(ff!=null && InodeUtils.isSet(ff.getInode())) {
            String stInode= ff.getFieldType().equals(Field.FieldType.CONSTANT.toString()) ? ff.getValues()
                    : host.getStringProperty(ff.getVelocityVarName());
            if(stInode!=null && UtilMethods.isSet(stInode)) {
                Structure type=StructureCache.getStructureByInode(stInode);
                if(type!=null && InodeUtils.isSet(type.getInode())) {
                    return stInode;
                }
            }
        }
        return DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE;
    }

    @Override
    public boolean rename(HTMLPageAsset page, String newName, User user) throws DotDataException, DotSecurityException {
        Identifier sourceIdent=APILocator.getIdentifierAPI().find(page);
        Host host=APILocator.getHostAPI().find(sourceIdent.getHostId(), user, false);
        Identifier targetIdent=APILocator.getIdentifierAPI().find(host, 
                sourceIdent.getParentPath()+newName);
        if(targetIdent==null || !InodeUtils.isSet(targetIdent.getId())) {
            Contentlet cont=APILocator.getContentletAPI().checkout(page.getInode(), user, false);
            cont.setStringProperty(URL_FIELD, newName);
            cont=APILocator.getContentletAPI().checkin(cont, user, false);
            if(page.isLive()) {
                APILocator.getContentletAPI().publish(cont, user, false);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean move(HTMLPageAsset page, Folder parent, User user) throws DotDataException, DotSecurityException {
        return move(page,APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(parent).getHostId(),user,false), parent, user);
    }

    @Override
    public boolean move(HTMLPageAsset page, Host host, User user) throws DotDataException, DotSecurityException {
        return move(page,host,APILocator.getFolderAPI().findSystemFolder(),user);
    }
    
    public boolean move(HTMLPageAsset page, Host host, Folder parent, User user) throws DotDataException, DotSecurityException {
        Identifier sourceIdent=APILocator.getIdentifierAPI().find(page);
        Identifier targetFolderIdent=APILocator.getIdentifierAPI().find(parent);
        Identifier targetIdent=APILocator.getIdentifierAPI().find(host,targetFolderIdent.getURI()+sourceIdent.getAssetName());
        if(targetIdent==null || !InodeUtils.isSet(targetIdent.getId())) {
            Contentlet cont=APILocator.getContentletAPI().checkout(page.getInode(), user, false);
            cont.setFolder(parent.getInode());
            cont.setHost(host.getIdentifier());
            cont=APILocator.getContentletAPI().checkin(cont, user, false);
            if(page.isLive()) {
                APILocator.getContentletAPI().publish(cont, user, false);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Returns the ids for Pages whose Templates, Containers, or Content 
     * have been modified between 2 dates even if the page hasn't been modified
     * @param host Must be set
     * @param pattern url pattern e.g., /some/path/*
     * @param include the pattern is to include or exclude
     * @param startDate Must be set
     * @param endDate Must be Set
     * @return
     */
    @Override
    public List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern,boolean include,Date startDate, Date endDate) {

        Set<String> ret = new HashSet<String>();
        
        String likepattern=RegEX.replaceAll(pattern, "%", "\\*");
        
        String concat;
        if(DbConnectionFactory.isMySql()){
            concat=" concat(ii.parent_path, ii.asset_name) ";
        }else if (DbConnectionFactory.isMsSql()) {
            concat=" (ii.parent_path + ii.asset_name) ";
        }else {
            concat=" (ii.parent_path || ii.asset_name) ";
        }
        
        Structure st=StructureCache.getStructureByInode(DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        Field tf=st.getFieldVar(TEMPLATE_FIELD);
        
        // htmlpage with modified template
        StringBuilder bob = new StringBuilder();
        DotConnect dc = new DotConnect();
        bob.append("SELECT ii.id as pident ")
        .append("from identifier ii ")
        .append("join contentlet cc on (cc.identifier = ii.id) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join template_version_info tvi on (cc.").append(tf.getFieldContentlet()).append(" = tvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and tvi.version_ts >= ? and tvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get pages asset with modified template. sql:"+bob,e);
        }
        
        // htmlpage with modified containers
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident ")
        .append("from identifier ii " )
        .append("join contentlet cc on (ii.id=cc.identifier) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join template_containers tc on (cc.").append(tf.getFieldContentlet()).append(" = tc.template_id) ")
        .append("join container_version_info cvi on (tc.container_id = cvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and cvi.version_ts >= ? and cvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get modified containers under page asset sql:"+bob,e);
        }
        
        // htmlpages with modified content
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident ")
        .append("from contentlet_version_info hvi join identifier ii on (hvi.identifier=ii.id) " )
        .append("join contentlet cc on (ii.id=cc.identifier) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join multi_tree mt on (hvi.identifier = mt.parent1) ")
        .append("join contentlet_version_info cvi on (mt.child = cvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and cvi.version_ts >= ? and cvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get mdified content under page asset sql:"+bob,e);
        }
        
        // htmlpage modified itself
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident from contentlet cc ")
        .append("join identifier ii on (ii.id=cc.identifier) ")
        .append("join contentlet_version_info vi on (vi.identifier=ii.id) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and vi.version_ts >= ? and vi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get modified page assets sql:"+bob,e);
        }
        
        return new ArrayList<String>(ret);
    }


}
