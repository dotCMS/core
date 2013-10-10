/**
 *
 */
package com.dotcms.content.elasticsearch.business;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.enterprise.cmis.QueryResult;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletAndBinary;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.gson.Gson;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author Jason Tesser
 * @author David Torres
 * @since 1.5
 *
 */
public class ESContentletAPIImpl implements ContentletAPI {

    private static final ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();
    private static final String CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT = "Can't change state of checked out content or where inode is not set. Use Search or Find then use method";
    private static final String CANT_GET_LOCK_ON_CONTENT ="Only the CMS Admin or the user who locked the contentlet can lock/unlock it";
    private ESContentFactoryImpl conFac;
    private PermissionAPI perAPI;
    private CategoryAPI catAPI;
    private RelationshipAPI relAPI;
    private FieldAPI fAPI;
    private LanguageAPI lanAPI;
    private DistributedJournalAPI<String> distAPI;
    private int MAX_LIMIT = 100000;
    private TagAPI tagAPI;

    private static final String backupPath = ConfigUtils.getBackupPath() + java.io.File.separator + "contentlets";

    public ESContentletAPIImpl () {
        fAPI = APILocator.getFieldAPI();
        conFac = new ESContentFactoryImpl();
        perAPI = APILocator.getPermissionAPI();
        catAPI = APILocator.getCategoryAPI();
        relAPI = APILocator.getRelationshipAPI();
        lanAPI = APILocator.getLanguageAPI();
        distAPI = APILocator.getDistributedJournalAPI();
        tagAPI = APILocator.getTagAPI();
    }

    public Object loadField(String inode, Field f) throws DotDataException {
        return conFac.loadField(inode, f.getFieldContentlet());
    }

    public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException{
        return conFac.findAllCurrent(offset, limit);
    }

    public boolean isContentlet(String inode) throws DotDataException, DotRuntimeException {
        Contentlet contentlet = new Contentlet();
        try{
            contentlet = find(inode, APILocator.getUserAPI().getSystemUser(), true);
        }catch (DotSecurityException dse) {
            throw new DotRuntimeException("Unable to use system user : ", dse);
        }catch (Exception e) {
            Logger.debug(this,"Inode unable to load as contentlet.  Asssuming it is not content");
            return false;
        }
        if(contentlet!=null){
            if(InodeUtils.isSet(contentlet.getInode())){
            return true;
            }
        }
        return false;
    }

    public Contentlet find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Contentlet c = conFac.find(inode);
        if(c  == null)
            return null;
        if(perAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            return c;
        }else{
        	Object u = (user == null) ? user : user.getUserId();
            throw new DotSecurityException("User:" + u + " does not have permissions to Contentlet:" + inode);
        }
    }

    public List<Contentlet> findByStructure(String structureInode, User user,   boolean respectFrontendRoles, int limit, int offset) throws DotDataException,DotSecurityException {
        List<Contentlet> contentlets = conFac.findByStructure(structureInode, limit, offset);
        return perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public List<Contentlet> findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset) throws DotDataException,DotSecurityException {
        return findByStructure(structure.getInode(), user, respectFrontendRoles, limit, offset);
    }

    /**
     * Returns a live Contentlet Object for a given language
     * @param languageId
     * @param inode
     * @return Contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Contentlet findContentletForLanguage(long languageId,    Identifier contentletId) throws DotDataException, DotSecurityException {
        Contentlet con = conFac.findContentletForLanguage(languageId, contentletId);
        if(con == null){
            Logger.debug(this,"No working contentlet found for language");
        }
        return con;
    }

    public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException {
        if(languageId<=0) {
            languageId=APILocator.getLanguageAPI().getDefaultLanguage().getId();
        }

        try {
            ContentletVersionInfo clvi = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
            if(clvi ==null){
                throw new DotContentletStateException("No contenlet found for given identifier");
            }
            if(live){
                return find(clvi.getLiveInode(), user, respectFrontendRoles);
            }
            else{
                return find(clvi.getWorkingInode(), user, respectFrontendRoles);
            }
        }catch (DotSecurityException se) {
			throw se;
    	}catch (Exception e) {
            throw new DotContentletStateException("Can't find contentlet: " + identifier + " lang:" + languageId + " live:" + live,e);
        }

    }

    public List<Contentlet> findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException {
        List<Contentlet> l = new ArrayList<Contentlet>();
        Long languageIdLong = languageId <= 0?null:new Long(languageId);
        for(String identifier : identifiers){
            Contentlet con = findContentletByIdentifier(identifier.trim(), live, languageIdLong, user, respectFrontendRoles);
            l.add(con);
        }
        return l;
    }

    public List<Contentlet> findContentlets(List<String> inodes)throws DotDataException, DotSecurityException {
        return conFac.findContentlets(inodes);
    }


    public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        try {
            return perAPI.filterCollection(search("+conFolder:" + parentFolder.getInode(), -1, 0, null , user, respectFrontendRoles), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }

    }

    public List<Contentlet> findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        try {
            return perAPI.filterCollection(search("+conHost:" + parentHost.getIdentifier() + " +working:true", -1, 0, null , user, respectFrontendRoles), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)){
            Logger.debug(PublishFactory.class, "publishAsset: user = " + user.getEmailAddress() + ", don't have permissions to publish: " + contentlet.getInode());

            //If the contentlet has CMS Owner Publish permission on it, the user creating the new contentlet is allowed to publish

            List<Role> roles = perAPI.getRoles(contentlet.getPermissionId(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
            Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
            boolean isCMSOwner = false;
            if(roles.size() > 0){
                for (Role role : roles) {
                    if(role == cmsOwner){
                        isCMSOwner = true;
                        break;
                    }
                }
                if(!isCMSOwner){
                    throw new DotSecurityException("User does not have permission to publish contentlet with inode " + contentlet.getInode());
                }
            }else{
                throw new DotSecurityException("User does not have permission to publish contentlet with inode " + contentlet.getInode());
            }
        }
        canLock(contentlet, user);




        String syncMe = (UtilMethods.isSet(contentlet.getIdentifier()))  ? contentlet.getIdentifier() : UUIDGenerator.generateUuid()  ;

        synchronized (syncMe) {


            Logger.debug(this, "*****I'm a Contentlet -- Publishing");

            Contentlet workingCon = findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(), user, respectFrontendRoles);

            if (workingCon == null || !InodeUtils.isSet(workingCon.getInode())) {
                workingCon = contentlet;
            }

            conFac.save(contentlet);

            //Set contentlet to live and unlocked
            APILocator.getVersionableAPI().setLive(contentlet);
            //APILocator.getVersionableAPI().setLocked(contentlet.getIdentifier(), false, user);

            finishPublish(contentlet, false);

        }
    }

    /* Not needed anymore
     * private void setLiveContentOff(Contentlet contentlet) throws DotDataException {
        List<Contentlet> liveCons = new ArrayList<Contentlet>();
        if (InodeUtils.isSet(contentlet.getIdentifier())) {
            liveCons = conFac.findContentletsByIdentifier(contentlet.getIdentifier(), true, contentlet
                    .getLanguageId());
        }
        Logger.debug(this, "working contentlet =" + contentlet.getInode());
        for (Contentlet liveCon : liveCons) {
            if ((liveCon != null) && (InodeUtils.isSet(liveCon.getInode()))
                    && (!liveCon.getInode().equalsIgnoreCase(contentlet.getInode()))) {

                Logger.debug(this, "live contentlet =" + liveCon.getInode());
                // sets previous live to false
                liveCon.setLive(false);
                liveCon.setModDate(new java.util.Date());

                // persists it
                conFac.save(liveCon);
            }
        }
    }*/

    private void finishPublish(Contentlet contentlet, boolean isNew) throws DotSecurityException, DotDataException,
            DotContentletStateException, DotStateException {
        finishPublish(contentlet, isNew, true);

    }

    private void finishPublish(Contentlet contentlet, boolean isNew, boolean isNewVersion) throws DotSecurityException, DotDataException,
    DotContentletStateException, DotStateException {

        if (!contentlet.isWorking())
            throw new DotContentletStateException("Only the working version can be published");



        User user = APILocator.getUserAPI().getSystemUser();

        // DOTCMS - 4393
        // Publishes the files associated with the Contentlet
        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        Language defaultLang=APILocator.getLanguageAPI().getDefaultLanguage();
        for (Field field : fields) {
            if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
                    || field.getFieldType().equals(Field.FieldType.FILE.toString())) {

                try {
                    String value = "";
                    if(UtilMethods.isSet(getFieldValue(contentlet, field))){
                        value = getFieldValue(contentlet, field).toString();
                    }
                    //Identifier id = (Identifier) InodeFactory.getInode(value, Identifier.class);
                    Identifier id = APILocator.getIdentifierAPI().find(value);
                    if (InodeUtils.isSet(id.getInode()) && id.getAssetType().equals("contentlet")) {
                    	Contentlet fileAssetCont = findBinaryAssociatedContent(id,contentlet.getLanguageId());
                        publish(fileAssetCont, APILocator.getUserAPI().getSystemUser(), false);
                    }else if(InodeUtils.isSet(id.getInode())){
                        File file  = (File) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);
                        PublishFactory.publishAsset(file, user, false, isNewVersion);
                    }
                } catch (Exception ex) {
                    Logger.debug(this, ex.toString());
                    throw new DotStateException("Problem occured while publishing file",ex);
                }
            }
        }

        // gets all not live file children
        List<File> files = getRelatedFiles(contentlet, user, false);
        for (File file : files) {
            Logger.debug(this, "*****I'm a Contentlet -- Publishing my File Child=" + file.getInode());
            try {
                PublishFactory.publishAsset(file, user, false, isNewVersion);
            } catch (DotSecurityException e) {
                Logger.debug(this, "User has permissions to publish the content = " + contentlet.getIdentifier()
                        + " but not the related file = " + file.getIdentifier());
            } catch (Exception e) {
                throw new DotStateException("Problem occured while publishing file");
            }
        }

        // gets all not live link children
        Logger.debug(this, "IM HERE BEFORE PUBLISHING LINKS FOR A CONTENTLET!!!!!!!");
        List<Link> links = getRelatedLinks(contentlet, user, false);
        for (Link link : links) {
            Logger.debug(this, "*****I'm a Contentlet -- Publishing my Link Child=" + link.getInode());
            try {
                PublishFactory.publishAsset(link, user, false, isNewVersion);
            } catch (DotSecurityException e) {
                Logger.debug(this, "User has permissions to publish the content = " + contentlet.getIdentifier()
                        + " but not the related link = " + link.getIdentifier());
                throw new DotStateException("Problem occured while publishing link");
            } catch (Exception e) {
                throw new DotStateException("Problem occured while publishing file");
            }
        }

        // writes the contentlet object to a file
        indexAPI.addContentToIndex(contentlet);

        if (!isNew) {
            // writes the contentlet to a live directory under velocity folder
            ContentletServices.invalidate(contentlet);
            ContentletMapServices.invalidate(contentlet);

            CacheLocator.getContentletCache().remove(contentlet.getInode());

            // Need to refresh the live pages that reference this piece of
            // content
            publishRelatedHtmlPages(contentlet);
        }

    }


    public List<Contentlet> search(String luceneQuery, int limit, int offset,String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles, PermissionAPI.PERMISSION_READ);
    }

    public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,DotSecurityException {
        PaginatedArrayList<Contentlet> contents = new PaginatedArrayList<Contentlet>();
        ArrayList<String> inodes = new ArrayList<String>();


        PaginatedArrayList <ContentletSearch> list =(PaginatedArrayList)searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
        contents.setTotalResults(list.getTotalResults());
        for(ContentletSearch conwrap: list){

            inodes.add(conwrap.getInode());
        }


        List<Contentlet> contentlets = findContentlets(inodes);
        Map<String, Contentlet> map = new HashMap<String, Contentlet>(contentlets.size());
        for (Contentlet contentlet : contentlets) {
            map.put(contentlet.getInode(), contentlet);
        }
        for (String inode : inodes) {
            if(map.get(inode) != null)
                contents.add(map.get(inode));
        }
        return contents;

    }

    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset,String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException  {
        return searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles, PermissionAPI.PERMISSION_READ);
    }

    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,DotSecurityException, ParseException {
    	return searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles, requiredPermission, false);
    }

    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException,DotSecurityException, ParseException {
        PaginatedArrayList<Contentlet> contents = new PaginatedArrayList<Contentlet>();
        PaginatedArrayList <ContentletSearch> list =(PaginatedArrayList)searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
        contents.setTotalResults(list.getTotalResults());

        List<String> identifierList = new ArrayList<String>();
        for(ContentletSearch conwrap: list){
            String ident=conwrap.getIdentifier();
            Identifier ii=APILocator.getIdentifierAPI().find(ident);
            if(ii!=null && UtilMethods.isSet(ii.getId()))
                identifierList.add(ident);
        }
        String[] identifiers=new String[identifierList.size()];
        identifiers=identifierList.toArray(identifiers);

        List<Contentlet> contentlets = new ArrayList<Contentlet>();
        if(anyLanguage){//GIT-816
        	for(String identifier : identifiers){
        		for(Language lang : APILocator.getLanguageAPI().getLanguages()){
                	try{
                		Contentlet languageContentlet = null;
                		try{
                			languageContentlet = findContentletByIdentifier(identifier, false, lang.getId(), user, respectFrontendRoles);
                		}catch (DotContentletStateException e) {
                			Logger.debug(this,e.getMessage(),e);
						}
                		if(languageContentlet != null && UtilMethods.isSet(languageContentlet.getInode())){
                			contentlets.add(languageContentlet);
                			break;
                		}
                    }catch(DotContentletStateException se){
                    	Logger.debug(this, se.getMessage());
                    }
                }
        	}
        }else{
        	contentlets = findContentletsByIdentifiers(identifiers, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontendRoles);
        }

        Map<String, Contentlet> map = new HashMap<String, Contentlet>(contentlets.size());
        for (Contentlet contentlet : contentlets) {
            map.put(contentlet.getIdentifier(), contentlet);
        }
        for (String identifier : identifiers) {
            if(map.get(identifier) != null && !contents.contains(map.get(identifier))){
                contents.add(map.get(identifier));
            }
        }
        return contents;

    }

    protected void addPermissionsToQuery(StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles) throws DotSecurityException, DotDataException  {
        if(user != null)
            buffy.append(" +((+owner:" + user.getUserId() + " +ownerCanRead:true) ");
        else
            buffy.append(" +(");
        if (0 < roles.size()) {
            buffy.append(" (");
            for (Role role : roles) {
                buffy.append("permissions:P" + role.getId() + ".1P* ");
            }
            buffy.append(") ");
        }
        if(respectFrontendRoles) {
            buffy.append("(permissions:P" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId() + ".1P*) ");
            if(user != null)
                buffy.append("(permissions:P" + APILocator.getRoleAPI().loadLoggedinSiteRole().getId() + ".1P*)");
        }
        buffy.append(")");
    }

    public List <ContentletSearch> searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException {
        boolean isAdmin = false;
        List<Role> roles = new ArrayList<Role>();
        if(user == null && !respectFrontendRoles){
            throw new DotSecurityException("You must specify a user if you are not respecting frontend roles");
        }
        if(user != null){
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            }else{
                isAdmin = true;
            }
        }
        StringBuffer buffy = new StringBuffer(luceneQuery);

        // Permissions in the query
        if (!isAdmin)
            addPermissionsToQuery(buffy, user, roles, respectFrontendRoles);

        int originalLimit = limit;
        if(UtilMethods.isSet(sortBy) && sortBy.trim().equalsIgnoreCase("random")){
            sortBy="random";
        }
        if(limit>MAX_LIMIT || limit <=0){
            limit = MAX_LIMIT;
        }
        SearchHits lc = conFac.indexSearch(buffy.toString(), limit, offset, sortBy);
        PaginatedArrayList <ContentletSearch> list=new PaginatedArrayList<ContentletSearch>();
        list.setTotalResults(lc.getTotalHits());

        for (SearchHit sh : lc.hits()) {
            try{
                Map<String, Object> hm = new HashMap<String, Object>();
                ContentletSearch conwrapper= new ContentletSearch();
                conwrapper.setIdentifier(sh.field("identifier").getValue().toString());
                conwrapper.setInode(sh.field("inode").getValue().toString());

                list.add(conwrapper);
            }
            catch(Exception e){
                Logger.error(this,e.getMessage(),e);
            }

        }
        return list;
    }

    public void publishRelatedHtmlPages(Contentlet contentlet) throws DotStateException, DotDataException{
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        //Get the contentlet Identifier to gather the related pages
        //Identifier identifier = (Identifier) InodeFactory.getInode(contentlet.getIdentifier(),Identifier.class);
        Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        //Get the identifier's number of the related pages
        List<MultiTree> multitrees = (List<MultiTree>) MultiTreeFactory.getMultiTreeByChild(identifier.getInode());
        for(MultiTree multitree : multitrees)
        {
            //Get the Identifiers of the related pages
            //Identifier htmlPageIdentifier = (Identifier) InodeFactory.getInode(multitree.getParent1(),Identifier.class);
            Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(multitree.getParent1());
            //Get the pages
            try{
                HTMLPage page = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(htmlPageIdentifier, APILocator.getUserAPI().getSystemUser(), false);

                if(page != null && page.isLive()){
                    //Rebuild the pages' files
                    PageServices.invalidate(page);
                }
            }
            catch(Exception e){
                Logger.error(this.getClass(), "Cannot publish related HTML Pages.  Fail");
            }

        }
    }

    public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles)
        throws DotSecurityException, DotDataException, DotMappingException {

        if(!perAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)){
            throw new DotSecurityException("Must be able to publish structure to clean all the fields");
        }

        conFac.cleanIdentifierHostField(structure.getInode());

    }

    public void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        if(!perAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)){
            throw new DotSecurityException("Must be able to publish structure to clean all the fields");
        }

        String type = field.getFieldType();
        if(Field.FieldType.LINE_DIVIDER.toString().equals(type) ||
                Field.FieldType.TAB_DIVIDER.toString().equals(type) ||
                Field.FieldType.RELATIONSHIPS_TAB.toString().equals(type) ||
                Field.FieldType.CATEGORIES_TAB.toString().equals(type) ||
                Field.FieldType.PERMISSIONS_TAB.toString().equals(type))
        {
            throw new DotDataException("Unable to clean a " + type + " system field");
        }

        //http://jira.dotmarketing.net/browse/DOTCMS-2178
        if(Field.FieldType.BINARY.toString().equals(field.getFieldType())){
            List<Contentlet> contentlets = conFac.findByStructure(structure.getInode(),0,0);

            deleteBinaryFiles(contentlets,field);

            return; // Binary fields have nothing to do with database.
        }

        conFac.cleanField(structure.getInode(), field);

    }

    public Date getNextReview(Contentlet content, User user, boolean respectFrontendRoles) throws DotSecurityException {

        Date baseDate = new Date();
        String reviewInterval = content.getReviewInterval();
        Pattern p = Pattern.compile("(\\d+)([dmy])");
        Matcher m = p.matcher(reviewInterval);
        boolean b = m.matches();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(baseDate);
        if (b) {
            int num = Integer.parseInt(m.group(1));
            String qual = m.group(2);
            if (qual.equals("d")) {
                cal.add(GregorianCalendar.DATE, num);
            }
            if (qual.equals("m")) {
                cal.add(GregorianCalendar.MONTH, num);
            }
            if (qual.equals("y")) {
                cal.add(GregorianCalendar.YEAR, num);
            }
        }
        return cal.getTime();
    }

    public List<Map<String, Object>> getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        if(contentlet == null || !InodeUtils.isSet(contentlet.getInode())){
            throw new DotContentletStateException("Contentlet must exist");
        }
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet");
        }
        Identifier id = APILocator.getIdentifierAPI().find(contentlet);
        if (!InodeUtils.isSet(id.getInode()))
            return results;
        List<MultiTree> trees = MultiTreeFactory.getMultiTreeByChild(id.getInode());
        for (MultiTree tree : trees) {
            HTMLPage page = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(tree.getParent1(), APILocator.getUserAPI().getSystemUser(), false);
            Container container = (Container) APILocator.getVersionableAPI().findWorkingVersion(tree.getParent2(), APILocator.getUserAPI().getSystemUser(), false);
            if (InodeUtils.isSet(page.getInode()) && InodeUtils.isSet(container.getInode())) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("page", page);
                map.put("container", container);
                results.add(map);
            }
        }
        return results;
    }

    public Object getFieldValue(Contentlet contentlet, Field theField){
        try {

            if(fAPI.isElementConstant(theField)){
                if(contentlet.getMap().get(theField.getVelocityVarName())==null)
                    contentlet.getMap().put(theField.getVelocityVarName(), theField.getValues());
                return theField.getValues();
            }


            if(theField.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
                if(FolderAPI.SYSTEM_FOLDER.equals(contentlet.getFolder()))
                     return contentlet.getHost();
                else
                     return contentlet.getFolder();
            }else if(theField.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
                Category category = catAPI.find(theField.getValues(), APILocator.getUserAPI().getSystemUser(), false);
                // Get all the Contentlets Categories
                List<Category> selectedCategories = catAPI.getParents(contentlet, APILocator.getUserAPI().getSystemUser(), false);
                Set<Category> categoryList = new HashSet<Category>();
                List<Category> categoryTree = catAPI.getAllChildren(category, APILocator.getUserAPI().getSystemUser(), false);
                if (selectedCategories.size() > 0 && categoryTree != null) {
                    for (int k = 0; k < categoryTree.size(); k++) {
                        Category cat = (Category) categoryTree.get(k);
                        for (Category categ : selectedCategories) {
                            if (categ.getInode().equalsIgnoreCase(cat.getInode())) {
                                categoryList.add(cat);
                            }
                        }
                    }
                }
                return categoryList;
            }else{
                return contentlet.get(theField.getVelocityVarName());
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if (InodeUtils.isSet(linkInode)) {
            Link link = (Link) InodeFactory.getInode(linkInode, Link.class);
            Identifier identifier = APILocator.getIdentifierAPI().find(link);
            relAPI.addRelationship(contentlet.getInode(),identifier.getInode(), relationName);
            ContentletServices.invalidate(contentlet, true);
            // writes the contentlet object to a file
            ContentletMapServices.invalidate(contentlet, true);
        }
    }

    public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if (InodeUtils.isSet(fileInode)) {
            File file = (File) InodeFactory.getInode(fileInode, File.class);
            Identifier identifier = APILocator.getIdentifierAPI().find(file);
            relAPI.addRelationship(contentlet.getInode(),identifier.getInode(), relationName);
            ContentletServices.invalidate(contentlet, true);
            // writes the contentlet object to a file
            ContentletMapServices.invalidate(contentlet, true);
        }
    }

    public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if (InodeUtils.isSet(imageInode)) {
            File image = (File) InodeFactory.getInode(imageInode, File.class);
            Identifier identifier = APILocator.getIdentifierAPI().find(image);
            relAPI.addRelationship(contentlet.getInode(),identifier.getInode(), relationName);
            ContentletServices.invalidate(contentlet, true);
            // writes the contentlet object to a file
            ContentletMapServices.invalidate(contentlet, true);
        }
    }

    public List<Contentlet> findPageContentlets(String HTMLPageIdentifier,String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles)    throws DotSecurityException, DotDataException {
        List<Contentlet> contentlets = conFac.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId);
        return perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public ContentletRelationships getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {

        return getAllRelationships(find(contentletInode, user, respectFrontendRoles));
    }

    public ContentletRelationships getAllRelationships(Contentlet contentlet)throws DotDataException {

        ContentletRelationships cRelationships = new ContentletRelationships(contentlet);
        Structure structure = contentlet.getStructure();
        List<ContentletRelationshipRecords> matches = cRelationships.getRelationshipsRecords();
        List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(structure);

        for (Relationship relationship : relationships) {

            ContentletRelationshipRecords records = null;
            List<Contentlet> contentletList = null;

            if (RelationshipFactory.isSameStructureRelationship(relationship, structure)) {

                //If it's a same structure kind of relationship we need to pull all related content
                //on both roles as parent and a child of the relationship

                //Pulling as child
                records = cRelationships.new ContentletRelationshipRecords(relationship, false);
                contentletList = new ArrayList<Contentlet> ();
                try {
                    contentletList.addAll(getRelatedContent(contentlet, relationship, false, APILocator.getUserAPI().getSystemUser(), true));
                } catch (DotSecurityException e) {
                    Logger.error(this,"Unable to get system user",e);
                }
                records.setRecords(contentletList);
                matches.add(records);

                //Pulling as parent
                records = cRelationships.new ContentletRelationshipRecords(relationship, true);
                contentletList = new ArrayList<Contentlet> ();
                try {
                    contentletList.addAll(getRelatedContent(contentlet, relationship, true, APILocator.getUserAPI().getSystemUser(), true));
                } catch (DotSecurityException e) {
                    Logger.error(this,"Unable to get system user",e);
                }
                records.setRecords(contentletList);
                matches.add(records);

            } else
            if (RelationshipFactory.isChildOfTheRelationship(relationship, structure)) {

                records = cRelationships.new ContentletRelationshipRecords(relationship, false);
                try{
                    contentletList = getRelatedContent(contentlet, relationship, APILocator.getUserAPI().getSystemUser(), true);
                } catch (DotSecurityException e) {
                    Logger.error(this,"Unable to get system user",e);
                }
                records.setRecords(contentletList);
                matches.add(records);

            } else
            if (RelationshipFactory.isParentOfTheRelationship(relationship, structure)) {
                records = cRelationships.new ContentletRelationshipRecords(relationship, true);
                try{
                    contentletList = getRelatedContent(contentlet, relationship, APILocator.getUserAPI().getSystemUser(), true);
                } catch (DotSecurityException e) {
                    Logger.error(this,"Unable to get system user",e);
                }
                records.setRecords(contentletList);
                matches.add(records);
            }



        }

        return cRelationships;
    }

    public List<Contentlet> getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles)
        throws DotDataException, DotSecurityException {

        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet");
        }

        List<Contentlet> contentletList =  null;


        if(isLiveContent != null){
            contentletList = conFac.getContentletsByIdentifier(contentlet.getIdentifier(), isLiveContent);
        }else{
            contentletList = conFac.getContentletsByIdentifier(contentlet.getIdentifier(), null);
        }
        return contentletList;
    }

    public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(contentlet == null){
            throw new DotContentletStateException("The contentlet cannot Be null");
        }
        canLock(contentlet, user);

        if(contentlet.isLocked() ){
            // persists the webasset
            APILocator.getVersionableAPI().setLocked(contentlet, false, user);
            indexAPI.addContentToIndex(contentlet);
        }
    }

    public Identifier getRelatedIdentifier(Contentlet contentlet,String relationshipType, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet");
        }
        return conFac.getRelatedIdentifier(contentlet, relationshipType);
    }

    public List<File> getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        return perAPI.filterCollection(conFac.getRelatedFiles(contentlet), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public List<Link> getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        return perAPI.filterCollection(conFac.getRelatedLinks(contentlet), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public List<Contentlet> getRelatedContent(Contentlet contentlet,Relationship rel, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {

        boolean isSameStructRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());
        String q = "";

        if(isSameStructRelationship) {
            q = "+type:content +(" + rel.getRelationTypeValue() + "-parent:" + contentlet.getIdentifier() + " " +
                rel.getRelationTypeValue() + "-child:" + contentlet.getIdentifier() + ") ";
            if(!InodeUtils.isSet(contentlet.getIdentifier())){
                q = "+type:content +(" + rel.getRelationTypeValue() + "-parent:" + "0 " +
                rel.getRelationTypeValue() + "-child:"  + "0 ) ";
            }
        } else {
            q = "+type:content +" + rel.getRelationTypeValue() + ":" + contentlet.getIdentifier();
            if(!InodeUtils.isSet(contentlet.getIdentifier())){
                q = "+type:content +" + rel.getRelationTypeValue() + ":" + "0";
            }
        }

        try{
        	return perAPI.filterCollection(searchByIdentifier(q, -1, 0, rel.getRelationTypeValue() + "-" + contentlet.getIdentifier() + "-order" , user, respectFrontendRoles, PermissionAPI.PERMISSION_READ, true), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        }catch (ParseException e) {
            throw new DotDataException("Unable look up related content",e);
        }

    }

    public List<Contentlet> getRelatedContent(Contentlet contentlet,Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {

        boolean isSameStructureRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());
        String q = "";

        if(isSameStructureRelationship) {
            String disc = pullByParent?"-parent":"-child";
            q = "+type:content +" + rel.getRelationTypeValue() + disc + ":" + contentlet.getIdentifier();
            if(!InodeUtils.isSet(contentlet.getIdentifier()))
                q = "+type:content +" + rel.getRelationTypeValue() + disc + ":" + "0";

        } else {
            q = "+type:content +" + rel.getRelationTypeValue() + ":" + contentlet.getIdentifier();
            if(!InodeUtils.isSet(contentlet.getIdentifier()))
                q = "+type:content +" + rel.getRelationTypeValue() + ":" + "0";
        }

        try{
        	return perAPI.filterCollection(searchByIdentifier(q, -1, 0, rel.getRelationTypeValue() + "-" + contentlet.getIdentifier() + "-order" , user, respectFrontendRoles, PermissionAPI.PERMISSION_READ, true), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        }catch (ParseException e) {
            throw new DotDataException("Unable look up related content",e);
        }

    }

    public void delete(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        List<Contentlet> contentlets = new ArrayList<Contentlet>();
        contentlets.add(contentlet);
        delete(contentlets, user, respectFrontendRoles);
    }

    public void delete(Contentlet contentlet, User user,boolean respectFrontendRoles, boolean allVersions) throws DotDataException,DotSecurityException {
        List<Contentlet> contentlets = new ArrayList<Contentlet>();
        contentlets.add(contentlet);
        delete(contentlets, user, respectFrontendRoles, allVersions);
    }

    public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(contentlets == null || contentlets.size() == 0){
            Logger.info(this, "No contents passed to delete so returning");
            return;
        }
        for (Contentlet contentlet : contentlets){
            if(contentlet.getInode().equals("")) {
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }
            canLock(contentlet, user);
        }
        List<Contentlet> perCons = perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
        List<Contentlet> contentletsVersion = new ArrayList<Contentlet>();
        contentletsVersion.addAll(contentlets);

        if(perCons.size() != contentlets.size()){
            throw new DotSecurityException("User does not have permission to delete some or all of the contentlets");
        }

        List<String> l = new ArrayList<String>();

        for (Contentlet contentlet : contentlets) {
            if(!l.contains(contentlet)){
                l.add(contentlet.getIdentifier());
            }
        }

        AdminLogger.log(this.getClass(), "delete", "User trying to delete the following contents" + l.toString(), user);


        for (Contentlet con : perCons) {
            List<Contentlet> otherLanguageCons;
            otherLanguageCons = conFac.getContentletsByIdentifier(con.getIdentifier());
            boolean cannotDelete = false;
            for (Contentlet contentlet : otherLanguageCons) {
                if(contentlet.getInode() != contentlet.getInode() && contentlet.getLanguageId() != con.getLanguageId() && !contentlet.isArchived()){
                    cannotDelete = true;
                    indexAPI.removeContentFromIndex(contentlet);
                    break;
                }
            }
            if(cannotDelete){
                Logger.warn(this, "Cannot delete content that has a working copy in another language");
                perCons.remove(con);
                continue;
            }
            catAPI.removeChildren(con, APILocator.getUserAPI().getSystemUser(), true);
            catAPI.removeParents(con, APILocator.getUserAPI().getSystemUser(), true);
            List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(con.getStructure());
            for(Relationship relationship :  rels){
                deleteRelatedContent(con,relationship,user,respectFrontendRoles);
            }

            contentletsVersion.addAll(findAllVersions(APILocator.getIdentifierAPI().find(con.getIdentifier()), user, respectFrontendRoles));
            APILocator.getVersionableAPI().deleteContentletVersionInfo(con.getIdentifier(), con.getLanguageId());

            List<MultiTree> mts = MultiTreeFactory.getMultiTreeByChild(con.getIdentifier());
            for (MultiTree mt : mts) {
                Identifier pageIdent = APILocator.getIdentifierAPI().find(mt.getParent1());
                if(pageIdent != null && UtilMethods.isSet(pageIdent.getInode())){
                    HTMLPage page=APILocator.getHTMLPageAPI().loadPageByPath(pageIdent.getURI(), pageIdent.getHostId());
                    if(page!=null && UtilMethods.isSet(page.getIdentifier()))
                        PageServices.invalidate(page);
                }
                MultiTreeFactory.deleteMultiTree(mt);
            }
        }

        // jira.dotmarketing.net/browse/DOTCMS-1073
        if (perCons.size() > 0) {
            XStream _xstream = new XStream(new DomDriver());
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String lastmoddate = sdf.format(date);
            java.io.File _writing = null;
            java.io.File _writingwbin = null;

            java.io.File backupFolder = new java.io.File(backupPath);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            for(Contentlet cont:perCons){
                Structure st=cont.getStructure();
                List <Field> fields= st.getFields();
                List<Map<String,Object>> filelist = new ArrayList<Map<String,Object>>();
                ContentletAndBinary contentwbin= new ContentletAndBinary ();
                contentwbin.setMap(cont.getMap()) ;
                Boolean arebinfiles=false;
                java.io.File file=null;
                for(Field field:fields){
                    if(field.getFieldType().equals(Field.FieldType.BINARY.toString())){
                        try{
                            file = getBinaryFile(cont.getInode(), field.getVelocityVarName(), user);
                        }catch (Exception ex) {
                            Logger.debug(this, ex.getMessage(), ex);
                        }
                        if (file != null) {
                            byte[] bytes = null;
                            try {
                                bytes = FileUtil.getBytes(file);
                            } catch (IOException e) {
                            }
                            Map<String,Object> temp = new HashMap<String,Object>();
                            temp.put(file.getName(), bytes);
                            filelist.add(temp);
                            arebinfiles = true;
                        }
                    }
                }

                _writing = new java.io.File(backupPath + java.io.File.separator + cont.getIdentifier().toString() + ".xml");
                _writingwbin = new java.io.File(backupPath + java.io.File.separator + cont.getIdentifier().toString() + "_bin" + ".xml");
                BufferedOutputStream _bout = null;

                if(!arebinfiles){
                    try {
                        _bout = new BufferedOutputStream(new FileOutputStream(_writing));
                    } catch (FileNotFoundException e) {
                    }
                    _xstream.toXML(cont, _bout);
                }
                else{
                    try {
                        _bout = new BufferedOutputStream(new FileOutputStream(_writingwbin));
                    } catch (FileNotFoundException e) {

                    }
                    contentwbin.setBinaryFilesList(filelist);
                    _xstream.toXML(contentwbin, _bout);
                    arebinfiles=false;
                }
            }

        }
        conFac.delete(contentletsVersion);

        for (Contentlet contentlet : perCons) {
            indexAPI.removeContentFromIndex(contentlet);
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        // jira.dotmarketing.net/browse/DOTCMS-1073
        deleteBinaryFiles(contentletsVersion,null);

        for (Contentlet contentlet : contentlets) {
        	try {
				PublisherAPI.getInstance().deleteElementFromPublishQueueTable(contentlet.getIdentifier());
			} catch (DotPublisherException e) {
				Logger.error(getClass(), "Error deleting Contentlet from Publishing Queue. Identifier:  " + contentlet.getIdentifier());
			}
		}

    }

    public void deleteAllVersionsandBackup(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        if(contentlets == null || contentlets.size() == 0){
            Logger.info(this, "No contents passed to delete so returning");
            return;
        }
        for (Contentlet con : contentlets)
            if(con.getInode().equals(""))
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        List<Contentlet> perCons = perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
        List<Contentlet> contentletsVersion = new ArrayList<Contentlet>();
        contentletsVersion.addAll(contentlets);

        if(perCons.size() != contentlets.size()){
            throw new DotSecurityException("User does not have permission to delete some or all of the contentlets");
        }
        for (Contentlet con : contentlets) {
            catAPI.removeChildren(con, APILocator.getUserAPI().getSystemUser(), true);
            catAPI.removeParents(con, APILocator.getUserAPI().getSystemUser(), true);
            List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(con.getStructure());
            for(Relationship relationship :  rels){
                deleteRelatedContent(con,relationship,user,respectFrontendRoles);
            }

            contentletsVersion.addAll(findAllVersions(APILocator.getIdentifierAPI().find(con.getIdentifier()), user, respectFrontendRoles));
        }

        // jira.dotmarketing.net/browse/DOTCMS-1073
        List<String> contentletInodes = new ArrayList<String>();
        for (Iterator iter = contentletsVersion.iterator(); iter.hasNext();) {
            Contentlet element = (Contentlet) iter.next();
            contentletInodes.add(element.getInode());
        }

        conFac.delete(contentletsVersion);

        for (Contentlet contentlet : perCons) {
            indexAPI.removeContentFromIndex(contentlet);
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        if (contentlets.size() > 0) {
            XStream _xstream = new XStream(new DomDriver());
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String lastmoddate = sdf.format(date);
            java.io.File _writing = null;

            java.io.File backupFolder = new java.io.File(backupPath);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            _writing = new java.io.File(backupPath + java.io.File.separator + lastmoddate + "_" + "deletedcontentlets" + ".xml");

            BufferedOutputStream _bout = null;
            try {
                _bout = new BufferedOutputStream(new FileOutputStream(_writing));
            } catch (FileNotFoundException e) {

            }
            _xstream.toXML(contentlets, _bout);
        }
        // jira.dotmarketing.net/browse/DOTCMS-1073
        deleteBinaryFiles(contentletsVersion,null);

    }


    public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,DotSecurityException {
        for (Contentlet con : contentlets){
            if(con.getInode().equals("")) {
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }
            if(!canLock(con, user)){
                throw new DotContentletStateException("Content Object is locked and cannot be deleted:" + con.getIdentifier());
            }
        }
        List<Contentlet> perCons = perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
        List<Contentlet> contentletsVersion = new ArrayList<Contentlet>();
        contentletsVersion.addAll(contentlets);

        if(perCons.size() != contentlets.size()){
            throw new DotSecurityException("User does not have permission to delete some or all of the contentlets");
        }
        for (Contentlet con : contentlets) {
            catAPI.removeChildren(con, APILocator.getUserAPI().getSystemUser(), true);
            catAPI.removeParents(con, APILocator.getUserAPI().getSystemUser(), true);
            List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(con.getStructure());
            for(Relationship relationship :  rels){
                deleteRelatedContent(con,relationship,user,respectFrontendRoles);
            }

        }

        // jira.dotmarketing.net/browse/DOTCMS-1073
        List<String> contentletInodes = new ArrayList<String>();
        for (Iterator iter = contentletsVersion.iterator(); iter.hasNext();) {
            Contentlet element = (Contentlet) iter.next();
            contentletInodes.add(element.getInode());
        }

        conFac.delete(contentletsVersion);

        for (Contentlet contentlet : perCons) {
            indexAPI.removeContentFromIndex(contentlet);
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        // jira.dotmarketing.net/browse/DOTCMS-1073
        deleteBinaryFiles(contentletsVersion,null);

    }

    public void deleteVersion(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        if(contentlet == null){
            Logger.info(this, "No contents passed to delete so returning");
            return;
        }
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user)){
            throw new DotSecurityException("User does not have permission to delete some or all of the contentlets");
        }

        catAPI.removeChildren(contentlet, APILocator.getUserAPI().getSystemUser(), true);
        catAPI.removeParents(contentlet, APILocator.getUserAPI().getSystemUser(), true);
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
        for(Relationship relationship :  rels){
            deleteRelatedContent(contentlet,relationship,user,respectFrontendRoles);
        }

        ArrayList<Contentlet> contentlets = new ArrayList<Contentlet>();
        contentlets.add(contentlet);
        conFac.deleteVersion(contentlet);

        ContentletVersionInfo cinfo=APILocator.getVersionableAPI().getContentletVersionInfo(
                contentlet.getIdentifier(), contentlet.getLanguageId());

        if(cinfo.getWorkingInode().equals(contentlet.getInode()) ||
                (InodeUtils.isSet(cinfo.getLiveInode()) && cinfo.getLiveInode().equals(contentlet.getInode())))
            // we remove from index if it is the working or live version
            indexAPI.removeContentFromIndex(contentlet);

        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);

        // jira.dotmarketing.net/browse/DOTCMS-1073
        deleteBinaryFiles(contentlets,null);
    }

    public void archive(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)){
            throw new DotSecurityException("User does not have permission to edit the contentlet");
        }
        Contentlet workingContentlet = findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(), user, respectFrontendRoles);
        Contentlet liveContentlet = null;
        try{
            liveContentlet = findContentletByIdentifier(contentlet.getIdentifier(), true, contentlet.getLanguageId(), user, respectFrontendRoles);
        }catch (DotContentletStateException ce) {
            Logger.debug(this,"No live contentlet found for identifier = " + contentlet.getIdentifier());
        }



        canLock(contentlet, user);
        User modUser = null;

        try{
            modUser = APILocator.getUserAPI().loadUserById(workingContentlet.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
        }catch(Exception ex){
            if(ex instanceof NoSuchUserException){
                modUser = APILocator.getUserAPI().getSystemUser();
            }
        }

        if(modUser != null){
            workingContentlet.setModUser(modUser.getUserId());
        }


        if (user == null || !workingContentlet.isLocked() || workingContentlet.getModUser().equals(user.getUserId())) {

            if (liveContentlet != null && InodeUtils.isSet(liveContentlet.getInode())) {
                APILocator.getVersionableAPI().removeLive(liveContentlet.getIdentifier(), liveContentlet.getLanguageId());
                indexAPI.removeContentFromLiveIndex(liveContentlet);
            }

            // sets deleted to true
            APILocator.getVersionableAPI().setDeleted(workingContentlet, true);

            // Updating lucene index
            indexAPI.addContentToIndex(workingContentlet);

            ContentletServices.invalidate(contentlet);
            ContentletMapServices.invalidate(contentlet);
            publishRelatedHtmlPages(contentlet);
        }else{
            throw new DotContentletStateException("Contentlet is locked: Unable to archive");
        }
    }

    public void archive(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
        boolean stateError = false;
        for (Contentlet contentlet : contentlets) {
            try{
                archive(contentlet, user, respectFrontendRoles);
            }catch (DotContentletStateException e) {
                stateError = true;
            }
        }
        if(stateError){
            throw new DotContentletStateException("Unable to archive one or more contentlets because it is locked");
        }

    }

    public void lock(Contentlet contentlet, User user,  boolean respectFrontendRoles) throws DotContentletStateException, DotDataException,DotSecurityException {
        if(contentlet == null){
            throw new DotContentletStateException("The contentlet cannot Be null");
        }
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot edit Contentlet");
        }


        canLock(contentlet, user);

        // persists the webasset
        APILocator.getVersionableAPI().setLocked(contentlet, true, user);
        indexAPI.addContentToIndex(contentlet);
    }

    //public void removeContentletFromIndex(String contentletIdentifier) throws DotDataException{
    //    indexAPI.removeContentFromIndex(content)
    //  distAPI.addContentIndexEntryToDelete(contentletIdentifier);
    //}

    public void reindex()throws DotReindexStateException {
        refreshAllContent();
    }

    public void reindex(Structure structure)throws DotReindexStateException {
        try {
            distAPI.addStructureReindexEntries(structure.getInode());
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex",e);
        }
    }

    public void reindex(Contentlet contentlet)throws DotReindexStateException, DotDataException{
        indexAPI.addContentToIndex(contentlet);
    }


    public void refresh(Structure structure) throws DotReindexStateException {
        try {
            distAPI.addStructureReindexEntries(structure.getInode());
            CacheLocator.getContentletCache().clearCache();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex",e);
        }

    }

    public void refresh(Contentlet contentlet) throws DotReindexStateException,
            DotDataException {
        indexAPI.addContentToIndex(contentlet);
        CacheLocator.getContentletCache().add(contentlet.getInode(), contentlet);
    }

    public void refreshAllContent() throws DotReindexStateException {
        try {
            HibernateUtil.startTransaction();

            // we lock the table dist_reindex_journal until we
            ReindexThread.getInstance().lockCluster();

            if(indexAPI.isInFullReindex()){
            	try{
            		ReindexThread.getInstance().unlockCluster();
            		HibernateUtil.commitTransaction();
            	}catch (Exception e) {
            		 try {
                         HibernateUtil.rollbackTransaction();
                     } catch (DotHibernateException e1) {
                         Logger.warn(this, e1.getMessage(),e1);
                     }
				}
            	return;
            }
            // we prepare the new index and aliases to point both old and new
            indexAPI.setUpFullReindex();

            // wait a bit while ES do its distribution work and new
            // index/aliases become available
            Thread.sleep(10000L);

            // new records to index
            distAPI.addBuildNewIndexEntries();

            // then we let the reindexThread start working
            ReindexThread.getInstance().unlockCluster();

            HibernateUtil.commitTransaction();

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.warn(this, e1.getMessage(),e1);
            }
            throw new DotReindexStateException("Unable to complete reindex",e);
        }

    }

    public void refreshContentUnderHost(Host host) throws DotReindexStateException {
        try {
            distAPI.refreshContentUnderHost(host);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex",e);
        }

    }

    public void refreshContentUnderFolder(Folder folder) throws DotReindexStateException {
        try {
            distAPI.refreshContentUnderFolder(folder);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex",e);
        }

    }

    public void unpublish(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot unpublish Contentlet");
        }


        unpublish(contentlet, user);
    }

    private void unpublish(Contentlet contentlet, User user) throws DotDataException,DotSecurityException, DotContentletStateException {
        if(contentlet == null || !UtilMethods.isSet(contentlet.getInode())){
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }
        canLock(contentlet, user);

        APILocator.getVersionableAPI().removeLive(contentlet.getIdentifier(), contentlet.getLanguageId());

        indexAPI.addContentToIndex(contentlet);
        indexAPI.removeContentFromLiveIndex(contentlet);


        ContentletServices.unpublishContentletFile(contentlet);
        ContentletMapServices.unpublishContentletMapFile(contentlet);
        publishRelatedHtmlPages(contentlet);

    }

    public void unpublish(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,    DotSecurityException, DotContentletStateException {
        boolean stateError = false;
        for (Contentlet contentlet : contentlets) {
            try{
                unpublish(contentlet, user, respectFrontendRoles);
            }catch (DotContentletStateException e) {
                stateError = true;
            }
        }
        if(stateError){
            throw new DotContentletStateException("Unable to unpublish one or more contentlets because it is locked");
        }
    }

    public void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot unpublish Contentlet");
        }
        Contentlet workingContentlet = findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(), user, respectFrontendRoles);
        Contentlet liveContentlet = null;
        canLock(contentlet, user);
        try{
            liveContentlet = findContentletByIdentifier(contentlet.getIdentifier(), true, contentlet.getLanguageId(), user, respectFrontendRoles);
        }catch (DotContentletStateException ce) {
            Logger.debug(this,"No live contentlet found for identifier = " + contentlet.getIdentifier());
        }
        if(liveContentlet != null && liveContentlet.getInode().equalsIgnoreCase(workingContentlet.getInode()) && !workingContentlet.isArchived())
            throw new DotContentletStateException("Contentlet is unarchivable");

        APILocator.getVersionableAPI().setDeleted(workingContentlet, false);

        indexAPI.addContentToIndex(workingContentlet);

        // we don't want to reindex this twice when it is the same version
        if(liveContentlet!=null && UtilMethods.isSet(liveContentlet.getInode())
                && !liveContentlet.getInode().equalsIgnoreCase(workingContentlet.getInode()))
            indexAPI.addContentToIndex(liveContentlet);

        ContentletServices.invalidate(contentlet);
        ContentletMapServices.invalidate(contentlet);
        publishRelatedHtmlPages(contentlet);
    }

    public void unarchive(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        boolean stateError = false;
        for (Contentlet contentlet : contentlets) {
            try{
                unarchive(contentlet, user, respectFrontendRoles);
            }catch (DotContentletStateException e) {
                stateError = true;
            }
        }
        if(stateError){
            throw new DotContentletStateException("Unable to unarchive one or more contentlets because it is locked");
        }
    }

    public void deleteRelatedContent(Contentlet contentlet,Relationship relationship, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException,DotContentletStateException {
        deleteRelatedContent(contentlet, relationship, RelationshipFactory.isParentOfTheRelationship(relationship, contentlet.getStructure()), user, respectFrontendRoles);
    }

    public void deleteRelatedContent(Contentlet contentlet,Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException,DotContentletStateException {
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot edit Contentlet1");
        }
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
        if(!rels.contains(relationship)){
            throw new DotContentletStateException("Contentlet does not have passed in relationship");
        }
        List<Contentlet> cons = getRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
        cons = perAPI.filterCollection(cons, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        RelationshipFactory.deleteRelationships(contentlet, relationship, cons);
    }

    private void deleteUnrelatedContents(Contentlet contentlet, ContentletRelationshipRecords related, boolean hasParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,DotContentletStateException {
        if (!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException("User cannot edit Contentlet1");
        }
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
        if (!rels.contains(related.getRelationship())) {
            throw new DotContentletStateException("Contentlet does not have passed in relationship");
        }
        List<Contentlet> cons = getRelatedContent(contentlet, related.getRelationship(), hasParent, user, respectFrontendRoles);
        cons = perAPI.filterCollection(cons, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

        boolean contentSelected;
        Tree tree;
        for (Contentlet relatedContent: cons) {
            contentSelected = false;

            for (Contentlet selectedRelatedContent: related.getRecords()) {
                if (selectedRelatedContent.getIdentifier().equals(relatedContent.getIdentifier())) {
                    contentSelected = true;
                    break;
                }
            }

            if (!contentSelected) {
                if (related.isHasParent()) {
                    tree = TreeFactory.getTree(contentlet.getIdentifier(), relatedContent.getIdentifier(), related.getRelationship().getRelationTypeValue());
                } else {
                    tree = TreeFactory.getTree(relatedContent.getIdentifier(), contentlet.getIdentifier(), related.getRelationship().getRelationTypeValue());
                }
                Tree treeToDelete = TreeFactory.getTree(tree);
                TreeFactory.deleteTree(tree);
            }
        }
    }

    public void relateContent(Contentlet contentlet, Relationship rel, List<Contentlet> records, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException {
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        boolean hasParent = RelationshipFactory.isParentOfTheRelationship(rel, st);
        ContentletRelationshipRecords related = new ContentletRelationships(contentlet).new ContentletRelationshipRecords(rel, hasParent);
        related.setRecords(records);
        relateContent(contentlet, related, user, respectFrontendRoles);
    }

    private Tree getTree(String parent, String child, String relationType, List<Tree> trees) {
        Tree result = new Tree();

        for (Tree tree: trees) {
            if ((tree.getParent().equals(parent)) &&
                (tree.getChild().equals(child)) &&
                (tree.getRelationType().equals(relationType))) {
                //try {
                //  BeanUtils.copyProperties(result, tree);
                //} catch (Exception e) {
                //}
                //return result;
                return tree;
            }
        }

        return result;
    }

    public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException {
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot edit Contentlet1");
        }
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
        if(!rels.contains(related.getRelationship())){
            throw new DotContentletStateException("Contentlet does not have passed in relationship");
        }

        boolean child = !related.isHasParent();

        List<Tree> contentParents = null;
        if (child)
            contentParents = TreeFactory.getTreesByChild(contentlet.getIdentifier());

        deleteUnrelatedContents(contentlet, related, related.isHasParent(), user, respectFrontendRoles);
        Tree newTree = null;
        Set<Tree> uniqueRelationshipSet = new HashSet<Tree>();

        Relationship rel = related.getRelationship();
        List<Contentlet> conRels = RelationshipFactory.getAllRelationshipRecords(related.getRelationship(), contentlet, related.isHasParent());

        int treePosition = (conRels != null && conRels.size() != 0) ? conRels.size() : 1 ;
        int positionInParent;
        List<Tree> trees;
        for (Contentlet c : related.getRecords()) {
            if (child) {
                //newTree = TreeFactory.getTree(c.getIdentifier(), contentlet.getIdentifier(), rel.getRelationTypeValue());
                newTree = getTree(c.getIdentifier(), contentlet.getIdentifier(), rel.getRelationTypeValue(), contentParents);
                if(!InodeUtils.isSet(newTree.getParent())) {
                    try {
                        positionInParent = 0;
                        trees = TreeFactory.getTreesByParent(c.getIdentifier());
                        for (Tree tree: trees) {
                            if ((tree.getRelationType().equals(rel.getRelationTypeValue())) && (positionInParent <= tree.getTreeOrder())) {
                                positionInParent = tree.getTreeOrder() + 1;
                            }
                        }
                    } catch (Exception e) {
                        positionInParent = 0;
                    }
                    if(positionInParent == 0)//DOTCMS-6878
						positionInParent = treePosition;
                    newTree = new Tree(c.getIdentifier(), contentlet.getIdentifier(), rel.getRelationTypeValue(), positionInParent);
                }else{
                	if(newTree.getTreeOrder() == 0)//DOTCMS-6855
						newTree.setTreeOrder(treePosition);
					else
						treePosition = newTree.getTreeOrder();//DOTCMS-6878
                }
            } else {
                newTree = TreeFactory.getTree(contentlet.getIdentifier(), c.getIdentifier(), rel.getRelationTypeValue());
                if(!InodeUtils.isSet(newTree.getParent()))
                    newTree = new Tree(contentlet.getIdentifier(), c.getIdentifier(), rel.getRelationTypeValue(), treePosition);
                else
                    newTree.setTreeOrder(treePosition);
            }

            //newTree.setTreeOrder(treePosition);
            if( uniqueRelationshipSet.add(newTree) ) {

            	int newTreePosistion = newTree.getTreeOrder();
            	Tree treeToUpdate = TreeFactory.getTree(newTree);
            	treeToUpdate.setTreeOrder(newTreePosistion);

            	if(treeToUpdate != null && UtilMethods.isSet(treeToUpdate.getRelationType()))
            		TreeFactory.saveTree(treeToUpdate);
            	else
            		TreeFactory.saveTree(newTree);

            	treePosition++;

            }

            if(!child){// when we change the order we need to index all the sibling content
            	for(Contentlet con : getSiblings(c.getIdentifier())){
            		refresh(con);
            	}
            }
        }
    }

    public void publish(List<Contentlet> contentlets, User user,    boolean respectFrontendRoles) throws DotSecurityException,DotDataException, DotContentletStateException {
        boolean stateError = false;
        for (Contentlet contentlet : contentlets) {
            try{
                publish(contentlet, user, respectFrontendRoles);
            }catch (DotContentletStateException e) {
                stateError = true;
            }
        }
        if(stateError){
            throw new DotContentletStateException("Unable to publish one or more contentlets because it is locked");
        }
    }


    public boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException {
        if(!perAPI.doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet1");
        }
        if(!perAPI.doesUserHavePermission(contentlet2, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet2");
        }
        if(contentlet1.getInode().equalsIgnoreCase(contentlet2.getInode())){
            return true;
        }
        return false;
    }

    public List<Contentlet> getSiblings(String identifier)throws DotDataException, DotSecurityException {
        List<Contentlet> contentletList = conFac.getContentletsByIdentifier(identifier );

        return contentletList;
    }

    public Contentlet checkin(Contentlet contentlet, List<Category> cats, List<Permission> permissions, User user, boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException, DotSecurityException,DotContentletStateException, DotContentletValidationException {

        Map<Relationship, List<Contentlet>> contentRelationships = null;
        Contentlet workingCon = null;

        //If contentlet is not new
        if(InodeUtils.isSet(contentlet.getIdentifier())) {
            workingCon = findWorkingContentlet(contentlet);
            contentRelationships = findContentRelationships(workingCon);
        }
        else
        {
            contentRelationships = findContentRelationships(contentlet);
        }

        if(permissions == null)
            permissions = new ArrayList<Permission>();
        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;

        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);

    }

    public Contentlet checkin(Contentlet contentlet, List<Permission> permissions, User user, boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException, DotSecurityException,DotContentletStateException, DotContentletValidationException {

        List<Category> cats = null;
        Map<Relationship, List<Contentlet>> contentRelationships = null;
        Contentlet workingCon = null;

        //If contentlet is not new
        if(InodeUtils.isSet(contentlet.getIdentifier())) {
            workingCon = findWorkingContentlet(contentlet);
            cats = catAPI.getParents(contentlet, APILocator.getUserAPI().getSystemUser(), true);
            contentRelationships = findContentRelationships(workingCon);
        }
        else
        {
            contentRelationships = findContentRelationships(contentlet);
        }

        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats, User user, boolean respectFrontendRoles)throws IllegalArgumentException, DotDataException,DotSecurityException, DotContentletStateException,DotContentletValidationException {

        List<Permission> permissions = null;
        Contentlet workingCon = null;

        //If contentlet is not new
        if(InodeUtils.isSet(contentlet.getIdentifier())) {
            workingCon = findWorkingContentlet(contentlet);
            permissions = perAPI.getPermissions(workingCon);
        }

        if(permissions == null)
            permissions = new ArrayList<Permission>();

        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,User user, boolean respectFrontendRoles)throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException,DotContentletValidationException {

        List<Permission> permissions = null;
        List<Category> cats = null;
        Contentlet workingCon = null;

        //If contentlet is not new
        if(InodeUtils.isSet(contentlet.getIdentifier())) {
            workingCon = findWorkingContentlet(contentlet);
            permissions = perAPI.getPermissions(workingCon);
            cats = catAPI.getParents(contentlet, APILocator.getUserAPI().getSystemUser(), true);
        }

        if(permissions == null)
            permissions = new ArrayList<Permission>();
        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException, DotSecurityException,DotContentletStateException, DotContentletValidationException {

        List<Permission> permissions = null;
        List<Category> cats = null;
        Map<Relationship, List<Contentlet>> contentRelationships = null;
        Contentlet workingCon = null;

        Identifier ident=null;
        if(InodeUtils.isSet(contentlet.getIdentifier()))
            ident = APILocator.getIdentifierAPI().find(contentlet);

        //If contentlet is not new
        if(ident!=null && InodeUtils.isSet(ident.getId()) && contentlet.getMap().get("_dont_validate_me") != null) {
            workingCon = findWorkingContentlet(contentlet);
            if(workingCon != null) {
	            permissions = perAPI.getPermissions(workingCon);
	            cats = catAPI.getParents(workingCon, APILocator.getUserAPI().getSystemUser(), true);
	            contentRelationships = findContentRelationships(workingCon);
            } else {
            	contentRelationships = findContentRelationships(contentlet);
            }
        }
        else
        {
            contentRelationships = findContentRelationships(contentlet);
        }

        if(permissions == null)
            permissions = new ArrayList<Permission>();
        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, List<Category> cats)throws IllegalArgumentException, DotDataException,DotSecurityException, DotContentletStateException,DotContentletValidationException {

        List<Permission> permissions = null;
        Map<Relationship, List<Contentlet>> contentRelationships = null;
        Contentlet workingCon = null;

        //If contentlet is not new
        if(InodeUtils.isSet(contentlet.getIdentifier())) {
            workingCon = findWorkingContentlet(contentlet);
            permissions = perAPI.getPermissions(workingCon, false, true);
            contentRelationships = findContentRelationships(workingCon);
        }
        else
        {
            contentRelationships = findContentRelationships(contentlet);
        }

        if(permissions == null)
            permissions = new ArrayList<Permission>();
        if(cats == null)
            cats = new ArrayList<Category>();
        if(contentRelationships == null)
            contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(workingCon == null)
            workingCon = contentlet;
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException {
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for(Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = (Relationship) relEntry.getKey();
            boolean hasParent = RelationshipFactory.isParentOfTheRelationship(relationship, st);
            ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
            relationshipsRecords.add(records);
        }
        return checkin(contentlet, relationshipsData, cats, permissions, user, respectFrontendRoles);
    }

    public Contentlet checkin(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException {
        return checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, true);
    }

    public Contentlet checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException {
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for(Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = (Relationship) relEntry.getKey();
            boolean hasParent = RelationshipFactory.isParentOfTheRelationship(relationship, st);
            ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
            relationshipsRecords.add(records);
        }
        return checkin(contentlet, relationshipsData, cats , permissions, user, respectFrontendRoles, false);
    }


    private Contentlet checkin(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats, List<Permission> permissions,
            User user, boolean respectFrontendRoles, boolean createNewVersion) throws DotDataException, DotSecurityException, DotContentletStateException,
            DotContentletValidationException {

        String syncMe = (UtilMethods.isSet(contentlet.getIdentifier())) ? contentlet.getIdentifier() : UUIDGenerator.generateUuid();

        synchronized (syncMe) {
            boolean saveWithExistingID=false;
            String existingInode=null, existingIdentifier=null;
            boolean changedURI=false;

        	Contentlet workingContentlet = contentlet;
            try {
				if (createNewVersion && contentlet != null && InodeUtils.isSet(contentlet.getInode())) {
				    // maybe the user want to save new content with existing inode & identifier comming from somewhere
				    // we need to check that the inode doesn't exists
				    DotConnect dc=new DotConnect();
				    dc.setSQL("select inode from contentlet where inode=?");
				    dc.addParam(contentlet.getInode());
				    if(dc.loadResults().size()>0){
				    	if(contentlet.getMap().get("_dont_validate_me") != null){
				    		Logger.debug(this, "forcing checking with no version as the _dont_validate_me is set and inode exists");
				    		createNewVersion = false;
				    	}else{
				    		throw new DotContentletStateException("Contentlet must not exist already");
				    	}
				    } else {
				        saveWithExistingID=true;
				        existingInode=contentlet.getInode();
				        contentlet.setInode(null);

				        Identifier ident=APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
				        if(ident==null || !UtilMethods.isSet(ident.getId())) {
				            existingIdentifier=contentlet.getIdentifier();
				            contentlet.setIdentifier(null);
				        }
				    }
				}
				if (!createNewVersion && contentlet != null && !InodeUtils.isSet(contentlet.getInode()))
				    throw new DotContentletStateException("Contentlet must exist already");
				if (contentlet != null && contentlet.isArchived() && contentlet.getMap().get("_dont_validate_me") == null)
				    throw new DotContentletStateException("Unable to checkin an archived piece of content, please un-archive first");
				if (!perAPI.doesUserHavePermission(InodeUtils.isSet(contentlet.getIdentifier()) ? contentlet : contentlet.getStructure(),
				        PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
				    List<Role> rolesPublish = perAPI.getRoles(contentlet.getStructure().getPermissionId(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
				    List<Role> rolesWrite = perAPI.getRoles(contentlet.getStructure().getPermissionId(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1);
				    Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
				    boolean isCMSOwner = false;
				    if (rolesPublish.size() > 0 || rolesWrite.size() > 0) {
				        for (Role role : rolesPublish) {
				            if (role.getId().equals(cmsOwner.getId())) {
				                isCMSOwner = true;
				                break;
				            }
				        }
				        if (!isCMSOwner) {
				            for (Role role : rolesWrite) {
				                if (role.getId().equals(cmsOwner.getId())) {
				                    isCMSOwner = true;
				                    break;
				                }
				            }
				        }
				        if (!isCMSOwner) {
				            throw new DotSecurityException("User doesn't have write permissions to Contentlet");
				        }
				    } else {
				        throw new DotSecurityException("User doesn't have write permissions to Contentlet");
				    }
				}
				if (createNewVersion && (contentRelationships == null || cats == null || permissions == null))
				    throw new IllegalArgumentException(
				            "The categories, permissions and content relationships cannot be null when trying to checkin. The method was called improperly");
				try {
				    validateContentlet(contentlet, contentRelationships, cats);

				} catch (DotContentletValidationException ve) {
				    throw ve;
				}

				canLock(contentlet, user);
				contentlet.setModUser(user.getUserId());
				// start up workflow
				WorkflowAPI wapi  = APILocator.getWorkflowAPI();
				WorkflowProcessor workflow=null;

				if(contentlet.getMap().get("__disable_workflow__")==null) {
				    workflow = wapi.fireWorkflowPreCheckin(contentlet,user);
				}

				workingContentlet = contentlet;
				if(createNewVersion)
				    workingContentlet = findWorkingContentlet(contentlet);
				String workingContentletInode = (workingContentlet==null) ? "" : workingContentlet.getInode();

				boolean priority = contentlet.isLowIndexPriority();
				boolean isNewContent = false;
				if(!InodeUtils.isSet(workingContentletInode)){
				    isNewContent = true;
				}

				if (contentlet.getLanguageId() == 0) {
				    Language defaultLanguage = lanAPI.getDefaultLanguage();
				    contentlet.setLanguageId(defaultLanguage.getId());
				}

				contentlet.setModUser(user != null ? user.getUserId() : "");

				if (contentlet.getOwner() == null || contentlet.getOwner().length() < 1) {
				    contentlet.setOwner(user.getUserId());
				}

				// check contentlet Host
				User sysuser = APILocator.getUserAPI().getSystemUser();
                if (!UtilMethods.isSet(contentlet.getHost())) {
				    contentlet.setHost(APILocator.getHostAPI().findSystemHost(sysuser, true).getIdentifier());
				}
				if (!UtilMethods.isSet(contentlet.getFolder())) {
				    contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
				}

				Contentlet contentletRaw=contentlet;

                if ( contentlet.getMap().get( "_use_mod_date" ) != null ) {
                    /*
                     When a content is sent using the remote push publishing we want to respect the modification
                     dates the content already had.
                     */
                    contentlet.setModDate( (Date) contentlet.getMap().get( "_use_mod_date" ) );
                } else {
                    contentlet.setModDate( new Date() );
                }

				// Keep the 5 properties BEFORE store the contentlet on DB.
				String contentPushPublishDate = contentlet.getStringProperty("wfPublishDate");
				String contentPushPublishTime = contentlet.getStringProperty("wfPublishTime");
				String contentPushExpireDate = contentlet.getStringProperty("wfExpireDate");
				String contentPushExpireTime = contentlet.getStringProperty("wfExpireTime");
				String contentPushNeverExpire = contentlet.getStringProperty("wfNeverExpire");
				String contentWhereToSend = contentlet.getStringProperty("whereToSend");
				String forcePush = contentlet.getStringProperty("forcePush");

				if(saveWithExistingID)
				    contentlet = conFac.save(contentlet, existingInode);
				else
				    contentlet = conFac.save(contentlet);

				if (!InodeUtils.isSet(contentlet.getIdentifier())) {
				    Treeable parent = null;
				    if(UtilMethods.isSet(contentletRaw.getFolder()) && !contentletRaw.getFolder().equals(FolderAPI.SYSTEM_FOLDER)){
				        parent = APILocator.getFolderAPI().find(contentletRaw.getFolder(), sysuser, false);
				    }else{
				        parent = APILocator.getHostAPI().find(contentlet.getHost(), sysuser, false);
				    }
				    Identifier ident;
				    final Contentlet contPar=contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET?contentletRaw:contentlet;
				    if(existingIdentifier!=null)
				        ident = APILocator.getIdentifierAPI().createNew(contPar, parent, existingIdentifier);
				    else
				        ident = APILocator.getIdentifierAPI().createNew(contPar, parent);
				    contentlet.setIdentifier(ident.getId());
				    contentlet = conFac.save(contentlet);
				} else {
				    Identifier ident = APILocator.getIdentifierAPI().find(contentlet);

				    String oldURI=ident.getURI();

				    // make sure the identifier is removed from cache
				    // because changes here may affect URI then IdentifierCache
				    // can't remove it
				    CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);

				    ident.setHostId(contentlet.getHost());
				    if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET){
				        try {
				            ident.setAssetName(contentletRaw.getBinary(FileAssetAPI.BINARY_FIELD).getName());
				        } catch (IOException e) {
				            // TODO
				        }
				    }
				    if(UtilMethods.isSet(contentletRaw.getFolder()) && !contentletRaw.getFolder().equals(FolderAPI.SYSTEM_FOLDER)){
				        Folder folder = APILocator.getFolderAPI().find(contentletRaw.getFolder(), sysuser, false);
				        Identifier folderIdent = APILocator.getIdentifierAPI().find(folder);
				        ident.setParentPath(folderIdent.getPath());
				    }
				    else {
				        ident.setParentPath("/");
				    }
				    ident=APILocator.getIdentifierAPI().save(ident);

				    changedURI = ! oldURI.equals(ident.getURI());
				}




				APILocator.getVersionableAPI().setWorking(contentlet);

				boolean structureHasAHostField = hasAHostField(contentlet.getStructureInode());

				List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
				for (Field field : fields) {
				    if (field.getFieldType().equals(Field.FieldType.TAG.toString())) {
				    	String value= null;
				    	if(contentlet.getStringProperty(field.getVelocityVarName()) != null)
				    		value=contentlet.getStringProperty(field.getVelocityVarName()).trim();

				        if(UtilMethods.isSet(value)) {
    				        String hostId = Host.SYSTEM_HOST;
    				        if(structureHasAHostField){
    				            Host host = null;
    				            try{
    				                host = APILocator.getHostAPI().find(contentlet.getHost(), user, true);
    				            }catch(Exception e){
    				                Logger.error(this, "Unable to get contentlet host");
    				            }
    				            if(host.getIdentifier().equals(Host.SYSTEM_HOST))
    				                hostId = Host.SYSTEM_HOST;
    				            else
    				                hostId = host.getIdentifier();

    				        }
    				        List<Tag> list=tagAPI.getTagsInText(value, user.getUserId(), hostId);
    				        for(Tag tag : list)
    				            tagAPI.addTagInode(tag.getTagName(), contentlet.getInode(), hostId);
				        }
				    }

				}


				if (workingContentlet == null) {
				    workingContentlet = contentlet;
				}

				// DOTCMS-4732
//          if(isNewContent && !hasAHostFieldSet(contentlet.getStructureInode(),contentlet)){
//              List<Permission> stPers = perAPI.getPermissions(contentlet.getStructure());
//              if(stPers != null && stPers.size()>0){
//                  if(stPers.get(0).isIndividualPermission()){
//                      perAPI.copyPermissions(contentlet.getStructure(), contentlet);
//                  }
//              }
//          }else{
//              perAPI.resetPermissionReferences(contentlet);
//          }

				if (createNewVersion || (!createNewVersion && (contentRelationships != null || cats != null))) {
				    moveContentDependencies(workingContentlet, contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
				}

				// Refreshing permissions
				if (hasAHostField(contentlet.getStructureInode()) && !isNewContent) {
				    perAPI.resetPermissionReferences(contentlet);
				}

				// Publish once if needed and reindex once if needed. The publish
				// method reindexes.
				contentlet.setLowIndexPriority(priority);





				// http://jira.dotmarketing.net/browse/DOTCMS-1073
				// storing binary files in file system.
				Logger.debug(this, "ContentletAPIImpl : storing binary files in file system.");


				// Binary Files
				String newInode = contentlet.getInode();
                String oldInode = workingContentlet.getInode();


                java.io.File newDir = new java.io.File(APILocator.getFileAPI().getRealAssetPath() + java.io.File.separator
                		+ newInode.charAt(0)
                        + java.io.File.separator
                        + newInode.charAt(1) + java.io.File.separator + newInode);
                newDir.mkdirs();

                java.io.File oldDir = null;
                if(UtilMethods.isSet(oldInode)) {
                	oldDir = new java.io.File(APILocator.getFileAPI().getRealAssetPath()
            			+ java.io.File.separator + oldInode.charAt(0)
            			+ java.io.File.separator + oldInode.charAt(1)
            			+ java.io.File.separator + oldInode);
                }





				// loop over the new field values
				// if we have a new temp file or a deleted file
				// do it to the new inode directory
			    List<Field> structFields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
			    for (Field field : structFields) {
			        if (field.getFieldContentlet().startsWith("binary")) {
			            try {

			                String velocityVarNm = field.getVelocityVarName();
			                java.io.File incomingFile = contentletRaw.getBinary(velocityVarNm);
			                java.io.File binaryFieldFolder = new java.io.File(newDir.getAbsolutePath() + java.io.File.separator + velocityVarNm);

			                java.io.File metadata=null;
			                if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
			                    metadata=APILocator.getFileAssetAPI().getContentMetadataFile(contentlet.getInode());
			                }

			                // if the user has removed this  file via the ui
			                if (incomingFile == null  || incomingFile.getAbsolutePath().contains("-removed-")){
			                    FileUtil.deltree(binaryFieldFolder);
			                    contentlet.setBinary(velocityVarNm, null);
			                    if(metadata!=null && metadata.exists())
			                        metadata.delete();
			                	continue;
			                }

			                // if we have an incoming file
			                else if (incomingFile.exists() ){
			                	String oldFileName  = incomingFile.getName();
			                	String newFileName  = (UtilMethods.isSet(contentlet.getStringProperty("fileName")) && contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) ? contentlet.getStringProperty("fileName"): oldFileName;




			                	java.io.File oldFile = null;
			                	if(UtilMethods.isSet(oldInode)) {
			                		//get old file
			                		oldFile = new java.io.File(oldDir.getAbsolutePath()  + java.io.File.separator + velocityVarNm + java.io.File.separator +  oldFileName);

			                		// do we have an inline edited file, if so use that
					                java.io.File editedFile = new java.io.File(oldDir.getAbsolutePath()  + java.io.File.separator + velocityVarNm + java.io.File.separator + WebKeys.TEMP_FILE_PREFIX + oldFileName);
				                    if(editedFile.exists()){
				                    	incomingFile = editedFile;
				                    }
			                	}

				                java.io.File newFile = new java.io.File(newDir.getAbsolutePath()  + java.io.File.separator + velocityVarNm + java.io.File.separator +  newFileName);
				                binaryFieldFolder.mkdirs();

				                // we move files that have been newly uploaded or edited
			                	if(oldFile==null || !oldFile.equals(incomingFile)){
				                	//FileUtil.deltree(binaryFieldFolder);

			                		FileUtil.move(incomingFile, newFile);

			                		// delete old content metadata if exists
			                		if(metadata!=null && metadata.exists())
			                		    metadata.delete();

			                		// what happens is we never clean up the temp directory
			                		// answer: this happends --> https://github.com/dotCMS/dotCMS/issues/1071
			                		// there is a quarz job to clean that
			                		/*java.io.File delMe = new java.io.File(incomingFile.getParentFile().getParentFile(), oldFileName);
			                		if(delMe.exists() && delMe.getAbsolutePath().contains(
			                		        APILocator.getFileAPI().getRealAssetPathTmpBinary()
											+ java.io.File.separator + user.getUserId()
											+ java.io.File.separator  ) ){
			                			delMe.delete();
			                			delMe = incomingFile.getParentFile().getParentFile();
			                			FileUtil.deltree(delMe);
			                		}*/

			                	}
			                	else if (oldFile.exists()) {
			                		// otherwise, we copy the files as hardlinks
			                		FileUtil.copyFile(oldFile, newFile);

			                		// try to get the content metadata from the old version
			                		if(metadata!=null) {
			                		    java.io.File oldMeta=APILocator.getFileAssetAPI().getContentMetadataFile(oldInode);
			                		    if(oldMeta.exists()) {
			                		        if(metadata.exists()) // unlikely to happend. deleting just in case
			                		            metadata.delete();
			                		        metadata.getParentFile().mkdirs();
			                		        FileUtil.copyFile(oldMeta, metadata);
			                		    }
			                		}
			                	}
			                	contentlet.setBinary(velocityVarNm, newFile);
			                }
			            } catch (FileNotFoundException e) {
			                e.printStackTrace();
			                throw new DotContentletValidationException("Error occurred while processing the file:" + e.getMessage());
			            } catch (IOException e) {
			                e.printStackTrace();
			                throw new DotContentletValidationException("Error occurred while processing the file:" + e.getMessage());
			            }
			        }
			    }


			    // lets update identifier's syspubdate & sysexpiredate
			    if ((contentlet != null) && InodeUtils.isSet(contentlet.getIdentifier())) {
			        Structure st=contentlet.getStructure();
			        if(UtilMethods.isSet(st.getPublishDateVar()) || UtilMethods.isSet(st.getPublishDateVar())) {
    			        Identifier ident=APILocator.getIdentifierAPI().find(contentlet);
    			        boolean save=false;
    			        if(UtilMethods.isSet(st.getPublishDateVar())) {
    			            Date pdate=contentletRaw.getDateProperty(st.getPublishDateVar());
    			            contentlet.setDateProperty(st.getPublishDateVar(), pdate);
    			            if((ident.getSysPublishDate()==null && pdate!=null) || // was null and now we have a value
    			                (ident.getSysPublishDate()!=null && //wasn't null and now is null or different
    			                   (pdate==null || !pdate.equals(ident.getSysPublishDate())))) {
    			                ident.setSysPublishDate(pdate);
    			                save=true;
    			            }
    			        }
    			        if(UtilMethods.isSet(st.getExpireDateVar())) {
                            Date edate=contentletRaw.getDateProperty(st.getExpireDateVar());
                            contentlet.setDateProperty(st.getExpireDateVar(), edate);
                            if((ident.getSysExpireDate()==null && edate!=null) || // was null and now we have a value
                                (ident.getSysExpireDate()!=null && //wasn't null and now is null or different
                                   (edate==null || !edate.equals(ident.getSysExpireDate())))) {
                                ident.setSysExpireDate(edate);
                                save=true;
                            }
                        }
    			        if(save) {
    			            // publish/expire dates changed
    			            APILocator.getIdentifierAPI().save(ident);

    			            // we take all inodes associated with that identifier
    			            // remove them from cache and then reindex them
    			            HibernateUtil hu=new HibernateUtil(ContentletVersionInfo.class);
    			            hu.setQuery("from "+ContentletVersionInfo.class.getCanonicalName()+" where identifier=?");
    			            hu.setParam(ident.getId());
    			            List<ContentletVersionInfo> list=hu.list();
    			            List<String> inodes=new ArrayList<String>();
    			            for(ContentletVersionInfo cvi : list) {
    			                inodes.add(cvi.getWorkingInode());
    			                if(UtilMethods.isSet(cvi.getLiveInode()) && !cvi.getWorkingInode().equals(cvi.getLiveInode()))
    			                    inodes.add(cvi.getLiveInode());
    			            }
    			            for(String inode : inodes) {
    			                CacheLocator.getContentletCache().remove(inode);
    			                Contentlet ct=APILocator.getContentletAPI().find(inode, sysuser, false);
    			                APILocator.getContentletIndexAPI().addContentToIndex(ct,false);
    			            }
    			        }
			        }
			    }

				Structure hostStructure = StructureCache.getStructureByVelocityVarName("Host");
				if ((contentlet != null) && InodeUtils.isSet(contentlet.getIdentifier()) && contentlet.getStructureInode().equals(hostStructure.getInode())) {
				    HostAPI hostAPI = APILocator.getHostAPI();
				    hostAPI.updateCache(new Host(contentlet));

				    ContentletCache cc = CacheLocator.getContentletCache();
				    Identifier ident=APILocator.getIdentifierAPI().find(contentlet);
				    List<Contentlet> contentlets = findAllVersions(ident, sysuser, respectFrontendRoles);
				    for (Contentlet c : contentlets) {
						Host h = new Host(c);
						cc.remove(h.getHostname());
						cc.remove(h.getIdentifier());
					}

				    hostAPI.updateVirtualLinks(new Host(workingContentlet), new Host(contentlet));//DOTCMS-5025
				    hostAPI.updateMenuLinks(new Host(workingContentlet), new Host(contentlet));

				  //update tag references
				    String oldTagStorageId = "SYSTEM_HOST";
				    if(workingContentlet.getMap().get("tagStorage")!=null) {
				    	oldTagStorageId = workingContentlet.getMap().get("tagStorage").toString();
					}

				    String newTagStorageId = "SYSTEM_HOST";
				    if(contentlet.getMap().get("tagStorage")!=null) {
				    	newTagStorageId = contentlet.getMap().get("tagStorage").toString();
				    }
					tagAPI.updateTagReferences(contentlet.getIdentifier(), oldTagStorageId, newTagStorageId);
				}

				if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET){
				    //Parse file META-DATA
				    java.io.File binFile =  getBinaryFile(contentlet.getInode(), FileAssetAPI.BINARY_FIELD, user);
				    if(binFile!=null){
				        contentlet.setProperty(FileAssetAPI.FILE_NAME_FIELD, binFile.getName());
				        if(!UtilMethods.isSet(contentlet.getStringProperty(FileAssetAPI.DESCRIPTION))){
				            String desc = UtilMethods.getFileName(binFile.getName());
				            contentlet.setProperty(FileAssetAPI.DESCRIPTION, desc);
				        }
				        Map<String, String> metaMap = APILocator.getFileAssetAPI().getMetaDataMap(contentlet, binFile);
				        if(metaMap!=null){
				            Identifier contIdent = APILocator.getIdentifierAPI().find(contentlet);
				            Gson gson = new Gson();
				            contentlet.setProperty(FileAssetAPI.META_DATA_FIELD, gson.toJson(metaMap));
				            contentlet = conFac.save(contentlet);
				            if(!isNewContent){
				                LiveCache.removeAssetFromCache(contentlet);
				                LiveCache.addToLiveAssetToCache(contentlet);
				                WorkingCache.removeAssetFromCache(contentlet);
				                WorkingCache.addToWorkingAssetToCache(contentlet);
				                Host host = APILocator.getHostAPI().find(contIdent.getHostId(), user, respectFrontendRoles);
				                Folder folder = APILocator.getFolderAPI().findFolderByPath(contIdent.getParentPath(), host , user, respectFrontendRoles);
				                if(RefreshMenus.shouldRefreshMenus(APILocator.getFileAssetAPI().fromContentlet(workingContentlet),APILocator.getFileAssetAPI().fromContentlet(contentlet))){
				                	RefreshMenus.deleteMenu(folder);
				                	CacheLocator.getNavToolCache().removeNav(host.getIdentifier(), folder.getInode());
				                }
				            }
				        }
				    }

				}
				if (contentlet.isLive()) {
				    finishPublish(contentlet, isNewContent, createNewVersion);
				} else {
				    if (!isNewContent) {
				        ContentletServices.invalidate(contentlet, true);
				        // writes the contentlet object to a file
				        ContentletMapServices.invalidate(contentlet, true);
				    }

				    indexAPI.addContentToIndex(contentlet);
				}

				if(structureHasAHostField && changedURI) {
				    DotConnect dc=new DotConnect();
				    dc.setSQL("select working_inode,live_inode from contentlet_version_info where identifier=? and lang<>?");
				    dc.addParam(contentlet.getIdentifier());
				    dc.addParam(contentlet.getLanguageId());
				    List<Map<String,Object>> others = dc.loadResults();
				    for(Map<String,Object> other : others) {
				        String workingi=(String)other.get("working_inode");
				        indexAPI.addContentToIndex(find(workingi,user,false));
				        String livei=(String)other.get("live_inode");
				        if(UtilMethods.isSet(livei) && !livei.equals(workingi))
				            indexAPI.addContentToIndex(find(livei,user,false));
				    }
				}

				// Set the properties again after the store on DB and before the fire on an Actionlet.
				contentlet.setStringProperty("wfPublishDate", contentPushPublishDate);
				contentlet.setStringProperty("wfPublishTime", contentPushPublishTime);
				contentlet.setStringProperty("wfExpireDate", contentPushExpireDate);
				contentlet.setStringProperty("wfExpireTime", contentPushExpireTime);
				contentlet.setStringProperty("wfNeverExpire", contentPushNeverExpire);
				contentlet.setStringProperty("whereToSend", contentWhereToSend);
				contentlet.setStringProperty("forcePush", forcePush);

				//wapi.
				if(workflow!=null) {
    				workflow.setContentlet(contentlet);
    				wapi.fireWorkflowPostCheckin(workflow);
				}

				// DOTCMS-7290
				DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
				Identifier contIdent = APILocator.getIdentifierAPI().find(contentlet);
				Host host = APILocator.getHostAPI().find(contIdent.getHostId(), user, respectFrontendRoles);
				cache.remove(LiveCache.getPrimaryGroup() + host.getIdentifier() + ":" + contIdent.getParentPath()+contIdent.getAssetName(),
						LiveCache.getPrimaryGroup() + "_" + host.getIdentifier());



			} catch (Exception e) {//DOTCMS-6946
            	if(createNewVersion && workingContentlet!= null && UtilMethods.isSet(workingContentlet.getInode())){
            		APILocator.getVersionableAPI().setWorking(workingContentlet);
            	}
            	Logger.error(this, e.getMessage());
				Logger.error(this, e.toString());
				if(e instanceof DotDataException)
					throw (DotDataException)e;
				if(e instanceof DotSecurityException)
					throw (DotSecurityException)e;
				if(e instanceof DotContentletValidationException)
					throw (DotContentletValidationException)e;
				if(e instanceof DotContentletStateException)
					throw (DotContentletStateException)e;
				if(e instanceof DotWorkflowException)
					throw (DotWorkflowException)e;
				if(e instanceof Exception)
					Logger.error(this, e.toString(), e);
					throw new DotRuntimeException(e.getMessage());
			}


        } // end syncronized block


        return contentlet;
    }

    public List<Contentlet> checkout(List<Contentlet> contentlets, User user,   boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        List<Contentlet> result = new ArrayList<Contentlet>();
        for (Contentlet contentlet : contentlets) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    public List<Contentlet> checkoutWithQuery(String luceneQuery, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, ParseException {
        List<Contentlet> result = new ArrayList<Contentlet>();
        List<Contentlet> cons = search(luceneQuery, 0, -1, "", user, respectFrontendRoles);
        for (Contentlet contentlet : cons) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    public List<Contentlet> checkout(String luceneQuery, User user,boolean respectFrontendRoles, int offset, int limit) throws DotDataException,DotSecurityException, DotContentletStateException, ParseException {
        List<Contentlet> result = new ArrayList<Contentlet>();
        List<Contentlet> cons = search(luceneQuery, limit, offset, "", user, respectFrontendRoles);
        for (Contentlet contentlet : cons) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    public Contentlet checkout(String contentletInode, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
        //return new version
        Contentlet contentlet = find(contentletInode, user, respectFrontendRoles);

        canLock(contentlet, user);
        lock(contentlet, user, respectFrontendRoles);
        Contentlet workingContentlet = new Contentlet();
        Map<String, Object> cmap = contentlet.getMap();
        workingContentlet.setStructureInode(contentlet.getStructureInode());
        workingContentlet.setInode(contentletInode);
        copyProperties(workingContentlet, cmap);
        workingContentlet.setInode("");
        return workingContentlet;
    }

    private void moveContentDependencies(Contentlet fromContentlet, Contentlet toContentlet, ContentletRelationships contentRelationships, List<Category> categories ,List<Permission> permissions, User user,boolean respect) throws DotDataException, DotSecurityException{

        //Handles Categories
        List<Category> categoriesUserCannotRemove = new ArrayList<Category>();
        if(categories == null){
            categories = new ArrayList<Category>();
        }
        //Find categories which the user can't use.  A user cannot remove a category they cannot use
        List<Category> cats = catAPI.getParents(fromContentlet, APILocator.getUserAPI().getSystemUser(), true);
        for (Category category : cats) {
            if(!catAPI.canUseCategory(category, user, false)){
                if(!categories.contains(category)){
                    categoriesUserCannotRemove.add(category);
                }
            }
        }
        categories = perAPI.filterCollection(categories, PermissionAPI.PERMISSION_USE, respect, user);
        categories.addAll(categoriesUserCannotRemove);
        if(!categories.isEmpty())
           catAPI.setParents(toContentlet, categories, user, respect);


        //Handle Relationships

        if(contentRelationships == null){
            contentRelationships = new ContentletRelationships(toContentlet);
        }
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(fromContentlet.getStructure());
        for (Relationship r : rels) {
            if(RelationshipFactory.isSameStructureRelationship(r, fromContentlet.getStructure())) {
                ContentletRelationshipRecords selectedRecords = null;

                //First all relationships as parent
                for(ContentletRelationshipRecords records : contentRelationships.getRelationshipsRecords()) {
                    if(records.getRelationship().getInode().equalsIgnoreCase(r.getInode()) && records.isHasParent()) {
                        selectedRecords = records;
                        break;
                    }
                }
                if (selectedRecords == null) {
                    selectedRecords = contentRelationships.new ContentletRelationshipRecords(r, true);
                    contentRelationships.getRelationshipsRecords().add(contentRelationships.new ContentletRelationshipRecords(r, true));
                }

                //Adding to the list all the records the user was not able to see becuase permissions forcing them into the relationship
                List<Contentlet> cons = getRelatedContent(fromContentlet, r, true, APILocator.getUserAPI().getSystemUser(), true);
                for (Contentlet contentlet : cons) {
                    if (!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false)) {
                        selectedRecords.getRecords().add(0, contentlet);
                    }
                }

                //Then all relationships as child
                for(ContentletRelationshipRecords records : contentRelationships.getRelationshipsRecords()) {
                    if(records.getRelationship().getInode().equalsIgnoreCase(r.getInode()) && !records.isHasParent()) {
                        selectedRecords = records;
                        break;
                    }
                }
                if (selectedRecords == null) {
                    selectedRecords = contentRelationships.new ContentletRelationshipRecords(r, false);
                    contentRelationships.getRelationshipsRecords().add(contentRelationships.new ContentletRelationshipRecords(r, false));
                }

                //Adding to the list all the records the user was not able to see becuase permissions forcing them into the relationship
                cons = getRelatedContent(fromContentlet, r, false, APILocator.getUserAPI().getSystemUser(), true);
                for (Contentlet contentlet : cons) {
                    if (!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false)) {
                        selectedRecords.getRecords().add(0, contentlet);
                    }
                }

            } else {
                ContentletRelationshipRecords selectedRecords = null;

                //First all relationships as parent
                for(ContentletRelationshipRecords records : contentRelationships.getRelationshipsRecords()) {
                    if(records.getRelationship().getInode().equalsIgnoreCase(r.getInode())) {
                        selectedRecords = records;
                        break;
                    }
                }
                boolean hasParent = RelationshipFactory.isParentOfTheRelationship(r, fromContentlet.getStructure());
                if (selectedRecords == null) {
                    selectedRecords = contentRelationships.new ContentletRelationshipRecords(r, hasParent);
                    contentRelationships.getRelationshipsRecords().add(contentRelationships.new ContentletRelationshipRecords(r, hasParent));
                }

                //Adding to the list all the records the user was not able to see because permissions forcing them into the relationship
                List<Contentlet> cons = getRelatedContent(fromContentlet, r, APILocator.getUserAPI().getSystemUser(), true);
                for (Contentlet contentlet : cons) {
                    if (!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false)) {
                        selectedRecords.getRecords().add(0, contentlet);
                    }
                }
            }
        }
        for (ContentletRelationshipRecords cr : contentRelationships.getRelationshipsRecords()) {
            relateContent(toContentlet, cr, APILocator.getUserAPI().getSystemUser(), true);
        }
    }

    public void restoreVersion(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotSecurityException, DotContentletStateException, DotDataException {
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot edit Contentlet");
        }
        if(contentlet == null){
            throw new DotContentletStateException("The contentlet was null");
        }
        canLock(contentlet, user);
        Contentlet currentWorkingCon = findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(), user, respectFrontendRoles);
        APILocator.getVersionableAPI().setWorking(contentlet);
        // Upodating lucene index
        ContentletServices.invalidate(contentlet, true);
        //writes the contentlet object to a file
        ContentletMapServices.invalidate(contentlet, true);
        // Updating lucene index
        indexAPI.addContentToIndex(currentWorkingCon);
        indexAPI.addContentToIndex(contentlet);
    }

    public List<Contentlet> findAllUserVersions(Identifier identifier,User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException {
        List<Contentlet> contentlets = conFac.findAllUserVersions(identifier);
        if(contentlets.isEmpty())
            return new ArrayList<Contentlet>();
        if(!perAPI.doesUserHavePermission(contentlets.get(0), PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet So Unable to View Versions");
        }
        return contentlets;
    }

    public List<Contentlet> findAllVersions(Identifier identifier, User user,boolean respectFrontendRoles) throws DotSecurityException,DotDataException, DotStateException {
        List<Contentlet> contentlets = conFac.findAllVersions(identifier);
        if(contentlets.isEmpty())
            return new ArrayList<Contentlet>();
        if(!perAPI.doesUserHavePermission(contentlets.get(0), PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet So Unable to View Versions");
        }
        return contentlets;
    }

    public String getName(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException,DotContentletStateException, DotDataException {
        if(!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
            throw new DotSecurityException("User cannot read Contentlet1");
        }
        if(contentlet == null){
            throw new DotContentletStateException("The contentlet was null");
        }
        String returnValue = (String) contentlet.getMap().get("__DOTNAME__");
        if(UtilMethods.isSet(returnValue)){
        	return returnValue;
        }


        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());

        for (Field fld : fields) {

            try{

                if(fld.isListed() && contentlet.getMap().get(fld.getVelocityVarName())!=null){
                    returnValue = contentlet.getMap().get(fld.getVelocityVarName()).toString();
                    returnValue = returnValue.length() > 250 ? returnValue.substring(0,250) : returnValue;
                    if(UtilMethods.isSet(returnValue)){
                    	contentlet.setStringProperty("__DOTNAME__", returnValue);
                    	return returnValue;
                    }
                }
            }
            catch(Exception e){
                Logger.warn(this.getClass(), "unable to get field value " + fld.getVelocityVarName() + " " + e, e);
            }
        }
        contentlet.setStringProperty("__NAME__", contentlet.getIdentifier());
        return contentlet.getIdentifier();
    }

    /**
     * This is the original method that copy the properties of one contentlet to another, this is tge original firm and call the overloaded firm with checkIsUnique false
     */

    public void copyProperties(Contentlet contentlet,Map<String, Object> properties) throws DotContentletStateException,DotSecurityException {
        boolean checkIsUnique = false;
        copyProperties(contentlet,properties, checkIsUnique);
    }

    /**
     * This is the new method of the copyProperties that copy one contentlet to another, the checkIsUnique should be by default false, it check if a String value is
     * unique and add a (Copy) string to the end of the field value, this method is called several times, so is important to call it with checkIsUnique false all the times
     * @param contentlet the new contentlet to the filled
     * @param properties the map with the fields and values of the old contentlet
     * @param checkIsUnique the variable that establish if the unique string values should be modified or not
     * @throws DotContentletStateException
     * @throws DotSecurityException
     */

    public void copyProperties(Contentlet contentlet,Map<String, Object> properties,boolean checkIsUnique) throws DotContentletStateException,DotSecurityException {
        if(!InodeUtils.isSet(contentlet.getStructureInode())){
            Logger.warn(this,"Cannot copy properties to contentlet where structure inode < 1 : You must set the structure's inode");
            return;
        }
        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        List<String> fieldNames = new ArrayList<String>();
        Map<String, Field> velFieldmap = new HashMap<String, Field>();

        for (Field field : fields) {
            if(!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) && !field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
            fieldNames.add(field.getFieldName());
            velFieldmap.put(field.getVelocityVarName(),field);
            }
        }
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if(fieldNames.contains(property.getKey())){
                Logger.debug(this, "The map found a field not within the contentlet's structure");
            }
            if(property.getValue() == null)
                continue;
            if((!property.getKey().equals("recurrence"))&&!(property.getValue() instanceof String || property.getValue() instanceof Boolean ||property.getValue() instanceof java.io.File || property.getValue() instanceof Float || property.getValue() instanceof Integer || property.getValue() instanceof Date || property.getValue() instanceof Long || property.getValue() instanceof List)){
                throw new DotContentletStateException("The map contains an invalid value");
            }
        }

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String conVariable = property.getKey();
            Object value = property.getValue();
            try{
                if(conVariable.equals(Contentlet.INODE_KEY)){
                    contentlet.setInode((String)value);
                }else if(conVariable.equals(Contentlet.LANGUAGEID_KEY)){
                    contentlet.setLanguageId((Long)value);
                }else if(conVariable.equals(Contentlet.STRUCTURE_INODE_KEY)){
                    contentlet.setStructureInode((String)value);
                }else if(conVariable.equals(Contentlet.LAST_REVIEW_KEY)){
                    contentlet.setLastReview((Date)value);
                }else if(conVariable.equals(Contentlet.NEXT_REVIEW_KEY)){
                    contentlet.setNextReview((Date)value);
                }else if(conVariable.equals(Contentlet.REVIEW_INTERNAL_KEY)){
                    contentlet.setReviewInterval((String)value);
                }else if(conVariable.equals(Contentlet.DISABLED_WYSIWYG_KEY)){
                    contentlet.setDisabledWysiwyg((List<String>)value);
                }else if(conVariable.equals(Contentlet.MOD_DATE_KEY)){
                    contentlet.setModDate((Date)value);
                }else if(conVariable.equals(Contentlet.MOD_USER_KEY)){
                    contentlet.setModUser((String)value);
                }else if(conVariable.equals(Contentlet.OWNER_KEY)){
                    contentlet.setOwner((String)value);
                }else if(conVariable.equals(Contentlet.IDENTIFIER_KEY)){
                    contentlet.setIdentifier((String)value);
                }else if(conVariable.equals(Contentlet.SORT_ORDER_KEY)){
                    contentlet.setSortOrder((Long)value);
                }else if(conVariable.equals(Contentlet.HOST_KEY)){
                    contentlet.setHost((String)value);
                }else if(conVariable.equals(Contentlet.FOLDER_KEY)){
                    contentlet.setFolder((String)value);
                }else if(velFieldmap.get(conVariable) != null){
                    Field field = velFieldmap.get(conVariable);
                    if(isFieldTypeString(field)) //|| field.getFieldType().equals(Field.FieldType.BINARY.toString()))
                    {
                        if(checkIsUnique && field.isUnique())
                        {
                            String dataType = (field.getFieldContentlet() != null) ? field.getFieldContentlet().replaceAll("[0-9]*", "") : "";
                            value = value + " (COPY)";
                        }
                        contentlet.setStringProperty(conVariable, value != null ? (String)value : null);
                    }else if(isFieldTypeBoolean(field)){
                        contentlet.setBoolProperty(conVariable, value != null ? (Boolean)value : null);
                    }else if(isFieldTypeFloat(field)){
                        contentlet.setFloatProperty(conVariable, value != null ? (Float)value : null);
                    }else if(isFieldTypeDate(field)){
                        contentlet.setDateProperty(conVariable,value != null ? (Date)value : null);
                    }else if(isFieldTypeLong(field)){
                        contentlet.setLongProperty(conVariable,value != null ? (Long)value : null);
                    }else if(isFieldTypeBinary(field)){
                        contentlet.setBinary(conVariable,(java.io.File)value);
                    }
                }else{
                    Logger.debug(this,"Value " + value + " in map cannot be set to contentlet");
                }
            }catch (ClassCastException cce) {
                Logger.error(this,"Value in map cannot be set to contentlet", cce);
            } catch (IOException ioe) {
                Logger.error(this,"IO Error in copying Binary File object ", ioe);
            }

            // workflow
            contentlet.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, (String) properties.get(Contentlet.WORKFLOW_ACTION_KEY));
            contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, (String) properties.get(Contentlet.WORKFLOW_COMMENTS_KEY));
            contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, (String) properties.get(Contentlet.WORKFLOW_ASSIGN_KEY));


            contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, (String) properties.get(Contentlet.WORKFLOW_PUBLISH_DATE));
            contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, (String) properties.get(Contentlet.WORKFLOW_PUBLISH_TIME));
            contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE, (String) properties.get(Contentlet.WORKFLOW_EXPIRE_DATE));
            contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME, (String) properties.get(Contentlet.WORKFLOW_EXPIRE_TIME));
            contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, (String) properties.get(Contentlet.WORKFLOW_NEVER_EXPIRE));






        }
    }

    public List<Contentlet> find(Category category, long languageId,boolean live,String orderBy,User user, boolean respectFrontendRoles) throws DotDataException,DotContentletStateException, DotSecurityException {
        List<Category> cats  = new ArrayList<Category>();
        return find(cats,languageId, live, orderBy, user, respectFrontendRoles);
    }

    public List<Contentlet> find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles)  throws DotDataException, DotContentletStateException,DotSecurityException {
        if(categories == null || categories.size() < 1)
            return new ArrayList<Contentlet>();
        StringBuffer buffy = new StringBuffer();
        buffy.append("+type:content +deleted:false");
        if(live)
            buffy.append(" +live:true");
        else
            buffy.append(" +working:true");
        if(languageId > 0)
            buffy.append(" +languageId:" + languageId);
        for (Category category : categories) {
            buffy.append(" +c" + category.getInode() + "c:on");
        }
        try {
            return search(buffy.toString(), 0, -1, orderBy, user, respectFrontendRoles);
        } catch (Exception pe) {
            Logger.error(this,"Unable to search for contentlets" ,pe);
            throw new DotContentletStateException("Unable to search for contentlets", pe);
        }
    }

    public void setContentletProperty(Contentlet contentlet,Field field, Object value)throws DotContentletStateException {
        String[] dateFormats = new String[] { "yyyy-MM-dd HH:mm", "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm",
                "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy",
                "hh:mm:ss aa", "HH:mm:ss", "yyyy-MM-dd"};
        if(contentlet == null){
            throw new DotContentletValidationException("The contentlet must not be null");
        }
        String stInode = contentlet.getStructureInode();
        if(!InodeUtils.isSet(stInode)){
            throw new DotContentletValidationException("The contentlet's structureInode must be set");
        }
        if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())){

        }else if(fAPI.isElementConstant(field)){
            Logger.debug(this, "Cannot set contentlet field value on field type constant. Value is saved to the field not the contentlet");
        }else if(field.getFieldContentlet().startsWith("text")){
            try{
                contentlet.setStringProperty(field.getVelocityVarName(), (String)value);
            }catch (Exception e) {
                contentlet.setStringProperty(field.getVelocityVarName(),value.toString());
            }
        }else if(field.getFieldContentlet().startsWith("long_text")){
            try{
                contentlet.setStringProperty(field.getVelocityVarName(), (String)value);
            }catch (Exception e) {
                contentlet.setStringProperty(field.getVelocityVarName(),value.toString());
            }
        }else if(field.getFieldContentlet().startsWith("date")){
            if(value instanceof Date){
                contentlet.setDateProperty(field.getVelocityVarName(), (Date)value);
            }else if(value instanceof String){
                if(((String) value).trim().length()>0) {
                    try {
                        contentlet.setDateProperty(field.getVelocityVarName(),
                                DateUtil.convertDate((String)value, dateFormats));
                    }catch (Exception e) {
                        throw new DotContentletStateException("Unable to convert string to date " + value);
                    }
                }
                else {
                    contentlet.setDateProperty(field.getVelocityVarName(), null);
                }
            }else if(field.isRequired() && value==null){
                throw new DotContentletStateException("Date fields must either be of type String or Date");
            }
        }else if(field.getFieldContentlet().startsWith("bool")){
            if(value instanceof Boolean){
                contentlet.setBoolProperty(field.getVelocityVarName(), (Boolean)value);
            }else if(value instanceof String){
                try{
                    String auxValue = (String) value;
                    Boolean auxBoolean = (auxValue.equalsIgnoreCase("1") || auxValue.equalsIgnoreCase("true") || auxValue.equalsIgnoreCase("t")) ? Boolean.TRUE : Boolean.FALSE;
                    contentlet.setBoolProperty(field.getVelocityVarName(), auxBoolean);
                }catch (Exception e) {
                    throw new DotContentletStateException("Unable to set string value as a Boolean");
                }
            }else{
                throw new DotContentletStateException("Boolean fields must either be of type String or Boolean");
            }
        }else if(field.getFieldContentlet().startsWith("float")){
            if(value instanceof Number){
                contentlet.setFloatProperty(field.getVelocityVarName(),((Number)value).floatValue());
            }else if(value instanceof String){
                try{
                    contentlet.setFloatProperty(field.getVelocityVarName(),new Float((String)value));
                }catch (Exception e) {
                    throw new DotContentletStateException("Unable to set string value as a Float");
                }
            }
        }else if(field.getFieldContentlet().startsWith("integer")){
            if(value instanceof Number){
                contentlet.setLongProperty(field.getVelocityVarName(),((Number)value).longValue());
            }else if(value instanceof String){
                try{
                    contentlet.setLongProperty(field.getVelocityVarName(),new Long((String)value));
                }catch (Exception e) {
                    throw new DotContentletStateException("Unable to set string value as a Long");
                }
            }
            // http://jira.dotmarketing.net/browse/DOTCMS-1073
            // setBinary
            }else if(field.getFieldContentlet().startsWith("binary")){
                try{
                	System.out.println(value.getClass());
                    contentlet.setBinary(field.getVelocityVarName(), (java.io.File) value);
                }catch (Exception e) {
                    throw new DotContentletStateException("Unable to set binary file Object");
                }
        }else{
            throw new DotContentletStateException("Unable to set value : Unknown field type");
        }
    }


    private static final String[] SPECIAL_CHARS = new String[] { "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[",
        "]", "^", "\"", "?", ":", "\\" };

    private static String escape(String text) {
        for (int i = SPECIAL_CHARS.length - 1; i >= 0; i--) {
            text = StringUtils.replace(text, SPECIAL_CHARS[i], "\\" + SPECIAL_CHARS[i]);
        }

        return text;
    }

    public void validateContentlet(Contentlet contentlet,List<Category> cats)throws DotContentletValidationException {
        if(contentlet == null){
            throw new DotContentletValidationException("The contentlet must not be null");
        }
        String stInode = contentlet.getStructureInode();
        if(!InodeUtils.isSet(stInode)){
            throw new DotContentletValidationException("The contentlet's structureInode must be set");
        }
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        if(Structure.STRUCTURE_TYPE_FILEASSET==st.getStructureType()){
            if(contentlet.getHost()!=null && contentlet.getHost().equals(Host.SYSTEM_HOST) && (!UtilMethods.isSet(contentlet.getFolder()) || contentlet.getFolder().equals(FolderAPI.SYSTEM_FOLDER))){
                DotContentletValidationException cve = new FileAssetValidationException("message.contentlet.fileasset.invalid.hostfolder");
                cve.addBadTypeField(st.getFieldVar(FileAssetAPI.HOST_FOLDER_FIELD));
                throw cve;
            }
            boolean fileNameExists = false;
            try {
                Host host = APILocator.getHostAPI().find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), false);
                Folder folder = null;
                if(UtilMethods.isSet(contentlet.getFolder()))
                    folder=APILocator.getFolderAPI().find(contentlet.getFolder(), APILocator.getUserAPI().getSystemUser(), false);
                else
                    folder=APILocator.getFolderAPI().findSystemFolder();
                String fileName = contentlet.getBinary(FileAssetAPI.BINARY_FIELD)!=null?contentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName():"";
                if(UtilMethods.isSet(contentlet.getStringProperty("fileName")))//DOTCMS-7093
                	fileName = contentlet.getStringProperty("fileName");
                if(UtilMethods.isSet(fileName)){
                    fileNameExists = APILocator.getFileAssetAPI().fileNameExists(host,folder,fileName,contentlet.getIdentifier());
                    if(!APILocator.getFolderAPI().matchFilter(folder, fileName)) {
                        DotContentletValidationException cve = new FileAssetValidationException("message.file_asset.error.filename.filters");
                        cve.addBadTypeField(st.getFieldVar(FileAssetAPI.HOST_FOLDER_FIELD));
                        throw cve;
                    }
                }

            } catch (Exception e) {
            	if(e instanceof FileAssetValidationException)
            		throw (FileAssetValidationException)e ;
                throw new FileAssetValidationException("Unable to validate field: " + FileAssetAPI.BINARY_FIELD,e);
            }
            if(fileNameExists){
                DotContentletValidationException cve = new FileAssetValidationException("message.contentlet.fileasset.filename.already.exists");
                cve.addBadTypeField(st.getFieldVar(FileAssetAPI.HOST_FOLDER_FIELD));
                throw cve;
            }


        }

        boolean hasError = false;
        DotContentletValidationException cve = new DotContentletValidationException("Contentlets' fields are not valid");
        List<Field> fields = FieldsCache.getFieldsByStructureInode(stInode);
        Map<String, Object> conMap = contentlet.getMap();
        for (Field field : fields) {
            Object o = conMap.get(field.getVelocityVarName());
            if(o != null){
                if(isFieldTypeString(field)){
                    if(!(o instanceof String)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A text contentlet must be of type String");
                    }
                }else if(isFieldTypeDate(field)){
                    if(!(o instanceof Date)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A date contentlet must be of type Date");
                    }
                }else if(isFieldTypeBoolean(field)){
                    if(!(o instanceof Boolean)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A bool contentlet must be of type Boolean");
                    }
                }else if(isFieldTypeFloat(field)){
                    if(!(o instanceof Float)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A float contentlet must be of type Float");
                    }
                }else if(isFieldTypeLong(field)){
                    if(!(o instanceof Long || o instanceof Integer)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A integer contentlet must be of type Long or Integer");
                    }
                    //  http://jira.dotmarketing.net/browse/DOTCMS-1073
                    //  binary field validation
                }else if(isFieldTypeBinary(field)){
                    if(!(o instanceof java.io.File)){
                        cve.addBadTypeField(field);
                        Logger.error(this,"A binary contentlet field must be of type File");
                    }
                }else if(isFieldTypeSystem(field) || isFieldTypeConstant(field)){

                }else{
                    Logger.error(this,"Found an unknown field type : This should never happen!!!");
                    throw new DotContentletStateException("Unknown field type");
                }
            }
            if (field.isRequired()) {
                if(o instanceof String){
                    String s1 = (String)o;
                    if(!UtilMethods.isSet(s1.trim())) {
                        cve.addRequiredField(field);
                        hasError = true;
                        continue;
                    }
                }
                else if( field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) ) {
                    if( cats == null || cats.size() == 0 ) {
                        cve.addRequiredField(field);
                        hasError = true;
                        continue;
                    }
                    try {
                        User systemUser = APILocator.getUserAPI().getSystemUser();
                        if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
                            CategoryAPI catAPI = APILocator.getCategoryAPI();
                            Category baseCat = catAPI.find(field.getValues(), systemUser, false);
                            List<Category> childrenCats = catAPI.getAllChildren(baseCat, systemUser, false);
                            boolean found = false;
                            for(Category cat : childrenCats) {
                                for(Category passedCat : cats) {
                                    try {
                                        if(passedCat.getInode().equalsIgnoreCase(cat.getInode()))
                                            found = true;
                                    } catch (NumberFormatException e) { }
                                }
                            }
                            if(!found) {
                                cve.addRequiredField(field);
                                hasError = true;
                                continue;
                            }
                        }
                    } catch (DotDataException e) {
                        throw new DotContentletValidationException("Unable to validate a category field: " + field.getVelocityVarName(), e);
                    } catch (DotSecurityException e) {
                        throw new DotContentletValidationException("Unable to validate a category field: " + field.getVelocityVarName(), e);
                    }
                } else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                    if (!UtilMethods.isSet(contentlet.getHost()) && !UtilMethods.isSet(contentlet.getFolder())) {
                        cve.addRequiredField(field);
                        hasError = true;
                        continue;
                    }
                } else if(!UtilMethods.isSet(o)) {
                    cve.addRequiredField(field);
                    hasError = true;
                    continue;
                }
                if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) || field.getFieldType().equals(Field.FieldType.FILE.toString())){
                    if(o instanceof Number){
                        Number n = (Number)o;
                        if(n.longValue() == 0){
                            cve.addRequiredField(field);
                            hasError = true;
                            continue;
                        }
                    }else if(o instanceof String){
                        String s = (String)o;
                        if(s.trim().equals("0")){
                            cve.addRequiredField(field);
                            hasError = true;
                            continue;
                        }
                    }
                    //WYSIWYG patch for blank content
                }else if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())){
                    if(o instanceof String){
                        String s = (String)o;
                        if (s.trim().toLowerCase().equals("<br>")){
                            cve.addRequiredField(field);
                            hasError = true;
                            continue;
                        }
                    }
                }
            }
            if(field.isUnique()){
            	try{
                StringBuilder buffy = new StringBuilder();

                buffy.append(" +(live:true working:true)");
                buffy.append(" +structureInode:" + contentlet.getStructureInode());
                buffy.append(" +languageId:" + contentlet.getLanguageId());
                buffy.append(" +(working:true live:true)");
                if(UtilMethods.isSet(contentlet.getIdentifier())){
                    buffy.append(" -(identifier:" + contentlet.getIdentifier() + ")");
                }
                buffy.append(" +" + contentlet.getStructure().getVelocityVarName() + "." + field.getVelocityVarName() + ":\"" + escape(getFieldValue(contentlet, field).toString()) + "\"");
                List<ContentletSearch> contentlets = new ArrayList<ContentletSearch>();
                try {
                    contentlets = searchIndex(buffy.toString(), -1, 0, "inode", APILocator.getUserAPI().getSystemUser(), false);
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(),e);
                    throw new DotContentletValidationException(e.getMessage(),e);
                }
                int size = contentlets.size();
                if(size > 0 && !hasError){

                	Boolean unique = true;
					for (ContentletSearch contentletSearch : contentlets) {
						Contentlet c = conFac.find(contentletSearch.getInode());
						Map<String, Object> cMap = c.getMap();
						Object obj = cMap.get(field.getVelocityVarName());

						if(((String) obj).equals(((String) o))) { //DOTCMS-7275
							unique = false;
							break;
						}

					}

					if(!unique) {
	                    if(UtilMethods.isSet(contentlet.getIdentifier())){//DOTCMS-5409
	                        Iterator<ContentletSearch> contentletsIter = contentlets.iterator();
	                        while (contentletsIter.hasNext()) {
	                            ContentletSearch cont = (ContentletSearch) contentletsIter.next();
	                                if(!contentlet.getIdentifier().equalsIgnoreCase(cont.getIdentifier()))
	                                {
	                                    cve.addUniqueField(field);
	                                    hasError = true;
	                                    break;
	                                }

	                        }
	                    }else{
	                        cve.addUniqueField(field);
	                        hasError = true;
	                        break;
	                    }
					}
                }

            	} catch (DotDataException e) {
					Logger.error(this,"Unable to get contentlets for structure: " + contentlet.getStructure().getName() ,e);
				} catch (DotSecurityException e) {
					Logger.error(this,"Unable to get contentlets for structure: " + contentlet.getStructure().getName() ,e);
				}
            }
            String dataType = (field.getFieldContentlet() != null) ? field.getFieldContentlet().replaceAll("[0-9]*", "") : "";
            if (UtilMethods.isSet(o) && dataType.equals("text")) {
                String s = "";
                try{
                    s = (String)o;
                }catch (Exception e) {
                    Logger.error(this,"Unable to get string value for text field in contentlet",e);
                    continue;
                }
                if (s.length() > 255) {
                    hasError = true;
                    cve.addMaxLengthField(field);
                    continue;
                }
            }
            String regext = field.getRegexCheck();
            if (UtilMethods.isSet(regext)) {
                if (UtilMethods.isSet(o)) {
                    if(o instanceof Number){
                        Number n = (Number)o;
                        String s = n.toString();
                        boolean match = Pattern.matches(regext, s);
                        if (!match) {
                            hasError = true;
                            cve.addPatternField(field);
                            continue;
                        }
                    }else if(o instanceof String && UtilMethods.isSet(((String)o).trim())){
                        String s = ((String)o).trim();
                        boolean match = Pattern.matches(regext, s);
                        if (!match) {
                            hasError = true;
                            cve.addPatternField(field);
                            continue;
                        }
                    }
                }
            }
        }
        if(hasError){
            throw cve;
        }
    }

    public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats)throws DotContentletValidationException {
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for(Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = (Relationship) relEntry.getKey();
            boolean hasParent = RelationshipFactory.isParentOfTheRelationship(relationship, st);
            ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
        }
        validateContentlet(contentlet, relationshipsData, cats);
    }

    public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats)throws DotContentletValidationException {
        if(contentlet.getMap().get("_dont_validate_me") != null)
            return;

        DotContentletValidationException cve = new DotContentletValidationException("Contentlet's fields are not valid");
        boolean hasError = false;
        String stInode = contentlet.getStructureInode();
        if(!InodeUtils.isSet(stInode)){
            throw new DotContentletValidationException("The contentlet's structureInode must be set");
        }
        try{
            validateContentlet(contentlet,cats);
        }catch (DotContentletValidationException ve) {
            cve = ve;
            hasError = true;
        }
        if( contentRelationships != null ) {
            List<ContentletRelationshipRecords> records = contentRelationships.getRelationshipsRecords();
            for (ContentletRelationshipRecords cr : records) {
                Relationship rel = cr.getRelationship();
                List<Contentlet> cons = cr.getRecords();
                if(cons == null)
                    cons = new ArrayList<Contentlet>();
                //if i am the parent
                if(rel.getParentStructureInode().equalsIgnoreCase(stInode)){
                    if(rel.isChildRequired() && cons.isEmpty()){
                        hasError = true;
                        cve.addRequiredRelationship(rel, cons);
                    }
                    for(Contentlet con : cons){
                        if(!con.getStructureInode().equalsIgnoreCase(rel.getChildStructureInode())){
                            hasError = true;
                            cve.addInvalidContentRelationship(rel, cons);
                        }
                    }
                }else if(rel.getChildStructureInode().equalsIgnoreCase(stInode)){
                    if(rel.isParentRequired() && cons.isEmpty()){
                        hasError = true;
                        cve.addRequiredRelationship(rel, cons);
                    }
                    for(Contentlet con : cons){
                        if(!con.getStructureInode().equalsIgnoreCase(rel.getParentStructureInode())){
                            hasError = true;
                            cve.addInvalidContentRelationship(rel, cons);
                        }
                    }
                }else{
                    hasError = true;
                    cve.addBadRelationship(rel, cons);
                }
            }
        }
        if(hasError){
            throw cve;
        }
    }

    public boolean isFieldTypeBoolean(Field field) {
        if(field.getFieldContentlet().startsWith("bool")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeDate(Field field) {
        if(field.getFieldContentlet().startsWith("date")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeFloat(Field field) {
        if(field.getFieldContentlet().startsWith("float")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeLong(Field field) {
        if(field.getFieldContentlet().startsWith("integer")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeString(Field field) {
        if(field.getFieldContentlet().startsWith("text")){
            return true;
        }
        return false;
    }

    //  http://jira.dotmarketing.net/browse/DOTCMS-1073
    public boolean isFieldTypeBinary(Field field) {
        if(field.getFieldContentlet().startsWith("binary")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeSystem(Field field) {
        if(field.getFieldContentlet().startsWith("system")){
            return true;
        }
        return false;
    }

    public boolean isFieldTypeConstant(Field field) {
        if(field.getFieldContentlet().startsWith("constant")){
            return true;
        }
        return false;
    }




    /* (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#convertContentletToFatContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.business.Contentlet)
     */
    public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet(
            Contentlet cont,
            com.dotmarketing.portlets.contentlet.business.Contentlet fatty)
    throws DotDataException {
        return conFac.convertContentletToFatContentlet(cont, fatty);
    }

    /* (non-Javadoc)
     * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#convertFatContentletToContentlet(com.dotmarketing.portlets.contentlet.business.Contentlet)
     */
    public Contentlet convertFatContentletToContentlet(
            com.dotmarketing.portlets.contentlet.business.Contentlet fatty)
    throws DotDataException, DotSecurityException {
        return conFac.convertFatContentletToContentlet(fatty);
    }

    private Contentlet findWorkingContentlet(Contentlet content)throws DotSecurityException, DotDataException, DotContentletStateException{
        Contentlet con = null;
        List<Contentlet> workingCons = new ArrayList<Contentlet>();
        if(InodeUtils.isSet(content.getIdentifier())){
            workingCons = conFac.findContentletsByIdentifier(content.getIdentifier(), false, content.getLanguageId());
        }
        if(workingCons.size() > 0)
            con = workingCons.get(0);
        if(workingCons.size()>1)
            Logger.warn(this, "Multiple working contentlets found for identifier:" + content.getIdentifier() + " with languageid:" + content.getLanguageId() + " returning the lastest modified.");
        return con;
    }

    private Map<Relationship, List<Contentlet>> findContentRelationships(Contentlet contentlet) throws DotDataException, DotSecurityException{
        Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
        if(contentlet == null)
            return contentRelationships;
        List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
        for (Relationship r : rels) {
            if(!contentRelationships.containsKey(r)){
                contentRelationships.put(r, new ArrayList<Contentlet>());
            }
            List<Contentlet> cons = getRelatedContent(contentlet, r, APILocator.getUserAPI().getSystemUser(), true);
            for (Contentlet c : cons) {
                List<Contentlet> l = contentRelationships.get(r);
                l.add(c);
            }
        }
        return contentRelationships;
    }

    public int deleteOldContent(Date deleteFrom) throws DotDataException {
        int results = 0;
        if(deleteFrom == null){
            throw new DotDataException("Date to delete from must not be null");
        }
        results = conFac.deleteOldContent(deleteFrom);
        return results;
    }

    public List<String> findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles) throws DotDataException {
        List<String> result = new ArrayList<String>();

        List<Contentlet> contentlets;
        if (field.isIndexed()) {
            contentlets = new ArrayList<Contentlet>();
            List<Contentlet> tempContentlets = new ArrayList<Contentlet>();
            int limit = 500;

            StringBuilder query = new StringBuilder("+deleted:false +live:true +structureInode:" + structureInode);

            try {
                tempContentlets = search(query.toString(), limit, 0, field.getFieldContentlet(), user, respectFrontEndRoles, PermissionAPI.PERMISSION_READ);
                if (0 < tempContentlets.size())
                    contentlets.addAll(tempContentlets);

                for (int offset = limit; 0 < tempContentlets.size(); offset+=limit) {
                    tempContentlets = search(query.toString(), limit, offset, field.getFieldContentlet(), user, respectFrontEndRoles, PermissionAPI.PERMISSION_READ);
                    if (0 < tempContentlets.size())
                        contentlets.addAll(tempContentlets);
                }
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        } else {
            contentlets = conFac.findContentletsWithFieldValue(structureInode, field);
            try {
                contentlets = perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ, respectFrontEndRoles, user);
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        }

        String value;
        for (Contentlet contentlet: contentlets) {
            try {
                value = null;
                if (field.getFieldType().equals(Field.DataType.BOOL))
                    value = "" + contentlet.getBoolProperty(field.getVelocityVarName());
                else if (field.getFieldType().equals(Field.DataType.DATE))
                    value = "" + contentlet.getDateProperty(field.getVelocityVarName());
                else if (field.getFieldType().equals(Field.DataType.FLOAT))
                    value = "" + contentlet.getFloatProperty(field.getVelocityVarName());
                else if (field.getFieldType().equals(Field.DataType.INTEGER))
                    value = "" + contentlet.getLongProperty(field.getVelocityVarName());
                else if (field.getFieldType().equals(Field.DataType.LONG_TEXT))
                    value = contentlet.getStringProperty(field.getVelocityVarName());
                else
                    value = contentlet.getStringProperty(field.getVelocityVarName());

                if (UtilMethods.isSet(value))
                    result.add(value);
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        }

        return result;
    }

    // jira.dotmarketing.net/browse/DOTCMS-1073
    private void deleteBinaryFiles(List<Contentlet> contentlets,Field field) {

            Iterator itr = contentlets.iterator();

            while(itr.hasNext()){
                Contentlet con = (Contentlet)itr.next();
                String inode =  con.getInode();

                // To delete binary files
                String contentletAssetPath = APILocator.getFileAPI().getRealAssetPath()
                                            + java.io.File.separator
                                            + inode.charAt(0)
                                            + java.io.File.separator
                                            + inode.charAt(1)
                                            + java.io.File.separator
                                            + inode;

                if(field != null){
                    contentletAssetPath = contentletAssetPath
                                            + java.io.File.separator
                                            + field.getVelocityVarName();
                }

                // To delete resized images
                String contentletAssetCachePath = APILocator.getFileAPI().getRealAssetPath()
                                + java.io.File.separator
                                + "cache"
                                + java.io.File.separator
                                + inode.charAt(0)
                                + java.io.File.separator
                                + inode.charAt(1)
                                + java.io.File.separator
                                + inode;

                if(field != null){
                contentletAssetCachePath = contentletAssetCachePath
                                + java.io.File.separator
                                + field.getVelocityVarName();
                }


                FileUtil.deltree(new java.io.File(contentletAssetPath));

                FileUtil.deltree(new java.io.File(contentletAssetCachePath));

            }

        }

    //http://jira.dotmarketing.net/browse/DOTCMS-2178
    public java.io.File getBinaryFile(String contentletInode, String velocityVariableName,User user) throws DotDataException,DotSecurityException {

        Logger.debug(this,"Retrieving binary file name : getBinaryFileName()." );

        Contentlet con = conFac.find(contentletInode);

        if(!perAPI.doesUserHavePermission(con,PermissionAPI.PERMISSION_READ,user))
            throw new DotSecurityException("Unauthorized Access");


        java.io.File binaryFile = null ;
        /*** THIS LOGIC IS DUPED IN THE CONTENTLET POJO.  IF YOU CHANGE HERE, CHANGE THERE **/
        try{
        java.io.File binaryFilefolder = new java.io.File(APILocator.getFileAPI().getRealAssetPath()
                + java.io.File.separator
                + contentletInode.charAt(0)
                + java.io.File.separator
                + contentletInode.charAt(1)
                + java.io.File.separator
                + contentletInode
                + java.io.File.separator
                + velocityVariableName);
                if(binaryFilefolder.exists()){
                java.io.File[] files = binaryFilefolder.listFiles(new BinaryFileFilter());

                for (java.io.File file : files) {
					String path = file.getPath();
					if(path!=null && path.indexOf("temp")==-1) {
						binaryFile = file;
						break;
					}
				}

            }
        }catch(Exception e){
            Logger.error(this,"Error occured while retrieving binary file name : getBinaryFileName(). ContentletInode : "+contentletInode+"  velocityVaribleName : "+velocityVariableName );
            throw new DotDataException("File System error.");
        }
        return binaryFile;
    }


    public long contentletCount() throws DotDataException {
        return conFac.contentletCount();
    }

    public long contentletIdentifierCount() throws DotDataException {
        return conFac.contentletIdentifierCount();
    }

    public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {
        List<Field> fields = FieldsCache.getFieldsByStructureVariableName(query.getFromClause());
        if(fields == null || fields.size() < 1){
            throw new ValidationException("No Fields found for Content");
        }
//      return conFac.DBSearch(query, fields, fields.get(0).getStructureInode());
        Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();
        for (Field field : fields) {
            dbColToObjectAttribute.put(field.getFieldContentlet(), field.getVelocityVarName());
        }

        String title = "inode";
        for (Field f : fields) {
            if(f.isListed()){
                title = f.getFieldContentlet();
                break;
            }
        }
        if(UtilMethods.isSet(query.getSelectAttributes())){

            if(!query.getSelectAttributes().contains(title)){
                query.getSelectAttributes().add(title);
            }
        }else{
            List<String> atts = new ArrayList<String>();
            atts.add("*");
            atts.add(title + " as " + QueryResult.CMIS_TITLE);
            query.setSelectAttributes(atts);
        }

        return QueryUtil.DBSearch(query, dbColToObjectAttribute, "structure_inode = '" + fields.get(0).getStructureInode() + "'", user, true,respectFrontendRoles);
    }

    private Contentlet copyContentlet(Contentlet contentlet, Host host, Folder folder, User user,boolean appendCopyToFileName, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {

    	boolean isContentletLive = false;

        if (user == null) {
            throw new DotSecurityException("A user must be specified.");
        }

        if (!perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException("You don't have permission to read the source file.");
        }

        // gets the new information for the template from the request object
        Contentlet newContentlet = new Contentlet();
        newContentlet.setStructureInode(contentlet.getStructureInode());
        copyProperties(newContentlet, contentlet.getMap(),true);
        //newContentlet.setLocked(false);
        //newContentlet.setLive(contentlet.isLive());

        if(contentlet.isLive())
        	isContentletLive = true;

        newContentlet.setInode("");
        newContentlet.setIdentifier("");
        newContentlet.setHost(host != null?host.getIdentifier(): (folder!=null? folder.getHostId() : contentlet.getHost()));
        newContentlet.setFolder(folder != null?folder.getInode(): null);
        newContentlet.setLowIndexPriority(contentlet.isLowIndexPriority());
        if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET){
        	if(appendCopyToFileName){
        		String fldNameNoExt=UtilMethods.getFileName(newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
                String fldfileExt=UtilMethods.getFileExtension(newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        		newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fldNameNoExt + "_(COPY)." + fldfileExt);
        	}
        	else
        		newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        }

        List <Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        java.io.File srcFile;
        java.io.File destFile = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + user.getUserId());
        if (!destFile.exists())
            destFile.mkdirs();

        String fieldValue;
        for (Field tempField: fields) {
            if (tempField.getFieldType().equals(Field.FieldType.BINARY.toString())) {
                fieldValue = "";
                try {
                    srcFile = getBinaryFile(contentlet.getInode(), tempField.getVelocityVarName(), user);
                    if(srcFile != null) {
                        if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET){
                            final String nameNoExt=UtilMethods.getFileName(srcFile.getName());
                            final String fileExt=UtilMethods.getFileExtension(srcFile.getName());
                        	if(appendCopyToFileName)
                        		fieldValue = nameNoExt + "_copy." + fileExt;
                        	else
                        		fieldValue = nameNoExt + "." + fileExt;
                        }else{
                            fieldValue=srcFile.getName();
                        }
                        destFile = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + user.getUserId() + java.io.File.separator + fieldValue);
                        if (!destFile.exists())
                            destFile.createNewFile();

                        FileUtils.copyFile(srcFile, destFile);
                        newContentlet.setBinary(tempField.getVelocityVarName(), destFile);
                    }
                } catch (Exception e) {
                    throw new DotDataException("Error copying binary file: '" + fieldValue + "'");
                }
            }

            if (tempField.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                if (folder != null || host != null){
                    newContentlet.setStringProperty(tempField.getVelocityVarName(), folder != null?folder.getInode():host.getIdentifier());
                }else{
                    if(contentlet.getFolder().equals(FolderAPI.SYSTEM_FOLDER)){
                        newContentlet.setStringProperty(tempField.getVelocityVarName(), contentlet.getFolder());
                    }else{
                        newContentlet.setStringProperty(tempField.getVelocityVarName(), contentlet.getHost());
                    }
                }
            }
        }

        List<Category> parentCats = catAPI.getParents(contentlet, false, user, respectFrontendRoles);
        ContentletRelationships cr = getAllRelationships(contentlet);
        List<ContentletRelationshipRecords> rr = cr.getRelationshipsRecords();
        Map<Relationship, List<Contentlet>> rels = new HashMap<Relationship, List<Contentlet>>();
        for (ContentletRelationshipRecords crr : rr) {
            rels.put(crr.getRelationship(), crr.getRecords());
        }

        newContentlet = checkin(newContentlet, rels, parentCats, perAPI.getPermissions(contentlet), user, respectFrontendRoles);

        perAPI.copyPermissions(contentlet, newContentlet);

        if(isContentletLive)
        	APILocator.getVersionableAPI().setLive(newContentlet);

        return newContentlet;
    }

    public Contentlet copyContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
        HostAPI hostAPI = APILocator.getHostAPI();
        FolderAPI folderAPI = APILocator.getFolderAPI();

        String hostIdentfier = contentlet.getHost();
        Identifier contIdentifier = APILocator.getIdentifierAPI().find(contentlet);

        Host host = hostAPI.find(hostIdentfier, user, respectFrontendRoles);
        if(host == null)
            host = new Host();
        Folder folder = folderAPI.findFolderByPath(contIdentifier.getParentPath(), host, user, false);

        return copyContentlet(contentlet, host, folder, user, needAppendCopy(contentlet,host,folder), respectFrontendRoles);
    }

    public Contentlet copyContentlet(Contentlet contentlet, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, host, null, user, needAppendCopy(contentlet,host,null), respectFrontendRoles);
    }

    public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, null, folder, user, needAppendCopy(contentlet,null,folder), respectFrontendRoles);
    }

    public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, null, folder, user, appendCopyToFileName, respectFrontendRoles);
    }

    private boolean needAppendCopy(Contentlet contentlet, Host host, Folder folder) throws DotDataException {
        if(host!=null && contentlet.getHost()!=null && !contentlet.getHost().equals(host.getIdentifier()))
            // if different host we really don't need to
            return false;

        String sourcef=null;
        if(UtilMethods.isSet(contentlet.getFolder()))
            sourcef=contentlet.getFolder();
        else
            sourcef=APILocator.getFolderAPI().findSystemFolder().getInode();

        String destf=null;
        if(UtilMethods.isSet(folder))
            destf=folder.getInode();
        else
            destf=APILocator.getFolderAPI().findSystemFolder().getInode();

        return sourcef.equals(destf);
    }

    private boolean hasAHostField(String structureInode) {
        List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
        for(Field f : fields) {
            if(f.getFieldType().equals("host or folder"))
                return true;
        }
        return false;
    }

    private boolean hasAHostFieldSet(String structureInode, Contentlet contentlet) {
        List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
        for(Field f : fields) {
            if(f.getFieldType().equals("host or folder") && UtilMethods.isSet(getFieldValue(contentlet, f))){
                return true;
            }
        }
        return false;
    }

    public boolean isInodeIndexed(String inode) {
        return isInodeIndexed(inode,false);
    }

    public boolean isInodeIndexed(String inode,boolean live) {
        if(!UtilMethods.isSet(inode)){
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }
        SearchHits lc;
        boolean found = false;
        int counter = 0;
        while(counter < 300){
            try {
                lc = conFac.indexSearch("+inode:" + inode+(live?" +live:true":""), 0, 0, "modDate");
            } catch (Exception e) {
                Logger.error(this.getClass(),e.getMessage(),e);
                return false;
            }
            if(lc.getTotalHits() > 0){
                found = true;
                return true;
            }
            try{
                Thread.sleep(100);
            }catch (Exception e) {
                Logger.debug(this, "Cannot sleep : ", e);
            }
            counter++;
        }
        return found;
    }

    public boolean isInodeIndexed(String inode, int secondsToWait) {
        SearchHits lc;
        boolean found = false;
        int counter = 0;
        while(counter <= (secondsToWait / 10)) {
            try {
                lc = conFac.indexSearch("+inode:" + inode, 0, 0, "modDate");
            } catch (Exception e) {
                Logger.error(this.getClass(),e.getMessage(),e);
                return false;
            }
            if(lc.getTotalHits() > 0){
                found = true;
                return true;
            }
            try{
                Thread.sleep(100);
            }catch (Exception e) {
                Logger.debug(this, "Cannot sleep : ", e);
            }
            counter++;
        }
        return found;
    }

    public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException, DotSecurityException {
        conFac.UpdateContentWithSystemHost(hostIdentifier);
    }

    public void removeUserReferences(String userId)throws DotDataException, DotSecurityException {
        conFac.removeUserReferences(userId);
    }

    public String getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {


    	// no structure, no inode, no workee
        if (!InodeUtils.isSet(contentlet.getInode()) || !InodeUtils.isSet(contentlet.getStructureInode())) {
        	return null;
        }

    	final String CONTENTLET_URL_MAP_FOR_CONTENT = "URL_MAP_FOR_CONTENT";
    	final String CONTENTLET_URL_MAP_FOR_CONTENT_404 = "URL_MAP_FOR_CONTENT_404";
    	String result = (String) contentlet.getMap().get(CONTENTLET_URL_MAP_FOR_CONTENT);
    	if(result != null){
        	if(CONTENTLET_URL_MAP_FOR_CONTENT_404.equals(result) ){
        		return null;
        	}
    		return result;
    	}




        // if there is no detail page, return
        Structure structure = StructureCache.getStructureByInode(contentlet.getStructureInode());
        if(!UtilMethods.isSet(structure.getDetailPage())) {
        	return null;
        }




        Identifier id = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);

        // File assets send their path
        // if(contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET){
        // 	result = id.getPath();
        // }

        // URL MAPPed
       if (UtilMethods.isSet(structure.getUrlMapPattern())) {
            List<RegExMatch> matches = RegEX.find(structure.getUrlMapPattern(), "({[^{}]+})");
            String urlMapField;
            String urlMapFieldValue;
            result = structure.getUrlMapPattern();
            for (RegExMatch match: matches) {
                urlMapField = match.getMatch();
                urlMapFieldValue = contentlet.getStringProperty(urlMapField.substring(1, (urlMapField.length() - 1)));
                urlMapField = urlMapField.replaceFirst("\\{", "\\\\{");
                urlMapField = urlMapField.replaceFirst("\\}", "\\\\}");
                if (UtilMethods.isSet(urlMapFieldValue)){
                	result = result.replaceAll(urlMapField, urlMapFieldValue);
                }
                else{
                	result = result.replaceAll(urlMapField, "");
                }
            }
        }

        // or Detail page with id=uuid
        else{
            HTMLPage p = APILocator.getHTMLPageAPI().loadLivePageById(structure.getDetailPage(), user, respectFrontendRoles);
        	if(p != null && UtilMethods.isSet(p.getIdentifier())){
        		result = p.getURI() + "?id=" + contentlet.getInode();
        	}
        }

        // we send the host of the content, not the detail page (is this right?)
        if ((host != null) && !host.isSystemHost() && ! respectFrontendRoles && result !=null) {
        	if(result.indexOf("?") <0){
        		result = result + "?host_id=" + host.getIdentifier();
        	}
        	else{
        		result = result + "&host_id=" + host.getIdentifier();
        	}
        }





    	if(result == null){
    		result = CONTENTLET_URL_MAP_FOR_CONTENT_404;
    	}
        contentlet.setStringProperty(CONTENTLET_URL_MAP_FOR_CONTENT, result);



        return result;
    }

    public Contentlet saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
        if(contentlet.getInode().equals(""))
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        canLock(contentlet, user);
        //get the latest and greatest from db
        Contentlet working = conFac.findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId());

        /*
         * Only draft if there is a working version that is not live
         * and always create a new version if the user is different
         */
        if(! working.isLive() && working.getModUser().equals(contentlet.getModUser())){

            // if we are the latest and greatest and are a draft
            if(working.getInode().equals(contentlet.getInode()) ){

                return checkinWithoutVersioning(contentlet, contentRelationships,
                        cats,
                        permissions, user, false);

            }
            else{
                String workingInode = working.getInode();
                copyProperties(working, contentlet.getMap());
                working.setInode(workingInode);
                working.setModUser(user.getUserId());
                return checkinWithoutVersioning(working, contentRelationships,
                        cats,
                        permissions, user, false);
            }
        }

        contentlet.setInode(null);
        return checkin(contentlet, contentRelationships,
                cats,
                permissions, user, false);
    }


    public void removeFolderReferences(Folder folder)throws DotDataException, DotSecurityException {
        conFac.removeFolderReferences(folder);
    }


    /**
     * Tests whether a user can potentially lock a piece of content (needed to test before publish, etc).  This method will return false if content is already locked
     * by another user.
     * @param contentlet
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public boolean canLock(Contentlet contentlet, User user) throws   DotLockException {
        if(contentlet ==null || !UtilMethods.isSet(contentlet.getIdentifier())){
            return true;
        }
        if(user ==null){
            throw new DotLockException("null User cannot lock content");
        }

        try{
            if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
                return true;
            }
            else if(!APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, false)){
                throw new DotLockException("User does not have Edit Permissions to lock content");
            }
        }catch(DotDataException dde){
            throw new DotLockException("User does not have Edit Permissions to lock content");
        }


        String lockedBy =null;
        try{
            lockedBy=APILocator.getVersionableAPI().getLockedBy(contentlet);
        }
        catch(Exception e){

        }
        if(lockedBy != null && !user.getUserId().equals(lockedBy)){
            throw new DotLockException(CANT_GET_LOCK_ON_CONTENT);
        }
        return true;

    }

	public Map<Relationship, List<Contentlet>> findContentRelationships(
			Contentlet contentlet, User user) throws DotDataException,
			DotSecurityException {

		if(!APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user)){
            throw new DotLockException("User does not have Edit Permissions on the content");
        }

		return findContentRelationships(contentlet);
	}

    @Override
    public long indexCount(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        boolean isAdmin = false;
        List<Role> roles = new ArrayList<Role>();
        if(user == null && !respectFrontendRoles){
            throw new DotSecurityException("You must specify a user if you are not respecting frontend roles");
        }
        if(user != null){
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            }else{
                isAdmin = true;
            }
        }
        StringBuffer buffy = new StringBuffer(luceneQuery);

        // Permissions in the query
        if (!isAdmin)
            addPermissionsToQuery(buffy, user, roles, respectFrontendRoles);

        return conFac.indexCount(buffy.toString());
    }

	@Override
	public List<Map<String, String>> getMostViewedContent(String structureVariableName, String startDateStr, String endDateStr, User user) {

		String[] dateFormats = new String[] { "yyyy-MM-dd HH:mm", "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm",
                "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy",
                "hh:mm:ss aa", "HH:mm:ss", "yyyy-MM-dd"};

		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		String structureInode = StructureCache.getStructureByVelocityVarName(structureVariableName).getInode();
		if(!UtilMethods.isSet(structureInode))
			return result;

		GregorianCalendar gCal = new GregorianCalendar();
		Date endDate = gCal.getTime();
		gCal.add(2, -3);
		Date startDate = gCal.getTime();// Default interval

		if(!UtilMethods.isSet(startDateStr) && !UtilMethods.isSet(endDateStr)){
			GregorianCalendar gc = new GregorianCalendar();
			endDate = gc.getTime();
			gc.add(2, -3);
			startDate = gc.getTime();
		}else if(!UtilMethods.isSet(startDateStr)){
			try {
				endDate = DateUtil.convertDate(endDateStr, dateFormats);
				Calendar gc = new GregorianCalendar();
				gc.setTime(endDate);
				gc.add(2, -3);
				startDate = gc.getTime();
			} catch (java.text.ParseException e) {
				GregorianCalendar gc = new GregorianCalendar();
				endDate = gc.getTime();
				gc.add(2, -3);
				startDate = gc.getTime();
			}
		}else if(!UtilMethods.isSet(endDateStr)){
			try {
				startDate = DateUtil.convertDate(endDateStr, dateFormats);
				Calendar gc = new GregorianCalendar();
				gc.setTime(startDate);
				gc.add(2, +3);
				endDate = gc.getTime();
			} catch (java.text.ParseException e) {
				GregorianCalendar gc = new GregorianCalendar();
				endDate = gc.getTime();
				gc.add(2, -3);
				startDate = gc.getTime();
			}
		}else{
			try {
				startDate = DateUtil.convertDate(startDateStr, dateFormats);
				endDate = DateUtil.convertDate(endDateStr, dateFormats);
			} catch (java.text.ParseException e) {}
		}

		try {
			result = conFac.getMostViewedContent(structureInode, startDate, endDate , user);
		} catch (Exception e) {}
		return result;
	}

	/**
	 * This method is called when I'm publishing a contentlet and one of its fields is an IMAGE or a FILE.
	 *
	 * Unlike the current version, before find the asset with the default language I try to do this by using the languageId of the
	 * current contentlet.
	 *
	 * In this way I can upload an asset into a language different from the default one, publish it and create another contentlet,
	 * into the same language, and link them.
	 *
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * Jun 20, 2013 - 2:32:05 PM
	 */
	private Contentlet findBinaryAssociatedContent(Identifier id, long languageId) throws DotContentletStateException, DotSecurityException, DotDataException{
		Contentlet fileAssetCont = null;
    	try {
    		fileAssetCont = findContentletByIdentifier(id.getId(), true, languageId, APILocator.getUserAPI().getSystemUser(), false);
        } catch(DotContentletStateException se) {
        	try{
        		fileAssetCont = findContentletByIdentifier(id.getId(), false, languageId, APILocator.getUserAPI().getSystemUser(), false);
        	}catch(DotContentletStateException se1) {
        		/**
        		 * Finally, if I didn't found the contentlet I do the "findContentletByIdentifier" with the default language,
        		 * like the current class version.
        		 */
        		fileAssetCont = findContentletByIdentifier(id.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
        	}
        }
    	return fileAssetCont;

	}
}