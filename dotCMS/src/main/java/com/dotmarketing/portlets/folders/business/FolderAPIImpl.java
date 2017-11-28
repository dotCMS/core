package com.dotmarketing.portlets.folders.business;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.dotcms.api.system.event.*;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.cmis.QueryResult;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotmarketing.business.APILocator.getPermissionAPI;

public class FolderAPIImpl implements FolderAPI  {
    
    public static final String ROOT_PATH = "/";
    public static final String SYSTEM_FOLDER_PATH = "/System folder";
    public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";
    private final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

    /**
     * Will get a folder for you on a given path for a particular host
     *
     * @param path
     * @param hostId
     * @return
     * @throws DotHibernateException
     */
    private final FolderFactory folderFactory = FactoryLocator.getFolderFactory();
    private final PermissionAPI permissionAPI = getPermissionAPI();

    @CloseDBIfOpened
    public Folder findFolderByPath(final String path, final Host host,
                                   final User user, final boolean respectFrontEndPermissions) throws DotStateException,
            DotDataException, DotSecurityException {

        final Folder folder = folderFactory.findFolderByPath(path, host);

        if (folder != null && InodeUtils.isSet(folder.getInode()) &&
                !doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

            // SYSTEM_FOLDER means if the user has permissions to the host, then they can see host.com/
            if(FolderAPI.SYSTEM_FOLDER.equals(folder.getInode()) && 
                    !Host.SYSTEM_HOST.equals(host.getIdentifier()) && 
                    !permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
                    throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read folder " + folder.getPath());
            }
        }

        return folder;
    }


    public Folder findFolderByPath(String path, String hostid, User user, boolean respectFrontEndPermissions) throws DotStateException,
            DotDataException, DotSecurityException {
        if(user==null){
            user = APILocator.systemUser();
        }
        Host host = APILocator.getHostAPI().find(hostid, user, false);
        return findFolderByPath(path,host,user, respectFrontEndPermissions);
    }

    @WrapInTransaction
    public boolean renameFolder(final Folder folder, final String newName,
                                final User user, final boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException {

        boolean renamed = false;

        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to edit folder" + folder.getPath());
        }

        try {

            renamed = folderFactory.renameFolder(folder, newName, user, respectFrontEndPermissions);

            return renamed;
        } catch (Exception e) {

            throw new DotDataException(e.getMessage(),e);
        }
    }

    @CloseDBIfOpened
    public Folder findParentFolder(final Treeable asset, final User user, final boolean respectFrontEndPermissions) 
            throws DotIdentifierStateException, DotDataException, DotSecurityException {
        Identifier id = APILocator.getIdentifierAPI().find(asset.getIdentifier());
        
        if(id==null || id.getParentPath()==null || ROOT_PATH.equals(id.getParentPath()) || SYSTEM_FOLDER_PATH.equals(id.getParentPath())){
            return null;
        }
        
        Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontEndPermissions);
        final Folder folder = folderFactory.findFolderByPath(id.getParentPath(), host);

        if(folder == null || !UtilMethods.isSet(folder.getInode())){
            return null;
        }
        
        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            final String userId = user.getUserId() != null ? user.getUserId(): "";
            final String path = folder.getPath() != null ? folder.getPath() : id.getParentPath();
            Logger.error(this, "User " + userId  + " does not have permission to read Folder " + path + " Please check the folder exists.");
            throw new DotSecurityException("User " + userId + " does not have permission to read Folder " + path);
        }
        
        return folder;

    }

    /**
     *
     * @param folder
     * @return List of sub folders for passed in folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @CloseDBIfOpened
    public List<Folder> findSubFolders(final Folder folder, final User user,
                                       final boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException {

        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {

            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
        }

        return this.folderFactory.getFoldersByParent(folder, user, respectFrontEndPermissions);
    } // findSubFolders.

    /**
     *
     * @param folder
     * @return List of sub folders for passed in folder
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public List<Folder> findSubFolders(final Host host, final User user,
                                       final boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
        }

        final List<Folder> allFolders = folderFactory.findFoldersByHost(host);
        
        return allFolders.stream().filter ((Folder folder) ->  
            doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)
                ).collect(CollectionsUtils.toImmutableList()); 
        
    }
    
    @CloseDBIfOpened
    public List<Folder> findThemes(final Host host, final User user,
                                   final boolean respectFrontEndPermissions) throws DotDataException,
    DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
        }

        final List<Folder> themesByHost = folderFactory.findThemesByHost(host);
        
        return themesByHost.stream().filter ((Folder folder) ->  
            doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)
                ).collect(CollectionsUtils.toImmutableList()); 
    }

    /**
     *
     * @param folder
     *            Recursively
     * @return List of sub folders for passed in folder
     * @throws DotSecurityException
     * @throws DotDataException
     */

    public List<Folder> findSubFoldersRecursively(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException {

        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
        }

        List<Folder> subFolders = findSubFolders(folder, user, respectFrontEndPermissions);
        List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
        for (final Folder subFolder : toIterateOver) {
            if (doesUserHavePermissions(subFolder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
                subFolders.addAll(findSubFoldersRecursively(subFolder, user, respectFrontEndPermissions));
            }
        }
        return subFolders;
    }

    /**
     *
     * @param folder
     * @return List of sub folders for passed in folder
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException {
        if (!this.permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname());
        }
        List<Folder> subFolders = folderFactory.findFoldersByHost(host);
        List<Folder> toIterateOver = new ArrayList<Folder>(subFolders);
        for (Folder subFolder : toIterateOver) {
            if (doesUserHavePermissions(subFolder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
                subFolders.addAll(findSubFoldersRecursively(subFolder, user, respectFrontEndPermissions));
            }
        }
        return subFolders;
    }

    /**
     * Will copy a folder to a new location with all it contains.
     *
     * @param folderToCopy
     * @param newParentFolder
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotStateException
     */
    @CloseDBIfOpened
    public void copy(final Folder folderToCopy, final Folder newParentFolder,
                     final User user, final boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException, DotStateException, IOException {

        if (!doesUserHavePermissions(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToCopy.getPath());
        }

        if (!doesUserHavePermissions(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentFolder.getPath());
        }

        folderFactory.copy(folderToCopy, newParentFolder);

        this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy, Visibility.EXCLUDE_OWNER,
                new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
    }

    @CloseDBIfOpened
    public void copy(final Folder folderToCopy, final Host newParentHost,
                     final User user, final boolean respectFrontEndPermissions) throws DotDataException,
            DotSecurityException, DotStateException, IOException {

        if (!doesUserHavePermissions(folderToCopy, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToCopy.getPath());
        }

        if (!this.permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Host " + newParentHost.getHostname());
        }

        folderFactory.copy(folderToCopy, newParentHost);

        this.systemEventsAPI.pushAsync(SystemEventType.COPY_FOLDER, new Payload(folderToCopy, Visibility.EXCLUDE_OWNER,
                new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
    }

    @CloseDBIfOpened
    public boolean exists(String folderInode) throws DotDataException {
        return folderFactory.exists(folderInode);
    }


    @SuppressWarnings("unchecked")
    @CloseDBIfOpened
    public List<Inode> findMenuItems(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException, DotDataException {
        return folderFactory.getMenuItems(folder);
    }

    /**
     * Takes a folder and a user and deletes all underneith assets User needs
     * edit permssions on folder to delete everything undernieth
     *
     * @param folder
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public void delete(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

        String path = StringUtils.EMPTY;

        if(folder==null || !UtilMethods.isSet(folder.getInode()) ){
            Logger.debug(getClass(), "Cannot delete null folder");
            return;
        } else {
            AdminLogger.log(this.getClass(), "delete", "Deleting folder with name " + (UtilMethods.isSet(folder.getName()) ? folder.getName() + " ": "name not set "), user);
        }
        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndPermissions)) {

            Logger.debug(getClass(), "The user: " + user.getEmailAddress() + " does not have permission for: " + folder );
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to edit Folder " + folder.getPath());
        }


        if(folder != null && FolderAPI.SYSTEM_FOLDER.equals(folder.getInode())) {

            Logger.debug(getClass(), "Cannot delete null folder: " + folder.getInode());
            throw new DotSecurityException("YOU CANNOT DELETE THE SYSTEM FOLDER");
        }


        // start transactional delete
        try {

            PermissionAPI papi = getPermissionAPI();
            if (!papi.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {
                final String userId = user.getUserId() != null?user.getUserId():"";

                Logger.error(this.getClass(), "User " +  userId + " does not have permission to Folder " + folder.getPath());
                throw new DotSecurityException("User " + userId +  "does not have edit permissions on Folder " + folder.getPath());
            }

            Folder faker = new Folder();
            faker.setShowOnMenu(folder.isShowOnMenu());
            faker.setInode(folder.getInode());
            faker.setIdentifier(folder.getIdentifier());
            faker.setHostId(folder.getHostId());
            Identifier ident = APILocator.getIdentifierAPI().find(faker.getIdentifier());
            
            List<Folder> folderChildren = findSubFolders(folder, user, respectFrontEndPermissions);

            // recursivily delete
            for (Folder childFolder : folderChildren) {
                // sub deletes use system user - if a user has rights to parent
                // permission (checked above) they can delete to children
                if (Logger.isDebugEnabled(getClass())) {
                    Logger.debug(getClass(), "Deleting the folder " + childFolder.getPath());
                }
                delete(childFolder, user, respectFrontEndPermissions);
            }

            // delete assets in this folder
            path = folder.getPath();
            if (Logger.isDebugEnabled(getClass())) {
                Logger.debug(getClass(), "Deleting the folder assets " + path);
            }
            _deleteChildrenAssetsFromFolder(folder, user, respectFrontEndPermissions);

            // get roles for the event.
            final Set<Role> roles =  papi.getRolesWithPermission
                    (folder, PermissionAPI.PERMISSION_READ);
            if (Logger.isDebugEnabled(getClass())) {
                Logger.debug(getClass(), "Getting the folder roles for " + path +
                        " roles: " + roles.stream().map(role -> role.getName()).collect(Collectors.toList()));
                Logger.debug(getClass(), "Removing the folder permissions for " + path);
            }

            papi.removePermissions(folder);

            //http://jira.dotmarketing.net/browse/DOTCMS-6362
            if (Logger.isDebugEnabled(getClass())) {
                Logger.debug(getClass(), "Removing the folder references for " + path);
            }
            APILocator.getContentletAPIImpl().removeFolderReferences(folder);

            // delete folder itself
            if (Logger.isDebugEnabled(getClass())) {
                Logger.debug(getClass(), "Removing the folder itself: " + path);
            }
            folderFactory.delete(folder);

            // The delete folder will avoid to send the message to the current user
            // in addition will check any match roles to propagate the event.
            if (Logger.isDebugEnabled(getClass())) {
                
                Logger.debug(getClass(), "Pushing async events: " + path);
            }

            this.systemEventsAPI.pushAsync(SystemEventType.DELETE_FOLDER, new Payload(folder, Visibility.EXCLUDE_OWNER,
                    new ExcludeOwnerVerifierBean(user.getUserId(),
                            new VisibilityRoles(VisibilityRoles.Operator.OR, roles), Visibility.ROLES)));

            // delete the menus using the fake proxy inode
            if (folder.isShowOnMenu()) {
                // RefreshMenus.deleteMenus();
                RefreshMenus.deleteMenu(faker);
                CacheLocator.getNavToolCache().removeNav(faker.getHostId(), faker.getInode());
                
                CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
                //remove value in the parent folder from the children listing
                final Folder parentFolder = !ROOT_PATH.equals(ident.getParentPath()) ? APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), faker.getHostId(), user, false) : APILocator.getFolderAPI().findSystemFolder();
                if(parentFolder != null){
                    CacheLocator.getNavToolCache().removeNav(faker.getHostId(), parentFolder.getInode());
                }
            }

            PublisherAPI.getInstance().deleteElementFromPublishQueueTable(folder.getInode());
        } catch (Exception e) {

            throw new DotDataException(e.getMessage(),e);
        }
    }

    private void _deleteChildrenAssetsFromFolder(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException,
            DotStateException, DotSecurityException {

        try {

            final ContentletAPI capi = APILocator.getContentletAPI();

            /************ conList *****************/
            HibernateUtil.getSession().clear();
            final List<Contentlet> conList = capi.findContentletsByFolder(folder, user, false);
            for (final Contentlet contentlet : conList) {
                // Find all multi-language contentlets and archive them
                final Identifier ident = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
                final List<Contentlet> otherLanguageCons = capi.findAllVersions(ident, user, false);
                for (final Contentlet cv : otherLanguageCons) {
                    if(cv.isLive()){
                        capi.unpublish(cv, user, false);
                    }
                    if(!cv.isArchived()){
                        capi.archive(cv, user, false);
                    }
                }
                capi.delete(contentlet, user, false);
            }

            /************ Links *****************/
            HibernateUtil.getSession().clear();
            final List<Link> links = getLinks(folder, user, respectFrontEndPermissions);
            for (final Link linker : links) {
                final Link link = (Link) linker;
                final Identifier identifier = APILocator.getIdentifierAPI().find(link);
                
                if (!InodeUtils.isSet(identifier.getInode())) {
                    Logger.warn(FolderFactory.class, "_deleteChildrenAssetsFromFolder: link inode = " + link.getInode()
                    + " doesn't have a valid identifier associated.");
                    continue;
                }
                permissionAPI.removePermissions(link);
                APILocator.getMenuLinkAPI().delete(link, user, false);
            }

            /******** delete possible orphaned identifiers under the folder *********/
            HibernateUtil.getSession().clear();
            final Identifier ident = APILocator.getIdentifierAPI().find(folder);
            final List<Identifier> orphanList = APILocator.getIdentifierAPI().findByParentPath(folder.getHostId(), ident.getURI());
            for(final Identifier orphan : orphanList) {
                APILocator.getIdentifierAPI().delete(orphan);
                HibernateUtil.getSession().clear();
                try {
                    final DotConnect dc = new DotConnect();
                    dc.setSQL("delete from identifier where id=?");
                    dc.addParam(orphan.getId());
                    dc.loadResult();
                } catch(Exception ex) {
                    Logger.warn(this, "can't delete the orphan identifier",ex);
                }
                HibernateUtil.getSession().clear();
            }

            /************ Structures *****************/
            
        } catch (DotSecurityException e) {
            Logger.error(this, "A DotSecurityException error has been detected: " +e.getMessage(), e);
            throw new DotSecurityException(e.getMessage());
        } catch (DotDataException e2) {
            Logger.error(this, "A DotDataException error has been detected: " + e2.getMessage(), e2);
            throw new DotDataException(e2.getMessage());
        }

    }

    /**
     * @param id
     *            the inode or id of the folder
     * @return Folder with a given id or inode
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public Folder find(final String id, final User user,
                       final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

        final Folder folder = this.folderFactory.find(id);

        if (!doesUserHavePermissions(folder, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folder.getPath());
        }

        return folder;
    } // find.


    /**
     * Saves a folder
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotStateException
     */
    @WrapInTransaction
    public void save(final Folder folder, final String existingId,
                     final User user, final boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

        final Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
        if(id ==null || !UtilMethods.isSet(id.getId())){
            throw new DotStateException("Folder must already have an identifier before saving");
        }

        final Host host = APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions);
        final Folder parentFolder = findFolderByPath(id.getParentPath(), id.getHostId(), user, respectFrontEndPermissions);
        
        final Permissionable parent = ROOT_PATH.equals(id.getParentPath()) || SYSTEM_FOLDER_PATH.equals(id.getParentPath())
                ? host : parentFolder;

        if(parent ==null){
            throw new DotStateException("No Folder Found for id: " + id.getParentPath());
        }
        if (!this.permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,respectFrontEndPermissions)
                || !this.permissionAPI.doesUserHavePermissions(PermissionableType.FOLDERS, PermissionAPI.PERMISSION_EDIT, user)) {
            final String userId = user.getUserId() != null ? user.getUserId() : "";
            final String parentFolderAsString =ROOT_PATH.equals(id.getParentPath()) || SYSTEM_FOLDER_PATH.equals(id.getParentPath())
                    ? host.getName(): parentFolder.getPath();
            throw new AddContentToFolderPermissionException(userId, parentFolderAsString);
        }

        final boolean isNew = folder.getInode() == null;
        folder.setModDate(new Date());
        folder.setName(folder.getName().toLowerCase());
        folderFactory.save(folder, existingId);

        final SystemEventType systemEventType = isNew ? SystemEventType.SAVE_FOLDER : SystemEventType.UPDATE_FOLDER;
        systemEventsAPI.pushAsync(systemEventType, new Payload(folder, Visibility.EXCLUDE_OWNER,
                new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
    }

    public void save(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotStateException, DotSecurityException {

        save( folder, null,  user,  respectFrontEndPermissions);

    }

    @CloseDBIfOpened
    public Folder findSystemFolder() throws DotDataException {
        return folderFactory.findSystemFolder();
    }


    @WrapInTransaction
    public Folder createFolders(String path, Host host, User user, boolean respectFrontEndPermissions) throws DotHibernateException,
            DotSecurityException, DotDataException {

        StringTokenizer st = new StringTokenizer(path, ROOT_PATH);
        StringBuffer sb = new StringBuffer(ROOT_PATH);

        Folder parent = null;

        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            sb.append(name + ROOT_PATH);
            Folder folder = findFolderByPath(sb.toString(), host, user, respectFrontEndPermissions);
            if (folder == null || !InodeUtils.isSet(folder.getInode())) {
                folder = new Folder();
                folder.setName(name);
                folder.setTitle(name);
                folder.setShowOnMenu(false);
                folder.setSortOrder(0);
                folder.setFilesMasks("");
                folder.setHostId(host.getIdentifier());
                folder.setDefaultFileType(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(APILocator.getFileAssetAPI().DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
                Identifier newIdentifier = new Identifier();
                if(!UtilMethods.isSet(parent)){
                    newIdentifier = APILocator.getIdentifierAPI().createNew(folder, host);
                }else{
                    newIdentifier = APILocator.getIdentifierAPI().createNew(folder, parent);
                }

                folder.setIdentifier(newIdentifier.getId());
                save(folder,  user,  respectFrontEndPermissions);
            }
            parent = folder;
        }
        return parent;
    }

    @CloseDBIfOpened
    public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
            DotDataException {
        Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

        if (UtilMethods.isSet(query.getSelectAttributes())) {

            if (!query.getSelectAttributes().contains("title")) {
                query.getSelectAttributes().add("title" + " as " + QueryResult.CMIS_TITLE);
            }
        } else {
            List<String> atts = new ArrayList<String>();
            atts.add("*");
            atts.add("title" + " as " + QueryResult.CMIS_TITLE);
            query.setSelectAttributes(atts);
        }

        return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, false, respectFrontendRoles);
    }

    /**
     * @deprecated Not implemented because it does not take a user
     */


    @CloseDBIfOpened
    public List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontendRoles) throws DotHibernateException {
        return folderFactory.findFoldersByHost(host);
    }

    @CloseDBIfOpened
    public  List<Link> getLinks(final Folder parent, final boolean working,
                                final boolean deleted, final User user,
                                final boolean respectFrontEndPermissions)
                                throws DotStateException, DotDataException, DotSecurityException {

        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }

        final ChildrenCondition cond = new ChildrenCondition();
        cond.working=working;
        cond.deleted=deleted;
        List list = folderFactory.getChildrenClass(parent, Link.class,cond);
        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public List<Link> getLinks ( Host host, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !this.permissionAPI.doesUserHavePermission( host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname() );
        }

        ChildrenCondition cond = new ChildrenCondition();
        cond.working = working;
        cond.deleted = deleted;
        List list = folderFactory.getChildrenClass( host, Link.class, cond );
        return this.permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

    @CloseDBIfOpened
    public List<Link> getLinks ( Host host, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !this.permissionAPI.doesUserHavePermission( host, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Host " + host.getHostname() );
        }

        List list = folderFactory.getChildrenClass( host, Link.class );
        return this.permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

    @CloseDBIfOpened
    public List<Link> getLinks ( Folder parent, User user, boolean respectFrontEndPermissions ) throws DotStateException, DotDataException, DotSecurityException {

        if ( !doesUserHavePermissions( parent, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions ) ) {
            throw new DotSecurityException( "User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath() );
        }

        List list = folderFactory.getChildrenClass( parent, Link.class );
        return this.permissionAPI.filterCollection( list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user );
    }

    @CloseDBIfOpened
    public  List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
        DotDataException, DotSecurityException{
        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }

        List list = folderFactory.getChildrenClass(parent, Contentlet.class);
        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public  List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
        DotDataException, DotSecurityException{
        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }
        List list = StructureFactory.getStructures("folder='"+parent.getInode()+"'", "mod_date", Integer.MAX_VALUE, 0, "asc");
        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public boolean isChildFolder(Folder folder1, Folder folder2) throws DotDataException,DotSecurityException {
       return folderFactory.isChildFolder(folder1, folder2);
    }

    @CloseDBIfOpened
    public boolean matchFilter(Folder folder, String fileName) {
        return folderFactory.matchFilter(folder, fileName);
    }

    @CloseDBIfOpened
    public List findMenuItems(Folder folder, int orderDirection) throws DotDataException{
        return folderFactory.getMenuItems(folder, orderDirection);
    }

    @CloseDBIfOpened
    public List<Inode> findMenuItems(Host host,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        return folderFactory.getMenuItems(host);
    }

    @CloseDBIfOpened
    public List<Folder> findSubFoldersTitleSort(Folder folder,User user,boolean respectFrontEndPermissions)throws DotDataException {
        return folderFactory.getSubFoldersTitleSort(folder);
    }

    @WrapInTransaction
    public boolean move(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
        if (!doesUserHavePermissions(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToMove.getPath());
        }

        if (!doesUserHavePermissions(newParentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentFolder.getName());
        }
        boolean move = folderFactory.move(folderToMove, newParentFolder);

        this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove, Visibility.EXCLUDE_OWNER,
                new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

        return move;
    }

    @WrapInTransaction
    public boolean move(Folder folderToMove, Host newParentHost,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException {
        if (!doesUserHavePermissions(folderToMove, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + folderToMove.getPath());
        }

        if (!this.permissionAPI.doesUserHavePermission(newParentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to add to Folder " + newParentHost.getHostname());
        }
        boolean move = folderFactory.move(folderToMove, newParentHost);

        this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FOLDER, new Payload(folderToMove, Visibility.EXCLUDE_OWNER,
                new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

        return move;
    }

    @CloseDBIfOpened
    public List<Folder> findSubFolders(Host host, boolean showOnMenu) throws DotHibernateException{
        return folderFactory.findSubFolders(host, showOnMenu);
    }

    @CloseDBIfOpened
    public List<Folder> findSubFolders(Folder folder,boolean showOnMenu)throws DotStateException, DotDataException {
        return folderFactory.findSubFolders(folder, showOnMenu);
    }

    @CloseDBIfOpened
    public List<String> getEntriesTree(Folder mainFolder, String openNodes,String view, String content, String structureInode,User user)
            throws DotStateException, DotDataException, DotSecurityException {
        Locale locale = user.getLocale();
        TimeZone timeZone = user.getTimeZone();
        Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
        boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
        return folderFactory.getEntriesTree(mainFolder, openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
    }


    @CloseDBIfOpened
    public List<String> getFolderTree(String openNodes, String view,
            String content, String structureInode,User user)
            throws DotStateException, DotDataException, DotSecurityException {
        Locale locale = user.getLocale();
        TimeZone timeZone = user.getTimeZone();
        Role[] roles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
        boolean isAdminUser = APILocator.getUserAPI().isCMSAdmin(user);
        return folderFactory.getFolderTree(openNodes, view, content, structureInode, locale, timeZone, roles, isAdminUser, user);
    }

    @CloseDBIfOpened
    public List<Contentlet> getLiveContent(Folder parent, User user,boolean respectFrontEndPermissions) 
            throws DotDataException, DotSecurityException {
        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getName());
        }
        ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
        List list = folderFactory.getChildrenClass(parent, Contentlet.class, cond);

        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public List<Link> getLiveLinks(final Folder parent, final User user,
                                   final boolean respectFrontEndPermissions)
                                   throws DotDataException, DotSecurityException {

        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }

        final ChildrenCondition cond = new ChildrenCondition();
        cond.live=true;
        cond.deleted=false;
        final List list = folderFactory.getChildrenClass(parent, Link.class, cond);

        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public List<Contentlet> getWorkingContent(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }
        ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        cond.deleted=false;
        List list = folderFactory.getChildrenClass(parent, Contentlet.class, cond);

        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }

    @CloseDBIfOpened
    public List<Link> getWorkingLinks(Folder parent, User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        if (!doesUserHavePermissions(parent, PermissionAPI.PERMISSION_READ, user,respectFrontEndPermissions)) {
            throw new DotSecurityException("User " + user.getUserId() != null?user.getUserId():"" + " does not have permission to read Folder " + parent.getPath());
        }
        ChildrenCondition cond = new ChildrenCondition();
        cond.working=true;
        //cond.deleted=false;
        List list = folderFactory.getChildrenClass(parent, Link.class, cond);

        return this.permissionAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
    }
    
    private boolean doesUserHavePermissions(Folder folder, int permissionLevel, User user, boolean respectFrontEndPermissions){
        try {
            return this.permissionAPI.doesUserHavePermission(folder, permissionLevel, user, respectFrontEndPermissions);
        } catch (DotDataException e) {
            final String userId = user.getUserId() != null ? user.getUserId(): "";
            Logger.error(this, "User " + userId  + " does not have permission to read Folder " + folder.getPath());
        }
        return false;
    }
}