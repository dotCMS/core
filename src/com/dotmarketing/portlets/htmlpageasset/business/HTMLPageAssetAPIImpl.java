package com.dotmarketing.portlets.htmlpageasset.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HTMLPageAssetAPIImpl implements HTMLPageAssetAPI {

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
        
        field = new Field(URL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "^[A-Za-z0-9-_]+$", true, false, true);
        field.setVelocityVarName(URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(TEMPLATE_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, false, true, 4, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(TEMPLATE_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(ADVANCED_PROPERTIES_TAB_NAME, Field.FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, structure, false, false, false, 5, "", "", "", false, false, false);
        field.setVelocityVarName(ADVANCED_PROPERTIES_TAB);
        FieldFactory.saveField(field);
        
        field = new Field(SHOW_ON_MENU_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 6, "|true", "false", "", true, false, false);
        field.setVelocityVarName(SHOW_ON_MENU_FIELD);
        FieldFactory.saveField(field);

        field = new Field(SORT_ORDER_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, true, false, true, 7, "", "0", "", true, false, true);
        field.setVelocityVarName(SORT_ORDER_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(FRIENDLY_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, false, true, 8, "", "", "", true, false, true);
        field.setVelocityVarName(FRIENDLY_NAME_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(CACHE_TTL_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, 9, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')", "", "^[0-9]+$", true, false, true);
        field.setVelocityVarName(CACHE_TTL_FIELD);
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
        for(Contentlet cont : APILocator.getContentletAPI().search(
                "+live:"+live+" +deleted:"+deleted+
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
        newpage.setStructureInode(DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
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
        HibernateUtil.getSession().clear();
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(legacyident.getId());
        
        if(clive!=null) {
            Contentlet cclive = APILocator.getContentletAPI().checkin(clive, user, respectFrontEndPermissions);
            APILocator.getContentletAPI().publish(cclive, user, respectFrontEndPermissions);
            APILocator.getContentletAPI().isInodeIndexed(cclive.getInode(),false);
            APILocator.getContentletAPI().isInodeIndexed(cclive.getInode(),true);
        }
        
        Contentlet ccworking = APILocator.getContentletAPI().checkin(cworking, user, respectFrontEndPermissions);
        
        if(vInfo.getLiveInode()!=null && vInfo.getWorkingInode().equals(ccworking.getInode())) {
            APILocator.getContentletAPI().publish(ccworking, user, respectFrontEndPermissions);
        }
        
        for(MultiTree mt : multiTree) {
            MultiTreeFactory.saveMultiTree(mt);
        }
        
        if(perms!=null) {
            APILocator.getPermissionAPI().permissionIndividually(ccworking.getParentPermissionable(), ccworking, user, respectFrontEndPermissions);
            APILocator.getPermissionAPI().assignPermissions(perms, ccworking, user, respectFrontEndPermissions);
        }
        
        return fromContentlet(ccworking);
    }

}
