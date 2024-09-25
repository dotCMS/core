package com.dotmarketing.portlets.htmlpageasset.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation class for the {@link HTMLPageAssetAPI} interface.
 *
 * @author Jorge Urdaneta
 * @since Aug 28th, 2014
 */
public class HTMLPageAssetAPIImpl implements HTMLPageAssetAPI {

    public static final Lazy<String> CMS_INDEX_PAGE = Lazy.of(() -> Config.getStringProperty(
            "CMS_INDEX_PAGE", "index"));
    public static final Lazy<Boolean> DEFAULT_PAGE_TO_DEFAULT_LANGUAGE =
            Lazy.of(() -> Config.getBooleanProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true));

    public static final String DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD = "defaultHTMLPageAssetStructure";

    private final PermissionAPI permissionAPI;
    private final IdentifierAPI identifierAPI;
    private final UserAPI userAPI;
    private final VersionableAPI versionableAPI;
    private final ContentletAPI contentletAPI;
    private final SystemEventsAPI systemEventsAPI;

    private final LanguageAPI languageAPI;
    public HTMLPageAssetAPIImpl() {
        permissionAPI = APILocator.getPermissionAPI();
        identifierAPI = APILocator.getIdentifierAPI();
        userAPI = APILocator.getUserAPI();
        versionableAPI = APILocator.getVersionableAPI();
        contentletAPI = APILocator.getContentletAPI();
        systemEventsAPI = APILocator.getSystemEventsAPI();
        languageAPI=APILocator.getLanguageAPI();
    }

    @WrapInTransaction
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
    public Template getTemplate(IHTMLPage page, boolean working) throws DotDataException, DotSecurityException {
        if (working) 
            return APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), userAPI.getSystemUser(), false);
        else
            return APILocator.getTemplateAPI().findLiveTemplate(page.getTemplateId(), userAPI.getSystemUser(), false);
    }

    @Override
    public Host getParentHost(IHTMLPage page) throws DotDataException, DotStateException, DotSecurityException {
        return APILocator.getHostAPI().find(identifierAPI.find(page).getHostId(), userAPI.getSystemUser(), false);
    }

    @CloseDBIfOpened
    @Override
    public HTMLPageAsset fromContentlet(Contentlet con) {
    	if (con != null){
    		if(con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
    			throw new DotStateException("Contentlet : " + con.getInode() + " is not a pageAsset");
    		}
    	}else{
    		throw new DotStateException("Contentlet is null");
    	}
    	
    	HTMLPageAsset pa = (HTMLPageAsset) CacheLocator.getHTMLPageCache().get(con.getInode());
    	if(pa!=null){
    		return pa;
    	}
        pa=new HTMLPageAsset();
        pa.setStructureInode(con.getStructureInode());
        try {
            contentletAPI.copyProperties((Contentlet) pa, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Page Copy Failed on Contentlet Inode: " + con.getInode(), e);
        }
        pa.setHost(con.getHost());
        pa.setVariantId(con.getVariantId());
        if(UtilMethods.isSet(con.getFolder())){
            try{
                Identifier ident = identifierAPI.find(con);
                User systemUser = userAPI.getSystemUser();
                Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
                Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUser, false);
                pa.setFolder(folder.getInode());
            }catch(Exception e){
            	pa=new HTMLPageAsset();
                Logger.warn(this, "Unable to convert contentlet to page asset: " + con, e);
            }
        }

        //We have to get PageUrl from the Identifier (AssetName)
        if(!UtilMethods.isSet(pa.getPageUrl())){
            try{
                Identifier identifier = identifierAPI.find(con);
                if(identifier != null && UtilMethods.isSet(identifier.getAssetName())){
                    pa.setPageUrl(identifier.getAssetName());
                } else {
                    Logger.warn(this, "Unable to convert Contentlet to page asset, error at set PageUrl: " + con);
                }
            }catch(Exception e){
                pa=new HTMLPageAsset();
                Logger.warn(this, "Unable to convert Contentlet to page asset: " + con, e);
            }
        }

        try {
			CacheLocator.getHTMLPageCache().add(pa);
		} catch (Exception e) {

		}
        
        return pa;
    }

    @CloseDBIfOpened
    @Override
    public IHTMLPage getPageByPath(final String uri, final Host site, final Long languageId, final Boolean live) {
        Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath URI: " + uri + " Site: " + site + " LanguageId: " + languageId + " Live: " + live);
        Identifier id;
        if(!UtilMethods.isSet(uri)){
            return null;
        }
        final String errorMsg = "Unable to find '%s' HTML Page with URI '%s' in language '%s' in Site '%s' [%s]: %s";
        if (CMSUrlUtil.getInstance().isFolder(uri, site)) {
            id = this.getIndexPageIdentifier(uri, site);
        } else {
            try {
                id = this.identifierAPI.find(site, uri);
            } catch (final Exception e) {
                Logger.error(this, String.format(errorMsg, live ? "live" : "working",
                        uri, languageId, site, site.getIdentifier(),
                        ExceptionUtil.getErrorMessage(e)), e);
                return null;
            }
        }
        Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath Identifier: " + (id== null? "Not Found" : id.toString()));
        if (id == null || id.getId() == null) {
            return null;
        }

        if (Identifier.ASSET_TYPE_CONTENTLET.equals(id.getAssetType())) {
            try {
                final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();
                Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath currentVariantId: " + currentVariantId);
                Optional<ContentletVersionInfo> cinfo = versionableAPI
                        .getContentletVersionInfo( id.getId(), languageId, currentVariantId);
                Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath contentletVersionInfo: " + (cinfo.isEmpty() ? "Not Found" : cinfo.toString()));
                if (cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(CMSUrlUtil.NOT_FOUND)) {

                    cinfo = versionableAPI.getContentletVersionInfo( id.getId(), languageId);

                    if (cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(CMSUrlUtil.NOT_FOUND)) {
                        Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath contentletVersionInfo not found");
                        return null;
                    }
                }

                final Contentlet contentlet = this.contentletAPI.find(live ? cinfo.get().getLiveInode()
                        : cinfo.get().getWorkingInode(), this.userAPI.getSystemUser(), false);
                Logger.debug(this.getClass(), "HTMLPageAssetAPIImpl_getPageByPath contentlet: " + contentlet.toString());
                if (BaseContentType.HTMLPAGE.getType() == contentlet.getContentType().baseType().getType()) {
                    return fromContentlet(contentlet);
                }
            } catch (final Exception e) {
                Logger.error(this, String.format(errorMsg, live ? "live" : "working",
                        uri, languageId, site, site.getIdentifier(),
                        ExceptionUtil.getErrorMessage(e)));
                return null;
            }
        }
        return null;
    }

    private Identifier getIndexPageIdentifier(final String folderURI, final Host host) {
        final String indexPageUri = folderURI.endsWith("/") ?
                folderURI + CMS_INDEX_PAGE.get() : folderURI + "/" + CMS_INDEX_PAGE.get();

        try {
            return identifierAPI.find(host, indexPageUri);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unable to find folder URI: " + folderURI);
            return null;
        }
    }

    @Override
    public List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		return getHTMLPages(parent, live, deleted, -1, 0, "", user,
				respectFrontEndRoles);
    }


    @CloseDBIfOpened
    @Override
    public List<IHTMLPage> getHTMLPagesByContainer(final String containerId) throws DotDataException, DotSecurityException {

        final List<MultiTree> pageContents = APILocator.getMultiTreeAPI().getMultiTrees(containerId);
        final ImmutableList.Builder<IHTMLPage> builder = new ImmutableList.Builder<>();

        if (UtilMethods.isSet(pageContents)) {

            for (final MultiTree pageContent : pageContents) {

                if (null != pageContent && UtilMethods.isSet(pageContent.getHtmlPage())) {
                    builder.add(this.findPage(pageContent.getHtmlPage(), APILocator.systemUser(), false));
                }
            }
        }

        return builder.build();
    }

    @CloseDBIfOpened
	@Override
	public List<IHTMLPage> getHTMLPages(Object parent, boolean live,
			boolean deleted, int limit, int offset, String sortBy, User user,
			boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException {
		List<IHTMLPage> pages = new ArrayList<>();
		StringBuffer query = new StringBuffer();
		String liveWorkingDeleted = (live) ? " +live:true "
				: (deleted) ? " +working:true +deleted:true "
						: " +working:true -deleted:true";
		query.append(liveWorkingDeleted);
		if (parent instanceof Folder) {
			query.append(" +conFolder:" + ((Folder) parent).getInode());
			query.append(" +conHost:" + ((Folder) parent).getHostId());
		} else if (parent instanceof Host) {
			query.append(" +conFolder:SYSTEM_FOLDER +conHost:"
					+ ((Host) parent).getIdentifier());
		// if not a folder or host the filtering is done by template (parent) 
		}else if (parent instanceof String)
			if(!((String)parent).isEmpty()){
				// list of content types (htmlpage type)
				List<Structure> structures = StructureFactory.getStructures("structureType="+Structure.STRUCTURE_TYPE_HTMLPAGE, "", 0, 0, "");
				StringBuilder structuresList = new StringBuilder();
				boolean notOR = true;
				
				// creates a list of content types with the template field e.g. htmlpageasset.template:## OR newpages.template:##
				for(Structure structure: structures){
					if(notOR){
						notOR=!notOR;
					}else
						structuresList.append(" OR ");
					structuresList.append(structure.getVelocityVarName());
					structuresList.append(".template:");
					structuresList.append((String)parent);					
				}
				if(structuresList.length()>0)
					query.append(" +("+ structuresList.toString().trim()+")" );
			}
		
		query.append(" +structureType:" + Structure.STRUCTURE_TYPE_HTMLPAGE);
		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate asc";
		}
		List<Contentlet> contentlets = contentletAPI.search(
				query.toString(), limit, offset, sortBy, user,
				respectFrontEndRoles);
		for (Contentlet cont : contentlets) {
			if(UtilMethods.isSet(fromContentlet(cont).getInode()))
				pages.add(fromContentlet(cont));
		}
		return pages;
	}
    
    @Override
    public List<IHTMLPage> getLiveHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, true, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getLiveHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, true, false, user, respectFrontEndRoles );
    }

    @Override
    public List<IHTMLPage> getWorkingHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getWorkingHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, false, false, user, respectFrontEndRoles );
    }

    @Override
    public List<IHTMLPage> getDeletedHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, true, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getDeletedHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, false, true, user, respectFrontEndRoles );
    }

    @CloseDBIfOpened
    @Override
    public Folder getParentFolder(IHTMLPage htmlPage) throws DotDataException, DotSecurityException {
        Identifier ident = identifierAPI.find(htmlPage.getIdentifier());
        if(ident.getParentPath().equals("/")) {
            return APILocator.getFolderAPI().findSystemFolder();
        }
        else {
            return APILocator.getFolderAPI().findFolderByPath(
                    ident.getParentPath(), APILocator.getHostAPI().find(
                            ident.getHostId(), userAPI.getSystemUser(), false),
                            userAPI.getSystemUser(), false);
        }
    }

    /**
     * @see HTMLPageAssetAPI#findPage(String, User, boolean)
     */
    @CloseDBIfOpened
    @Override
    public IHTMLPage findPage(final String inode, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        Contentlet cpage = contentletAPI.find(inode, user, respectFrontendRoles);
        if(cpage!=null) {
            return this.fromContentlet(cpage);
        }
        return null;

    }
    
    @Override
    public String getHostDefaultPageType(String hostId) throws DotDataException, DotSecurityException {
        return getHostDefaultPageType(APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false));
    }

    @CloseDBIfOpened
    @Override
    public String getHostDefaultPageType(Host host) {
        Field ff=host.getStructure().getField(DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD);
        if(ff!=null && InodeUtils.isSet(ff.getInode())) {
            String stInode= ff.getFieldType().equals(Field.FieldType.CONSTANT.toString()) ? ff.getValues()
                    : host.getStringProperty(ff.getVelocityVarName());
            if(stInode!=null && UtilMethods.isSet(stInode)) {
                Structure type=CacheLocator.getContentTypeCache().getStructureByInode(stInode);
                if(type!=null && InodeUtils.isSet(type.getInode())) {
                    return stInode;
                }
            }
        }
        return DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE;
    }

    @WrapInTransaction
    @Override
    public boolean rename ( HTMLPageAsset page, String newName, User user ) throws DotDataException, DotSecurityException {

        Identifier sourceIdent = identifierAPI.find( page );
        Host host = APILocator.getHostAPI().find( sourceIdent.getHostId(), user, false );
        Identifier targetIdent = identifierAPI.find( host, sourceIdent.getParentPath() + newName );
        if (targetIdent == null || !InodeUtils.isSet(targetIdent.getId())
          || sourceIdent.getId().equals(targetIdent.getId())) // we can rename the page itself
         {
            Contentlet cont = contentletAPI.checkout( page.getInode(), user, false );
            cont.setStringProperty( URL_FIELD, newName );
            cont = contentletAPI.checkin( cont, user, false );
            if ( page.isLive() ) {
                contentletAPI.publish( cont, user, false );
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean move(HTMLPageAsset page, Folder parent, User user) throws DotDataException, DotSecurityException {
        return move(page,APILocator.getHostAPI().find(identifierAPI.find(parent.getIdentifier()).getHostId(),user,false), parent, user);
    }

    @Override
    public boolean move(HTMLPageAsset page, Host host, User user) throws DotDataException, DotSecurityException {
        return move(page,host,APILocator.getFolderAPI().findSystemFolder(),user);
    }

    @WrapInTransaction
    public boolean move(HTMLPageAsset page, Host host, Folder parent, User user)
            throws DotDataException, DotSecurityException {
        Identifier sourceIdent = identifierAPI.find(page);
        Identifier targetFolderIdent = identifierAPI.find(parent.getIdentifier());
        Identifier targetIdent = identifierAPI.find(host,
                targetFolderIdent.getURI() + sourceIdent.getAssetName());
        if (targetIdent == null || !InodeUtils.isSet(targetIdent.getId())) {
            final Contentlet contentlet = contentletAPI
                    .find(page.getInode(), user, false);

            contentletAPI.move(contentlet, user, host, parent, false);

            if ( parent != null ) {
                CacheLocator.getNavToolCache().removeNav(parent.getHostId(), parent.getInode());
            }

            final Folder oldParent = APILocator.getFolderAPI().findFolderByPath( sourceIdent.getParentPath(), host, user, false);

            CacheLocator.getNavToolCache().removeNav(oldParent.getHostId(), oldParent.getInode());

            systemEventsAPI.pushAsync(SystemEventType.MOVE_PAGE_ASSET, new Payload(page.getMap(), Visibility.EXCLUDE_OWNER,
                    new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

            return true;
        }
        return false;
    }

    /**
     * @see HTMLPageAssetAPI#findPagesByTemplate(Template, User, boolean)
     */
    @Override
    public List<Contentlet> findPagesByTemplate(Template template, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return findPagesByTemplate(template, user, respectFrontendRoles, -1);
    }

    /**
     * @see HTMLPageAssetAPI#findPagesByTemplate(Template, User, boolean, int)
     */
    @Override
    public List<Contentlet> findPagesByTemplate(Template template, User user, boolean respectFrontendRoles, int limit)
            throws DotDataException, DotSecurityException {

        return permissionAPI.filterCollection(
                    contentletAPI.search("+catchall:" + template.getIdentifier() + " +baseType:"
                                    + BaseContentType.HTMLPAGE.getType(), limit, 0, null, user,
                            respectFrontendRoles),
                    PermissionAPI.PERMISSION_READ,
                    respectFrontendRoles,
                    user);
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
    @CloseDBIfOpened
    @Override
    public List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern,boolean include,Date startDate, Date endDate) {

        Set<String> ret = new HashSet<>();
        
        String likepattern=RegEX.replaceAll(pattern, "%", "\\*");
        
        String concat;
        if(DbConnectionFactory.isMySql()){
            concat=" concat(ii.parent_path, ii.asset_name) ";
        }else if (DbConnectionFactory.isMsSql()) {
            concat=" (ii.parent_path + ii.asset_name) ";
        }else {
            concat=" (ii.parent_path || ii.asset_name) ";
        }
        
        Structure st=CacheLocator.getContentTypeCache().getStructureByInode(DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
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
        
        return new ArrayList<>(ret);
    }
    
    @Override
    public String getHTML(IHTMLPage htmlPage, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(htmlPage, true, null, userAgent);
	}

    @Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(htmlPage, liveMode, null, userAgent);
	}

	@Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, String userAgent) throws DotStateException,
			DotDataException, DotSecurityException {
		return getHTML(htmlPage, liveMode, contentId, null, userAgent);
	}
	
	@Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		String uri = htmlPage.getURI();
		Host host = getParentHost(htmlPage);
		return getHTML(uri, host, liveMode, contentId, user, userAgent);
	}

    @Override
    public String getHTML(IHTMLPage htmlPage, boolean liveMode,
                          String contentId, User user, long langId, String userAgent)
            throws DotStateException, DotDataException, DotSecurityException {
        String uri = htmlPage.getURI();
        Host host = getParentHost(htmlPage);
        return getHTML(uri, host, liveMode, contentId, user, langId, userAgent);
    }

	@Override
	public String getHTML(String uri, Host host, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(uri, host, liveMode, contentId, user, languageAPI.getDefaultLanguage().getId(), userAgent);
	}


    @CloseDBIfOpened
	@Override
    public String getHTML(final String uri, final Host host, final boolean liveMode, final String contentId, final User user, final long viewingLang,
            String userAgent) throws DotStateException, DotDataException, DotSecurityException {

        HttpServletRequest requestProxy =
            new MockAttributeRequest(
                new MockSessionRequest(
                    new FakeHttpRequest(host.getHostname(), uri).request()
                ).request()
            ).request();
        HttpServletResponse responseProxy = new BaseResponse().response();


        Identifier ident = identifierAPI.find(host, uri);
        if (ident==null || !UtilMethods.isSet(ident.getId()) ) {
            throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
        }

        

        Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI().getContentletVersionInfo( ident.getId(), viewingLang );
        if((cinfo.isEmpty() || cinfo.get().getLiveInode() == null)
                && viewingLang!=languageAPI.getDefaultLanguage().getId()
                && languageAPI.canDefaultPageToDefaultLanguage()){
          cinfo = APILocator.getVersionableAPI().getContentletVersionInfo( ident.getId(), languageAPI.getDefaultLanguage().getId() );
        }
        // if we still have nothing.
        if (!InodeUtils.isSet(ident.getId()) || cinfo.isEmpty()
                || cinfo.get().getLiveInode() == null && liveMode) {
            throw new ResourceNotFoundException(String.format("Resource %s not found in Live mode!", uri));
        }

        responseProxy.setContentType("text/html");
        requestProxy.setAttribute("User-Agent", userAgent);
        requestProxy.setAttribute("idInode", ident.getId());
        requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, Long.toString(viewingLang));
        /* Set long lived cookie regardless of who this is */
        String _dotCMSID = UtilMethods.getCookieValue(requestProxy.getCookies(),
                com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        if (!UtilMethods.isSet(_dotCMSID)) {
            /* create unique generator engine */
            Cookie idCookie = CookieUtil.createCookie();
            responseProxy.addCookie(idCookie);
        }


        PageMode mode =  (liveMode) ?PageMode.setPageMode(requestProxy, PageMode.LIVE) : PageMode.setPageMode(requestProxy, PageMode.PREVIEW_MODE);
            
        
        boolean signedIn = (user != null);


        Logger.debug(HTMLPageAssetAPIImpl.class, "Page Permissions for URI=" + uri);

        IHTMLPage pageProxy = new HTMLPageAsset();
        pageProxy.setIdentifier(ident.getInode());

        // Check if the page is visible by a CMS Anonymous role
        try {
            if (!permissionAPI.doesUserHavePermission(pageProxy, PermissionAPI.PERMISSION_READ, user, true)) {
                // this page is protected. not anonymous access

                /*******************************************************************
                 * If we need to redirect someone somewhere to login before seeing a page, we need
                 * to edit the /portal/401.jsp page to sendRedirect the user to the proper login
                 * page. We are not using the REDIRECT_TO_LOGIN variable in the config any longer.
                 ******************************************************************/
                if (!signedIn) {
                    // No need for the below LAST_PATH attribute on the front
                    // end http://jira.dotmarketing.net/browse/DOTCMS-2675
                    // request.getSession().setAttribute(WebKeys.LAST_PATH,
                    // new ObjectValuePair(uri, request.getParameterMap()));
                    requestProxy.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, uri);

                    Logger.debug(HTMLPageAssetAPIImpl.class,
                            "VELOCITY CHECKING PERMISSION: Page doesn't have anonymous access" + uri);

                    Logger.debug(HTMLPageAssetAPIImpl.class, "401 URI = " + uri);

                    Logger.debug(HTMLPageAssetAPIImpl.class, "Unauthorized URI = " + uri);
                    responseProxy.sendError(401, "The requested page/file is unauthorized");
                    return "An SYSTEM ERROR OCCURED !";

                } else if (!permissionAPI.getReadRoles(ident)
                        .contains(APILocator.getRoleAPI().loadLoggedinSiteRole())) {
                    // user is logged in need to check user permissions
                    Logger.debug(HTMLPageAssetAPIImpl.class, "VELOCITY CHECKING PERMISSION: User signed in");

                    // check user permissions on this asset
                    if (!permissionAPI.doesUserHavePermission(ident, PermissionAPI.PERMISSION_READ, user, true)) {
                        // the user doesn't have permissions to see this page
                        // go to unauthorized page
                        Logger.warn(HTMLPageAssetAPIImpl.class,
                                "VELOCITY CHECKING PERMISSION: Page doesn't have any access for this user");
                        responseProxy.sendError(403, "The requested page/file is forbidden");
                        return "PAGE NOT FOUND!";
                    }
                }
            }

            if (UtilMethods.isSet(contentId)) {
                requestProxy.setAttribute(WebKeys.WIKI_CONTENTLET, contentId);
            }


 

            LanguageWebAPI langWebAPI = WebAPILocator.getLanguageWebAPI();
            langWebAPI.checkSessionLocale(requestProxy);

            requestProxy.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
            requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
            return VelocityModeHandler.modeHandler(mode, requestProxy, responseProxy).eval();

        } catch (Exception e1) {
            Logger.error(this, e1.getMessage(), e1);
            return null;
        } 

    }
	
    /**
     * This returns the proper ihtml page based on id, state and language
     * 
     * @param identifier
     * @param tryLang
     * @param live
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
	@Override
    public IHTMLPage findByIdLanguageFallback(final Identifier identifier, final long tryLang, final boolean live, final User user, final boolean respectFrontEndPermissions)
            throws DotDataException, DotSecurityException {


	    return findByIdLanguageFallback(identifier.getId(), tryLang, live, user, respectFrontEndPermissions);

    }

    @CloseDBIfOpened
	@Override
    public IHTMLPage findByIdLanguageFallback(final String identifier, final long tryLang, final boolean live, final User user, final boolean respectFrontEndPermissions)
            throws DotDataException, DotSecurityException {

        IHTMLPage htmlPage;
        Contentlet contentlet;

        try {
            contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier, live, tryLang,
                    user, respectFrontEndPermissions);
            htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

        } catch (DotStateException dse) {
            htmlPage = findPageInDefaultLanguageDifferentThanProvided(identifier, tryLang, live, user,
                    respectFrontEndPermissions, dse);

        }

        return htmlPage;
    }

    private IHTMLPage findPageInDefaultLanguageDifferentThanProvided(String identifier, long providedLang, boolean live,
                                                                     User user, boolean respectFrontEndPermissions,
            DotStateException dse)
            throws DotDataException, DotSecurityException {
        Contentlet contentlet;
        IHTMLPage htmlPage;

        if (languageAPI.canDefaultPageToDefaultLanguage()
                && providedLang != languageAPI.getDefaultLanguage().getId()) {
            try {
                contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier, live,
                    languageAPI.getDefaultLanguage().getId(), user, respectFrontEndPermissions);
                htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
            } catch(DotStateException e) {
                throw new DoesNotExistException(
                        "Unable to find Page. Identifier: " + identifier + ", Live: " + live + ", Lang: "
                                + languageAPI.getDefaultLanguage().getId(), e);
            }
        } else {
            throw new DoesNotExistException(
                    "Unable to find Page. Identifier: " + identifier + ", Live: " + live + ", Lang: " + providedLang, dse);
        }
        return htmlPage;
    }

    @Override
    public IHTMLPage findByIdLanguageVariantFallback(@NotNull final String identifier, final long tryLang,
                                                     @NotNull final String tryVariant, final boolean live,
                                                     @NotNull final User user,
                                                     final boolean respectFrontEndPermissions) throws DotSecurityException {
        final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final boolean fallbackLang = tryLang != defaultLang && DEFAULT_PAGE_TO_DEFAULT_LANGUAGE.get();

        // given lang and variant
        HTMLPageAsset asset = Try.of(() -> fromContentlet(contentletAPI
                .findContentletByIdentifier(identifier, live, tryLang, tryVariant, APILocator.systemUser(), true))).getOrNull();

        if (asset == null) {

            // given lang and DEFAULT varian
            asset = Try.of(() -> fromContentlet(contentletAPI.findContentletByIdentifier(identifier, live, tryLang, VariantAPI.DEFAULT_VARIANT.name(), APILocator.systemUser(), true))).getOrNull();
        }

        if (asset == null && fallbackLang) {
            // DEFAULT lang and given variant
            asset = Try.of(() -> fromContentlet(contentletAPI.findContentletByIdentifier(identifier, live, defaultLang, tryVariant, APILocator.systemUser(), true))).getOrNull();
            if (asset == null) {
                // DEFAULT lang and DEFAULT variant
                asset = Try.of(() -> fromContentlet(contentletAPI.findContentletByIdentifier(identifier, live, defaultLang, VariantAPI.DEFAULT_VARIANT.name(), APILocator.systemUser(), true))).getOrNull();
            }
        }
        if (asset == null) {
            throw new DotStateException("Unable to find page that matches. id:" + identifier + " lang:" + tryLang + " variant:" + tryVariant);
        }

        permissionAPI.checkPermission(asset, PermissionLevel.READ, user);
        return asset;
    }

}
