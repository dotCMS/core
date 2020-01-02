package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author jtesser
 * @author david torres
 *
 */
public class HostAPIImpl implements HostAPI {

    private ContentletFactory contentletFactory = FactoryLocator.getContentletFactory();
    private HostCache hostCache = CacheLocator.getHostCache();
    private Host systemHost;
    public final static String FOUR_OH_FOUR_HOSTNAME="__fourOhFourHost__";
    protected final static Host FOUR_OH_FOUR_HOST = new Host(new Contentlet(ImmutableMap.of(Host.HOST_NAME_KEY, FOUR_OH_FOUR_HOSTNAME,Contentlet.IDENTIFIER_KEY, FOUR_OH_FOUR_HOSTNAME)));
    
    private final SystemEventsAPI systemEventsAPI;
    private static final String CONTENT_TYPE_CONDITION = "+contentType";
    private final DotConcurrentFactory concurrentFactory = DotConcurrentFactory.getInstance();

    public HostAPIImpl() {
        this.systemEventsAPI = APILocator.getSystemEventsAPI();
    }

    private ContentType hostType() throws DotDataException, DotSecurityException{
        return APILocator.getContentTypeAPI(APILocator.systemUser()).find(Host.HOST_VELOCITY_VAR_NAME);
    }

    /**
     *
     * @return the default host from cache.  If not found, returns from content search and adds to cache
     * @throws DotSecurityException, DotDataException
     */
    @Override
    @WrapInTransaction
    public Host findDefaultHost(User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        Host host;
        try{
            host  = (hostCache.getDefaultHost()!=null) ? hostCache.getDefaultHost() : getOrCreateDefaultHost();

            APILocator.getPermissionAPI().checkPermission(host, PermissionLevel.READ, user);
            return host;

        } catch (DotSecurityException | DotDataException e) {
            Logger.warn(HostAPIImpl.class, "Error trying to het default host:" + e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
        

    }

    /**
     * This method takes a server name (from a web request) and maps it to a host.
     * It is designed to do a lightweight cache lookup to get the mapping from server name -> host
     * and to prevent unnecessary lucene lookups
     * @param serverName
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    @CloseDBIfOpened
    public Host resolveHostName(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host host = hostCache.getHostByAlias(serverName);
        User systemUser = APILocator.systemUser();

        if(host == null || FOUR_OH_FOUR_HOSTNAME.equals(host.getHostname())){
            
            Optional<Host> optHost = resolveHostNameWithoutDefault(serverName, systemUser, respectFrontendRoles);
            host = optHost.orElse(findDefaultHost(systemUser, respectFrontendRoles));
            
            if(host != null){
                hostCache.addHostAlias(serverName, host);
            }
        }

        checkHostPermission(user, respectFrontendRoles, host);
        return host;
    }

    @Override
    @CloseDBIfOpened
    public Optional<Host> resolveHostNameWithoutDefault(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        Host host = hostCache.getHostByAlias(serverName);
        User systemUser = APILocator.systemUser();

        if(host == null){
            host = findByNameNotDefault(serverName, systemUser, respectFrontendRoles);

            if(host == null){
                host = findByAlias(serverName, systemUser, respectFrontendRoles);
            }
            if(host == null){
                host = FOUR_OH_FOUR_HOST;
            }
            hostCache.addHostAlias(serverName, host);
            
        }
        if(FOUR_OH_FOUR_HOSTNAME.equals(host.getHostname())) {
            return Optional.empty();
        }

        if (host != null) {
            checkHostPermission(user, respectFrontendRoles, host);
        }

        return Optional.ofNullable(host);
    }

    private void checkHostPermission(User user, boolean respectFrontendRoles, Host host) throws DotDataException, DotSecurityException {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            String u = (user != null) ? user.getUserId() : null;
            String h = (host != null) ? host.getHostname() : null;
            throw new DotSecurityException("User: " + u + " does not have read permissions to " + h);
        }
    }

    /**
     *
     * @param hostName
     * @param user
     * @param respectFrontendRoles
     * @return the host with the passed in name or default in error case
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    @CloseDBIfOpened
    public Host findByName(final String hostName,
                           final User user,
                           final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        
        try {
            return findByNameNotDefault(hostName, user, respectFrontendRoles);
        } catch (Exception e) {
            
            try {
                return findDefaultHost(APILocator.systemUser(), respectFrontendRoles);
            } catch(Exception ex){
                throw new DotRuntimeException(e.getMessage(), e);
            }
        }
    }
    
    /**
     *
     * @param hostName
     * @param user
     * @param respectFrontendRoles
     * @return the host with the passed in name or null in error case
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Host findByNameNotDefault(String hostName, User user, boolean respectFrontendRoles) {
        Host host = null;
        
        try{
            host  = hostCache.get(hostName);
            
            if(host != null){
                if(APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
                    return host;
                }
            }
            
        } catch(Exception e){
            Logger.debug(HostAPIImpl.class, e.getMessage(), e);
        }

        try {
            StringBuilder queryBuffer = new StringBuilder();
            queryBuffer.append(String.format("%s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME));
            queryBuffer.append(String.format(" +working:true +%s.hostName:%s", Host.HOST_VELOCITY_VAR_NAME, hostName));

            final List<Contentlet> list = APILocator.getContentletAPI().search(queryBuffer.toString(), 0, 0, null, user, respectFrontendRoles);
            
            if(list.size() > 1) {
                Logger.fatal(this, "More of one host has the same name or alias = " + hostName + "!!");
                int i=0;
                
                for(Contentlet c : list){
                    Logger.fatal(this, "\tdupe Host " + (i+1) + ": " + list.get(i).getTitle() );
                    i++;
                }
                
            }else if (list.size() == 0){
                return null;
            }
            
            host = new Host(list.get(0));
            hostCache.add(host);

            return host;
            
        }  catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @return the host with the passed in name
     */
    @Override
    public Host findByAlias(String alias, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host host = null;

        try {
            StringBuilder queryBuffer = new StringBuilder();
            queryBuffer.append(String.format("%s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME ));
            queryBuffer.append(String.format(" +working:true +%s.aliases:%s", Host.HOST_VELOCITY_VAR_NAME, alias));

            List<Contentlet> list = APILocator.getContentletAPI().search(queryBuffer.toString(), 0, 0, null, user, respectFrontendRoles);
            if(list.size() > 1){
                for(Contentlet cont: list){
                    final boolean isDefaultHost = (Boolean)cont.get(Host.IS_DEFAULT_KEY);
                    if(isDefaultHost){
                        host = new Host(cont);
                        if(host.isDefault()){
                            break;
                        }
                    }
                }
                if(host==null){
                    Logger.error(this, "More of one host match the same alias " + alias + "!!");
                    host = new Host(list.get(0));
                }
            }else if (list.size() == 0){
                return null;
            }else{
                host = new Host(list.get(0));
            }
            return host;
        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @CloseDBIfOpened
    public Host find(final String id, final User user,
                     final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        if(!UtilMethods.isSet(id)) {
            return null;
        }

        if(Host.SYSTEM_HOST.equals(id)){
            return findSystemHost();
        }

        Host host  = hostCache.get(id);

        if(host ==null){
            host = DBSearch(id,user,respectFrontendRoles);
        }

        if(host != null){
            if(!APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
                String u = (user != null) ? user.getUserId() : null;

                String message = "User " + u + " does not have permission to host:" + host.getHostname();
                Logger.error(HostAPIImpl.class, message);
                throw new DotSecurityException(message);
            }
        }
        return host;
    }

    /**
     * Retrieves the list of all hosts in the system
     * @throws DotSecurityException
     * @throws DotDataException
     *
     */
    @Override
    @CloseDBIfOpened
    public List<Host> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.findAll(user, 0, 0, null, respectFrontendRoles);
    }

    public List<Host> findAll(User user, int limit, int offset, String sortBy, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        try {
            StringBuilder queryBuffer = new StringBuilder();
            queryBuffer.append(String.format("%s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME ));
            queryBuffer.append(" +working:true");

            List<Contentlet> list = APILocator.getContentletAPI().search(queryBuffer.toString(), limit, offset, sortBy,
                    user, respectFrontendRoles);
            return convertToHostList(list);
        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves the list of all hosts in the system
     * @throws DotSecurityException
     * @throws DotDataException
     *
     */
    @Override
    @CloseDBIfOpened
    public List<Host> findAllFromDB(final User user,
                                    final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        final List<Host> hosts = new ArrayList<Host>();

        final String sql = "select  c.title, c.inode from contentlet_version_info clvi, contentlet c, structure s  " +
                " where c.structure_inode = s.inode and  s.name = 'Host' and c.identifier <> ? and clvi.working_inode = c.inode ";

        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Host.SYSTEM_HOST);
        @SuppressWarnings("unchecked")
        final List<Map<String,String>> ret = dc.loadResults();

        for(Map<String,String> m : ret) {
            String inode=m.get("inode");
            final Contentlet con=APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            hosts.add(new Host(con));
        }

        return hosts;
    }


    /**
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public Host save(Host host, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        if(host != null){
            hostCache.remove(host);
        }

        Contentlet contentletHost;
        try {
            contentletHost = APILocator.getContentletAPI().checkout(host.getInode(), user, respectFrontendRoles);
        } catch (DotContentletStateException e) {

            contentletHost = new Contentlet();
            contentletHost.setStructureInode(hostType().inode() );
        }

        contentletHost.getMap().put(Contentlet.DONT_VALIDATE_ME, host.getMap().get(Contentlet.DONT_VALIDATE_ME));
        APILocator.getContentletAPI().copyProperties(contentletHost, host.getMap());
        contentletHost.setInode("");
        contentletHost.setIndexPolicy(host.getIndexPolicy());
        contentletHost.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentletHost = APILocator.getContentletAPI().checkin(contentletHost, user, respectFrontendRoles);

        if(host.isWorking() || host.isLive()){
            APILocator.getVersionableAPI().setLive(contentletHost);
        }
        Host savedHost =  new Host(contentletHost);

        updateDefaultHost(savedHost, user, respectFrontendRoles);
        hostCache.clearAliasCache();
        return savedHost;

    }

    @Override
    @WrapInTransaction
    public void updateDefaultHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
        // If host is marked as default make sure that no other host is already set to be the default
        if(host.isDefault()) {
            ContentletAPI conAPI = APILocator.getContentletAPI();
            List<Host> hosts= findAllFromDB(user, respectFrontendRoles);
            Host otherHost;
            Contentlet otherHostContentlet;
            for(Host h : hosts){
                if(h.getIdentifier().equals(host.getIdentifier())){
                    continue;
                }
                // if this host is the default as well then ours should become the only one
                if(h.isDefault()){
                    boolean isHostRunning = h.isLive();
                    otherHostContentlet = APILocator.getContentletAPI().checkout(h.getInode(), user, respectFrontendRoles);
                    otherHost =  new Host(otherHostContentlet);
                    hostCache.remove(otherHost);
                    otherHost.setDefault(false);
                    if(host.getMap().containsKey(Contentlet.DONT_VALIDATE_ME))
                        otherHost.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                    if(host.getMap().containsKey(Contentlet.DISABLE_WORKFLOW))
                        otherHost.setProperty(Contentlet.DISABLE_WORKFLOW,true);

                    Contentlet cont = conAPI.checkin(otherHost, user, respectFrontendRoles);
                    if(isHostRunning) {
                        otherHost = new Host(cont);
                        publish(otherHost, user, respectFrontendRoles);
                    }
                }
            }
        }
    }

    @Override
    @CloseDBIfOpened
    public List<Host> getHostsWithPermission(int permissionType, boolean includeArchived, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        try {
            StringBuilder queryBuffer = new StringBuilder();
            queryBuffer.append(String.format("%s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME));
            queryBuffer.append(" +working:true");

            List<Contentlet> list = APILocator.getContentletAPI().search(queryBuffer.toString(), 0, 0, null, user, respectFrontendRoles);
            list = APILocator.getPermissionAPI().filterCollection(list, permissionType, respectFrontendRoles, user);
            if (includeArchived) {
                return convertToHostList(list);
            } else {
                List<Host> hosts = convertToHostList(list);

                List<Host> filteredHosts = new ArrayList<Host>();
                for (Host host: hosts) {
                    if (!host.isArchived())
                        filteredHosts.add(host);
                }

                return filteredHosts;
            }
        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Host> getHostsWithPermission(int permissionType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return getHostsWithPermission(permissionType, true, user, respectFrontendRoles);
    }

    @Override
    @CloseDBIfOpened
    public Host findSystemHost (User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(systemHost != null){
            return systemHost;
        }

        try {
            String systemHostSql = "select id from identifier where id = ?";
            DotConnect db  = new DotConnect();
            db.setSQL(systemHostSql);
            db.addParam(Host.SYSTEM_HOST);
            List<Map<String, Object>> rs = db.loadObjectResults();
            if(rs.isEmpty()) {
                createSystemHost();
            } else {
                final String systemHostId = (String) rs.get(0).get("id");
                this.systemHost = DBSearch(systemHostId, user, respectFrontendRoles);
            }
            if(rs.size() > 1){
                Logger.fatal(this, "There is more than one working version of the system host!!");
            }
        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return systemHost;
    }

    @Override
    public Host findSystemHost () throws DotDataException {

        try {
            return findSystemHost(APILocator.systemUser(), false);
        } catch (DotSecurityException e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }

    }

    @Override
    public Host findParentHost(Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(folder.getIdentifier() !=null){
            return find(APILocator.getIdentifierAPI().find(folder.getIdentifier()).getHostId(), user, respectFrontendRoles);
        }
        return findDefaultHost(user, respectFrontendRoles);
    }

    @Override
    public Host findParentHost(WebAsset asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(asset.getIdentifier()!=null){
            return find(APILocator.getIdentifierAPI().find(asset.getIdentifier()).getHostId(), user, respectFrontendRoles);
        }

        return null;
    }

    @Override
    public Host findParentHost(Treeable asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(asset.getIdentifier()!=null){
            return find(APILocator.getIdentifierAPI().find(asset.getIdentifier()).getHostId(), user, respectFrontendRoles);
        }
        return null;
    }

    @Override
    public boolean doesHostContainsFolder(Host parent, String folderName) throws DotDataException, DotSecurityException {
        List<Folder> trees = APILocator.getFolderAPI().findFoldersByHost(parent, APILocator.systemUser(), false);
        for (Folder folder : trees) {
            if (folder.getName().equals(folderName))
                return true;
        }
        return false;

    }

    @Override
    public void delete(final Host host, final User user, final boolean respectFrontendRoles) {
        delete(host,user,respectFrontendRoles,false);
    }

    @Override
    @CloseDBIfOpened
    public Optional<Future<Boolean>> delete(final Host host, final User deletingUser,
                                   final boolean respectFrontendRoles,
                                   final boolean runAsSeparatedThread) {

        Optional<Future<Boolean>> future = Optional.empty();
        try {

            Logger.debug(this, ()-> "Deleting the host: " + host);
            APILocator.getPermissionAPI().checkPermission(host, PermissionLevel.PUBLISH, deletingUser);
        } catch (DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }

        final User user = (null != deletingUser)?deletingUser:APILocator.systemUser();
        class DeleteHostThread implements Callable<Boolean> {

            @WrapInTransaction
            @Override
            public Boolean call() {

                try {
                    deleteHost();
                    HibernateUtil.addCommitListener
                            (() -> generateNotification());
                } catch (Exception e) {
                    // send notification
                    try {

                        APILocator.getNotificationAPI().generateNotification(
                                new I18NMessage("notification.hostapi.delete.error.title"), // title = Host Notification
                                new I18NMessage("notifications_host_deletion_error", host.getHostname(), e.getMessage()),
                                null, // no actions
                                NotificationLevel.ERROR,
                                NotificationType.GENERIC,
                                user.getUserId(),
                                user.getLocale()
                        );

                    } catch (DotDataException e1) {
                        Logger.error(HostAPIImpl.class, "error saving Notification", e);
                    }

                    Logger.error(HostAPIImpl.class, e.getMessage(), e);
                    throw new DotRuntimeException(e.getMessage(), e);
                }

                return Boolean.TRUE;
            }

            private void generateNotification() {
                try {

                    APILocator.getNotificationAPI().generateNotification(
                            new I18NMessage("message.host.delete.title"), // title = Host Notification
                            new I18NMessage("message.host.delete",
                                    "Site deleted:" + host.getHostname(), host.getHostname()),
                            null, // no actions
                            NotificationLevel.INFO,
                            NotificationType.GENERIC,
                            user.getUserId(),
                            user.getLocale());
                } catch (Exception e) {

                    Logger.debug(this, e.getMessage(), e);
                }
            }

            public void deleteHost() throws Exception {
                if(host != null){
                    hostCache.remove(host);
                }

                final DotConnect dc = new DotConnect();

                // Remove Links
                MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
                List<Link> links = linkAPI.findLinks(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
                for (Link link : links) {
                    linkAPI.delete(link, user, respectFrontendRoles);
                }

                // Remove Contentlet
                ContentletAPI contentAPI = APILocator.getContentletAPI();
                contentAPI.deleteByHost(host, APILocator.systemUser(), respectFrontendRoles);

                // Remove Folders
                FolderAPI folderAPI = APILocator.getFolderAPI();
                List<Folder> folders = folderAPI.findFoldersByHost(host, user, respectFrontendRoles);
                for (Folder folder : folders) {
                    folderAPI.delete(folder, user, respectFrontendRoles);
                }

                // Remove Templates
                TemplateAPI templateAPI = APILocator.getTemplateAPI();
                List<Template> templates = templateAPI.findTemplatesAssignedTo(host, true);
                for (Template template : templates) {
                    dc.setSQL("delete from template_containers where template_id = ?");
                    dc.addParam(template.getIdentifier());
                    dc.loadResult();

                    templateAPI.delete(template, user, respectFrontendRoles);
                }

                // Remove Containers
                ContainerAPI containerAPI = APILocator.getContainerAPI();
                List<Container> containers = containerAPI.findContainers(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
                for (Container container : containers) {
                    containerAPI.delete(container, user, respectFrontendRoles);
                }

                // Remove Structures
                List<ContentType> types = APILocator.getContentTypeAPI(user, respectFrontendRoles).search(" host = '" + host.getIdentifier() + "'");

                for (ContentType type : types) {
                    List<Contentlet> structContent = contentAPI.findByStructure(new StructureTransformer(type).asStructure(), APILocator.systemUser(), false, 0, 0);
                    for (Contentlet c : structContent) {
                        //We are deleting a site/host, we don't need to validate anything.
                        c.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                        contentAPI.delete(c, user, respectFrontendRoles);
                    }

                    ContentTypeAPI contentTypeAPI = APILocator
                            .getContentTypeAPI(user, respectFrontendRoles);
                    //Validate if are allow to delete this content type
                    if (!type.system() && !type.defaultType()) {
                        contentTypeAPI.delete(type);
                    } else {
                        //If we can not delete it we need to change the host to SYSTEM_HOST
                        ContentType clonedContentType = ContentTypeBuilder.builder(type)
                                .host(findSystemHost(user, false).getIdentifier()).build();
                        contentTypeAPI.save(clonedContentType);
                    }

                }

                // wipe bad old containers
                dc.setSQL("delete from container_structures where exists (select * from identifier where host_inode=? and container_structures.container_id=id)");
                dc.addParam(host.getIdentifier());
                dc.loadResult();

                Inode.Type[] assets = {Inode.Type.CONTAINERS, Inode.Type.TEMPLATE, Inode.Type.LINKS};
                for(Inode.Type asset : assets) {
                    dc.setSQL("select inode from "+asset.getTableName()+" where exists (select * from identifier where host_inode=? and id="+asset.getTableName()+".identifier)");
                    dc.addParam(host.getIdentifier());
                    for(Map row : (List<Map>)dc.loadResults()) {
                        dc.setSQL("delete from "+asset.getVersionTableName()+" where working_inode=? or live_inode=?");
                        dc.addParam(row.get("inode"));
                        dc.addParam(row.get("inode"));
                        dc.loadResult();

                        dc.setSQL("delete from "+asset.getTableName()+" where inode=?");
                        dc.addParam(row.get("inode"));
                        dc.loadResult();
                    }
                }

                //Remove Tags
                APILocator.getTagAPI().deleteTagsByHostId(host.getIdentifier());

                // Double-check that ALL contentlets are effectively removed  
                // before using dotConnect to kill bad identifiers
                List<Contentlet> remainingContenlets = contentAPI
                        .findContentletsByHost(host, user, respectFrontendRoles);
                if (remainingContenlets != null
                        && remainingContenlets.size() > 0) {
                    contentAPI.deleteByHost(host, user, respectFrontendRoles);
                }
                
                // kill bad identifiers pointing to the host
                dc.setSQL("delete from identifier where host_inode=?");
                dc.addParam(host.getIdentifier());
                dc.loadResult();

                // Remove Host
                Contentlet c = contentAPI.find(host.getInode(), user, respectFrontendRoles);
                contentAPI.delete(c, user, respectFrontendRoles);
                hostCache.remove(host);
                hostCache.clearAliasCache();
            }
        }
        final DeleteHostThread deleteHostThread = new DeleteHostThread();

        if(runAsSeparatedThread) {

            future = Optional.of(this.concurrentFactory.getSubmitter
                    (DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL).submit(deleteHostThread));
        } else {
            deleteHostThread.call();
        }

        return future;
    } // delete.

    @Override
    @WrapInTransaction
    public void archive(final Host host, final User user,
                        final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException,
            DotContentletStateException {

        if(host != null) {

            hostCache.remove(host);
        }

        final Contentlet contentlet = APILocator.getContentletAPI().find
                (host.getInode(), user, respectFrontendRoles);
        //retrieve all hosts that have this current host as tag storage host
        final List<Host> hosts = retrieveHostsPerTagStorage(host.getIdentifier(), user);
        for(Host hostItem: hosts) {
            if(hostItem.getIdentifier() != null){
                if(!hostItem.getIdentifier().equals(host.getIdentifier())){
                    //prevents changing tag storage for archived host.
                    //the tag storage will change for all hosts which tag storage is archived host
                    // Apparently this code updates all other hosts setting their own self as tag storage
                    hostItem.setTagStorage(hostItem.getIdentifier());
                    //So In order to avoid an exception updating a host that could be archived we're gonna tell the API to skip validation.
                    hostItem.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
                    hostItem = save(hostItem, user, true);
                }
            }
        }

        contentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
        APILocator.getContentletAPI().archive(contentlet, user, respectFrontendRoles);
        host.setModDate(new Date ());
        hostCache.clearAliasCache();

        HibernateUtil.addCommitListener(() -> this.sendArchiveSiteSystemEvent(contentlet), 1000);
    }

    private void sendArchiveSiteSystemEvent (final Contentlet contentlet) {

        try {
            this.systemEventsAPI.pushAsync(SystemEventType.ARCHIVE_SITE, new Payload(contentlet, Visibility.PERMISSION,
                    String.valueOf(PermissionAPI.PERMISSION_READ)));
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    @Override
    @WrapInTransaction
    public void unarchive(final Host host, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException,
            DotContentletStateException {

        if(host != null) {

            hostCache.remove(host);
        }

        final Contentlet contentlet = APILocator.getContentletAPI()
                .find(host.getInode(), user, respectFrontendRoles);
        APILocator.getContentletAPI().unarchive(contentlet, user, respectFrontendRoles);
        host.setModDate(new Date ());
        hostCache.clearAliasCache();
        HibernateUtil.addCommitListener(() -> this.sendUnArchiveSiteSystemEvent(contentlet), 1000);
    }

    private void sendUnArchiveSiteSystemEvent (final Contentlet contentlet) {

        try {
            
            DateUtil.sleep(DateUtil.SECOND_MILLIS * 2);

            systemEventsAPI.pushAsync(SystemEventType.UN_ARCHIVE_SITE, new Payload(contentlet, Visibility.PERMISSION,
                        String.valueOf(PermissionAPI.PERMISSION_READ)));

        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }


    @WrapInTransaction
    private synchronized Host getOrCreateDefaultHost() throws DotDataException,
    DotSecurityException {
        
        List<Field> fields = hostType().fields();
        Field isDefault = null;
        for(Field f : fields){
            if("isDefault".equalsIgnoreCase(f.variable())){
                isDefault=f;
            }
        }

        DotConnect dc = new DotConnect();
        dc.setSQL("select working_inode from contentlet_version_info join contentlet on (contentlet.inode = contentlet_version_info.working_inode) " +
                  " where " + isDefault.dbColumn() +" = ? and structure_inode =?");
        dc.addParam(true);
        dc.addParam(hostType().inode());
        String inode = dc.getString("working_inode");

        Host defaultHost = new Host();

        if(UtilMethods.isSet(inode)){
            defaultHost = new Host(APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false));
            hostCache.add(defaultHost);
        } else {
            defaultHost.setDefault(true);
            defaultHost.setHostname("noDefault-"  + System.currentTimeMillis());

            for(Field f : fields){
                if(f.required() && UtilMethods.isSet(f.defaultValue())){
                    defaultHost.setProperty(f.variable(), f.defaultValue());
                }
            }
            defaultHost.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
            defaultHost = save(defaultHost, APILocator.systemUser(), false);
         

            sendNotification();
        }



        return defaultHost;

    }

    private void sendNotification() {
        try {

            Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();

            APILocator.getNotificationAPI().generateNotification(
                    new I18NMessage("NO DEFAULT HOST"), // title = Reindex Notification
                    new I18NMessage("THERE IS NO DEFAULT HOST " ),
                    null, // no action

                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    Visibility.ROLE,
                    cmsAdminRole.getId(),
                    APILocator.systemUser().getUserId(),
                    APILocator.systemUser().getLocale());

            throw new DotStateException("NO DEFAULT HOST, creating a fake one");
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    @WrapInTransaction
    private synchronized Host createSystemHost() throws DotDataException,
    DotSecurityException {

        User systemUser = APILocator.systemUser();

        String systemHostSql = "select id from identifier where id = ?";
        DotConnect db  = new DotConnect();
        db.setSQL(systemHostSql);
        db.addParam(Host.SYSTEM_HOST);
        List<Map<String, Object>> rs = db.loadObjectResults();
        if(rs.isEmpty()) {
            Host systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            systemHost.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            systemHost = new Host(contentletFactory.save(systemHost));
            systemHost.setIdentifier(Host.SYSTEM_HOST);
            systemHost.setModDate(new Date());
            systemHost.setModUser(systemUser.getUserId());
            systemHost.setOwner(systemUser.getUserId());
            systemHost.setHost(null);
            systemHost.setFolder(null);
            contentletFactory.save(systemHost);
            APILocator.getVersionableAPI().setWorking(systemHost);
            this.systemHost = systemHost;
        } else {
            final String systemHostId = (String) rs.get(0).get("id");
            this.systemHost =  APILocator.getHostAPI().DBSearch(systemHostId, systemUser, false);
        }
        return systemHost;
    }
    
    private List<Host> convertToHostList(List<Contentlet> list) {
        List<Host> hosts = new ArrayList<Host>();
        for(Contentlet c : list) {
            hosts.add(new Host(c));
        }
        return hosts;
    }

    @Override
    public void publish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {

        if(host != null){
            hostCache.remove(host);
        }

        final Contentlet contentletHost = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
        contentletHost.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(contentletHost, user, respectFrontendRoles);
        hostCache.add(host);
        hostCache.clearAliasCache();

    }

    @Override
    public void unpublish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
        if(host != null){
            hostCache.remove(host);
        }
        Contentlet c = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
        APILocator.getContentletAPI().unpublish(c, user, respectFrontendRoles);
        hostCache.add(host);
        hostCache.clearAliasCache();
    }

    @WrapInTransaction
    @Override
    public void makeDefault(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
        host.setDefault(true);
        host.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
        save(host, user, respectFrontendRoles);
    }

    @Override
    @CloseDBIfOpened
    public Host DBSearch(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (!UtilMethods.isSet(id))
            return null;

        Host host = null;

        final ContentletVersionInfo vinfo = HibernateUtil.load(ContentletVersionInfo.class,
                "from "+ContentletVersionInfo.class.getName()+" where identifier=?", id);

        if(vinfo!=null && UtilMethods.isSet(vinfo.getIdentifier())) {

            User systemUser = APILocator.systemUser();

            String hostInode=vinfo.getWorkingInode();
            final Contentlet cont= APILocator.getContentletAPI().find(hostInode, systemUser, respectFrontendRoles);
            final ContentType type =APILocator.getContentTypeAPI(systemUser, respectFrontendRoles).find(Host.HOST_VELOCITY_VAR_NAME);
            if(cont.getStructureInode().equals(type.inode())) {
                host=new Host(cont);
                hostCache.add(host);
            }
        }

        return host;
    }

    @Override
    public void updateCache(Host host) {
        hostCache.remove(host);
        hostCache.clearAliasCache();
        hostCache.add(new Host(host));
    }

    @Override
    public List<String> parseHostAliases(Host host) {
        List<String> ret = new ArrayList<String>();
        if(host.getAliases() == null){
            return ret;
        }
        StringTokenizer tok = new StringTokenizer(host.getAliases(), ", \n\r\t");
        while (tok.hasMoreTokens()) {
            ret.add(tok.nextToken());
        }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    @WrapInTransaction
    public void updateMenuLinks(Host workinghost,Host updatedhost) throws DotDataException {//DOTCMS-5090

        String workingHostName = workinghost.getHostname();
        String updatedHostName = updatedhost.getHostname();
        if(!workingHostName.equals(updatedHostName)) {
            HibernateUtil dh = new HibernateUtil(Link.class);
            List<Link> resultList = new ArrayList<Link>();
            dh.setQuery("select asset from asset in class " + Link.class.getName() + " where asset.url like ?");
            dh.setParam(workingHostName+"/%");
            resultList = dh.list();
            for(Link link : resultList){
                try {
                    //We need to ONLY update links that are INTERNALS and working/live.
                    //https://github.com/dotCMS/core/issues/10609
                    if ( Link.LinkType.INTERNAL.toString().equals(link.getLinkType() )
                        && ( link.isLive() || link.isWorking() )){

                        String workingURL = link.getUrl();
                        String newURL = updatedHostName+workingURL.substring(workingHostName.length());//gives url with updatedhostname
                        link.setUrl(newURL);
                        try {
                            APILocator.getMenuLinkAPI().save(link, APILocator.systemUser(), false);
                        } catch (DotSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (DotSecurityException e){
                    Logger.error(this, "Could not update Menu Link with inode" + link.getInode());
                }
            }
        }

    }

    @Override
    @CloseDBIfOpened
    public List<Host> retrieveHostsPerTagStorage (String tagStorageId, User user) {
        List<Host> hosts = new ArrayList<Host>();
        List<Host> allHosts = new ArrayList<Host>();
        try {
            allHosts = findAll(user, true);
        } catch (DotDataException e) {
            e.printStackTrace();
        } catch (DotSecurityException e) {
            e.printStackTrace();
        }

        if (allHosts.size() > 0) {
            for (Host h: allHosts) {
                if(h.isSystemHost())
                    continue;
                if (h.getTagStorage() != null){
                    if(h.getTagStorage().equals(tagStorageId))
                        hosts.add(h);
                }
            }
        }

        return hosts;

    }

    @Override
    public PaginatedArrayList<Host> searchByStopped(String filter, boolean showStopped, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles){
        String condition = String.format(" +live:%b", !showStopped);
        return search(filter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public PaginatedArrayList<Host> search(String filter, boolean showArchived, boolean showStopped, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles){
        String condition = String.format(" +deleted:%b +live:%b", showArchived, !showStopped);
        return search(filter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public PaginatedArrayList<Host> search(String filter, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles){
        return search(filter, StringUtils.EMPTY, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
	public PaginatedArrayList<Host> search(String filter, boolean showArchived, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles) {
        String condition = String.format(" +deleted:%b", showArchived);
        return search(filter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    private PaginatedArrayList<Host> search(String filter, String condition, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles) {
        try {

            StringBuilder queryBuffer = new StringBuilder(condition);
            queryBuffer.append(String.format(" %s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME));

            if(UtilMethods.isSet(filter)){
                queryBuffer.append( String.format(" +%s.hostName:%s*", Host.HOST_VELOCITY_VAR_NAME, filter.trim() ) );
            }
            if(!showSystemHost){
                queryBuffer.append( String.format(" +%s.isSystemHost:false", Host.HOST_VELOCITY_VAR_NAME));
            }
            PaginatedArrayList<Contentlet> list = (PaginatedArrayList<Contentlet>)APILocator.getContentletAPI().search( queryBuffer.toString(), limit, offset, Host.HOST_VELOCITY_VAR_NAME + ".hostName", user, respectFrontendRoles);

            return convertToHostPaginatedArrayList(list);
        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Return the number of sites for user
     *
     * @param user
     * @param respectFrontendRoles
     * @return
     */
    @Override
    public long count(User user, boolean respectFrontendRoles) {
        try {
            return APILocator.getContentletAPI()
                    .indexCount(String.format("%s:%s", CONTENT_TYPE_CONDITION, Host.HOST_VELOCITY_VAR_NAME), user, respectFrontendRoles);

        } catch (Exception e) {
            Logger.error(HostAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

	private PaginatedArrayList<Host> convertToHostPaginatedArrayList(PaginatedArrayList<Contentlet> list) {
		
		PaginatedArrayList<Host> hosts = new PaginatedArrayList<Host>();
		hosts.addAll(list.stream().map( content -> new Host(content)).collect(Collectors.toList()));
		hosts.setQuery(list.getQuery());
		hosts.setTotalResults(list.getTotalResults());
		
		return hosts;
	}
}