package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.web.UpdateContainersPathsJob;
import com.dotmarketing.portlets.contentlet.business.web.UpdatePageTemplatePathJob;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This API allows developers to access information related to Sites objects in your dotCMS content repository.
 * <p>
 * A single dotCMS instance may manage multiple different web sites. Each “site” is actually a separate website, but all
 * sites are managed and the content for all sites is served from a single dotCMS instance. A single dotCMS server can
 * manage literally hundreds of sites.</p>
 *
 * @author jtesser
 * @author david torres
 */
public class HostAPIImpl implements HostAPI, Flushable<Host> {

    private final HostCache hostCache = CacheLocator.getHostCache();
    private Host systemHost;
    private final SystemEventsAPI systemEventsAPI;
    private HostFactory hostFactory;

    public HostAPIImpl() {
        this(APILocator.getSystemEventsAPI());
    }

    @VisibleForTesting
    HostAPIImpl(final SystemEventsAPI systemEventsAPI) {
        this.systemEventsAPI = systemEventsAPI;
    }

    private ContentType hostType() throws DotDataException, DotSecurityException{
        return APILocator.getContentTypeAPI(APILocator.systemUser()).find(Host.HOST_VELOCITY_VAR_NAME);
    }

    /**
     * Lazy initialization of the Host Factory service. This helps prevent startup issues when several factories or APIs
     * are initialized during the initialization phase of the Host Factory.
     *
     * @return An instance of the {@link HostFactory} service.
     */
    private HostFactory getHostFactory() {
        if (null == this.hostFactory) {
            this.hostFactory = FactoryLocator.getHostFactory();
        }
        return this.hostFactory;
    }

    @Override
    @CloseDBIfOpened
    public Host findDefaultHost(User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        Host site;
        try{
            site  = (this.hostCache.getDefaultHost()!=null) ? this.hostCache.getDefaultHost() : getOrCreateDefaultHost();

            APILocator.getPermissionAPI().checkPermission(site, PermissionLevel.READ, user);
            return site;

        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(HostAPIImpl.class, String.format("An error occurred when user '%s' tried to get the default " +
                    "Site: %s", user.getUserId(), e.getMessage()));
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format("User '%s' could not retrieve the default Site: %s", user
                    .getUserId(), e.getMessage()), e);
        }
        

    }

    @Override
    @CloseDBIfOpened
    public Host resolveHostName(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        User systemUser = APILocator.systemUser();
        Host host;
        try {
            final Optional<Host> optional =
                    resolveHostNameWithoutDefault(serverName, systemUser, respectFrontendRoles);
            host = optional.isPresent() ? optional.get() : findDefaultHost(systemUser, respectFrontendRoles);
        } catch (Exception e) {
            Logger.debug(this, "Exception resolving host using default", e);
            host = findDefaultHost(systemUser, respectFrontendRoles);
        }
        checkSitePermission(user, respectFrontendRoles, host);
        return host;
    }

    @Override
    @CloseDBIfOpened
    public Optional<Host> resolveHostNameWithoutDefault(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        Host host;
        final Host cachedHostByAlias = hostCache.getHostByAlias(serverName);
        if (UtilMethods.isSet(() -> cachedHostByAlias.getIdentifier())) {
            if (HostCache.CACHE_404_HOST.equals(cachedHostByAlias.getIdentifier())) {
                return Optional.empty();
            }
            host = cachedHostByAlias;
        } else {
            User systemUser = APILocator.systemUser();
            host = findByNameNotDefault(serverName, systemUser, respectFrontendRoles);

            if(host == null){
                host = findByAlias(serverName, systemUser, respectFrontendRoles);
            }

            if (host == null) {
                hostCache.addHostAlias(serverName, HostCache.cache404Contentlet);
            } else {
                hostCache.addHostAlias(serverName, host);
            }
        }

        if (host != null) {
            checkSitePermission(user, respectFrontendRoles, host);
        }

        return Optional.ofNullable(host);
    }

    /**
     * Verifies that the specified User has READ permission on a given Site. If it doesn't, a
     * {@link DotSecurityException} will be thrown
     *
     * @param user                 The {@link User} whose READ permission needs to be checked.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     * @param site                 The {@link Host} that will be validated.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    private void checkSitePermission(final User user, final boolean respectFrontendRoles, final Host site) throws DotDataException, DotSecurityException {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            final String userId = Try.of(user::getUserId).getOrElse("- null -");
            final String siteName = Try.of (site::getHostname).getOrElse("- null -");
            throw new DotSecurityException(String.format("User '%s' does not have read permissions on '%s'", userId,
                    siteName));
        }
    }

    @Override
    @CloseDBIfOpened
    public Host findByName(final String siteName,
                           final User user,
                           final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        
        try {
            return findByNameNotDefault(siteName, user, respectFrontendRoles);
        } catch (Exception e) {
            
            try {
                return findDefaultHost(APILocator.systemUser(), respectFrontendRoles);
            } catch(Exception ex){
                throw new DotRuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the Site that matches the specified name. Unlike the {@link #findByName(String, User, boolean)} method,
     * if no Site matches the specified name, the default Site will NOT be returned instead.
     *
     * @param siteName             The name of the Site.
     * @param user                 The user performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The {@link Host} object that matches the specified name.
     */
    private Host findByNameNotDefault(final String siteName, final User user, final boolean respectFrontendRoles) {
        final Host site = this.getHostFactory().bySiteName(siteName);
        if (null != site) {
            try {
                checkSitePermission(user, respectFrontendRoles, site);
                return site;
            } catch (final DotDataException | DotSecurityException e) {
                Logger.error(this, String.format("An error occurred when checking READ permission from User '%s' on " +
                        "Site '%s': %s", user.getUserId(), siteName, e.getMessage()), e);
            }
        }
        return null;
    }

    @Override
    public Optional<Host> findByIdOrKey(final String siteIdOrKey, final User user,
                                        final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        checkNotEmpty(siteIdOrKey, IllegalArgumentException.class, "'siteIdOrKey' parameter cannot be null or empty");
        checkNotNull(user, IllegalArgumentException.class, "'user' parameter cannot be null");
        final String trimmedSiteIdOrKey = siteIdOrKey.trim();
        final Optional<Host> siteOpt = UUIDUtil.isUUID(trimmedSiteIdOrKey) || Host.SYSTEM_HOST.equals(trimmedSiteIdOrKey)
                ? Optional.ofNullable(find(trimmedSiteIdOrKey, user, respectFrontendRoles))
                : resolveHostNameWithoutDefault(trimmedSiteIdOrKey, APILocator.systemUser(), respectFrontendRoles);
        if (siteOpt.isPresent()) {
            this.checkSitePermission(user, respectFrontendRoles, siteOpt.get());
        } else {
            Logger.debug(this, () -> String.format("Site ID/Key '%s' was not found", siteIdOrKey));
        }
        return siteOpt;
    }

    @Override
    @CloseDBIfOpened
    public Host findByAlias(final String alias, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        final Host site = this.getHostFactory().byAlias(alias);
        if (null != site) {
            try {
                checkSitePermission(user, respectFrontendRoles, site);
                return site;
            } catch (final DotDataException | DotSecurityException e) {
                Logger.error(this, String.format("An error occurred when checking READ permission from User '%s' on " +
                        "Site Alias '%s': %s", user.getUserId(), alias, e.getMessage()), e);
            }
        }
        return null;
    }

    /**
     * Utility method used to transform a list of Site Aliases into a list. Site Aliases can be separated by either
     * commas, blank spaces, or line breaks, so this method (1) replaces the separation characters with blank spaces,
     * and then (2) adds each element to a list.
     *
     * @param data The list of Site Aliases for a given Site.
     *
     * @return A {@link List} with every unique Site Alias.
     */
    private List<String> parseSiteAliases(final String data) {
        final List<String> result = new ArrayList<>();
        final StringTokenizer tok = new StringTokenizer(data, ", \n\r\t");
        while (tok.hasMoreTokens()) {
            result.add(tok.nextToken());
        }
        return result;
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

        Host site = null;
        Host cachedSiteById = hostCache.getById(id);
        if (UtilMethods.isSet(() -> cachedSiteById.getIdentifier())) {
            if (HostCache.CACHE_404_HOST.equals(cachedSiteById.getIdentifier())) {
                return null;
            }
            site = cachedSiteById;
        }

        if (site == null) {
            site = DBSearch(id,user,respectFrontendRoles);
        }

        if(site != null){
            if(!APILocator.getPermissionAPI().doesUserHavePermission(site, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
                String u = (user != null) ? user.getUserId() : null;
                String message = String.format("User '%s' does not have READ permissions on Site '%s'", user, site);
                Logger.error(HostAPIImpl.class, message);
                throw new DotSecurityException(message);
            }
        }
        return site;
    }

    @Override
    @CloseDBIfOpened
    public Host find(final Contentlet contentlet,
                     final User user,
                     final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return find(
                org.apache.commons.lang3.StringUtils.defaultIfBlank(
                        contentlet.getHost(),
                        contentlet.getContentType().host()),
                user,
                respectFrontendRoles);
    }

    @Override
    @Deprecated
    public List<Host> findAll(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.findAllFromDB(user, respectFrontendRoles);
    }

    @Override
    public List<Host> findAll(final User user, final int limit, final int offset, final String sortBy, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return this.findPaginatedSitesFromDB(user, limit, offset, sortBy, respectFrontendRoles);
    }

    @Override
    public List<Host> findAllFromDB(final User user, final boolean respectFrontendRoles) throws DotDataException,
            DotSecurityException {
        return this.findAllFromDB(user, true, respectFrontendRoles);
    }

    @Override
    public List<Host> findAllFromDB(final User user, final boolean includeSystemHost,
                                    final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.findPaginatedSitesFromDB(user, 0, 0, null, includeSystemHost, respectFrontendRoles);
    }

    /**
     * Returns an optionally paginated list of all Sites in your dotCMS content repository, including the System Host.
     *
     * @param user                 The {@link User} performing this action.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param sortBy               Optional sorting criterion, as specified by the available columns in: {@link
     *                             com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    @CloseDBIfOpened
    private List<Host> findPaginatedSitesFromDB(final User user, final int limit, final int offset, final String
            sortBy, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.findPaginatedSitesFromDB(user, limit, offset, sortBy, true, respectFrontendRoles);
    }

    /**
     * Returns an optionally paginated list of all Sites in your dotCMS content repository. This method allows you to
     * <b>EXCLUDE</b> the System Host from the result list.
     *
     * @param user                 The {@link User} performing this action.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param sortBy               Optional sorting criterion, as specified by the available columns in: {@link
     *                             com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}
     * @param includeSystemHost    If the System Host should be included in the result list, set to {@code true}.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    private List<Host> findPaginatedSitesFromDB(final User user, final int limit, final int offset,
                                                final String sortBy, final boolean includeSystemHost,
                                                final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        final List<Host> siteList = this.getHostFactory().findAll(limit, offset, sortBy, includeSystemHost);
        if (null != siteList && !siteList.isEmpty()) {
            return siteList.stream().filter(site -> {
                try {
                    if (site.isSystemHost() && !user.isAdmin()){
                        return includeSystemHost &&
                               APILocator.getPermissionAPI().doesSystemHostHavePermissions(APILocator.systemHost(), user, respectFrontendRoles, Host.class.getCanonicalName());
                    }
                    this.checkSitePermission(user, respectFrontendRoles, site);
                    return true;
                } catch (final DotDataException | DotSecurityException e) {
                    Logger.warn(this,
                            String.format("An error occurred when checking permissions from User '%s' on " + "Site " +
                                                  "'%s': %s", user.getUserId(), site.getInode(), e.getMessage()));
                }
                return false;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<Host> findAllFromCache(final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Set<Host> cachedSites = hostCache.getAllSites();
        if(null == cachedSites){
            final List<Host> allFromDB = findAllFromDB(user, respectFrontendRoles);
            hostCache.addAll(allFromDB);
            cachedSites = hostCache.getAllSites();
        }
        return ImmutableList.copyOf(cachedSites);
    }

    @Override
    @WrapInTransaction
    public Host save(final Host hostToBeSaved, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        if(hostToBeSaved != null){
            hostCache.remove(hostToBeSaved);
        }

        Contentlet contentletHost;
        try {
            contentletHost = APILocator.getContentletAPI().checkout(hostToBeSaved.getInode(), user, respectFrontendRoles);
        } catch (DotContentletStateException e) {

            contentletHost = new Contentlet();
            contentletHost.setStructureInode(hostType().inode() );
        }

        if(null != contentletHost.get(Host.HOST_NAME_KEY) && null != hostToBeSaved.get(Host.HOST_NAME_KEY) &&
                !contentletHost.get(Host.HOST_NAME_KEY).equals(hostToBeSaved.get(Host.HOST_NAME_KEY)) &&
                !hostToBeSaved.getBoolProperty("forceExecution")){
            throw new IllegalArgumentException("Updating the hostName is a Dangerous Execution, to achieve this 'forceExecution': true property needs to be sent.");
        }

        contentletHost.getMap().put(Contentlet.DONT_VALIDATE_ME, hostToBeSaved.getMap().get(Contentlet.DONT_VALIDATE_ME));
        APILocator.getContentletAPI().copyProperties(contentletHost, hostToBeSaved.getMap());
        contentletHost.setInode("");
        contentletHost.setIndexPolicy(hostToBeSaved.getIndexPolicy());
        contentletHost.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentletHost = APILocator.getContentletAPI().checkin(contentletHost, user, respectFrontendRoles);

        if (null != contentletHost.get(Host.HOST_NAME_KEY) && null != hostToBeSaved.get(Host.HOST_NAME_KEY) &&
                !contentletHost.isNew() && !contentletHost.getTitle().equals(hostToBeSaved.get(Host.HOST_NAME_KEY))) {

            UpdateContainersPathsJob.triggerUpdateContainersPathsJob(
                    hostToBeSaved.get(Host.HOST_NAME_KEY).toString(),
                    (String) contentletHost.get(Host.HOST_NAME_KEY)
            );
            UpdatePageTemplatePathJob.triggerUpdatePageTemplatePathJob(
                    hostToBeSaved.get(Host.HOST_NAME_KEY).toString(),
                    (String) contentletHost.get(Host.HOST_NAME_KEY)
            );
        }

        if(hostToBeSaved.isWorking() || hostToBeSaved.isLive()){
            APILocator.getVersionableAPI().setLive(contentletHost);
        }
        Host savedHost =  new Host(contentletHost);

        updateDefaultHost(savedHost, user, respectFrontendRoles);
        this.flushAllCaches(savedHost);
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
    @Deprecated
    public List<Host> getHostsWithPermission(int permissionType, boolean includeArchived, final User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return getHostsWithPermission(permissionType, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public List<Host> getHostsWithPermission(int permissionType, final User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        final List<Host> siteList = this.getHostFactory().findAll();
        List<Host> filteredSiteList = new ArrayList<>();
        if (null != siteList && !siteList.isEmpty()) {
            filteredSiteList = APILocator.getPermissionAPI().filterCollection(siteList, permissionType,
                    respectFrontendRoles, user);
        }
        return filteredSiteList;
    }

    @Override
    @CloseDBIfOpened
    public Host findSystemHost(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (null != this.systemHost) {
            return this.systemHost;
        }
        this.systemHost = this.getHostFactory().findSystemHost(user, respectFrontendRoles);
        if (null == this.systemHost) {
            this.systemHost = this.createSystemHost();
        }
        return this.systemHost;
    }

    @WrapInTransaction
    private synchronized Host createSystemHost() throws DotDataException, DotSecurityException {
        return this.getHostFactory().createSystemHost();
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
    public Optional<Future<Boolean>> delete(final Host site, final User deletingUser,
                                   final boolean respectFrontendRoles,
                                   final boolean runAsSeparatedThread) {
        Logger.info(this, ()-> "Deleting Site: " + site);
        try {
            APILocator.getPermissionAPI().checkPermission(site, PermissionLevel.PUBLISH, deletingUser);
        } catch (final DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when User '%s' tried to delete Site '%s': %s",
                    deletingUser.getUserId(), site, e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        final User user = (null != deletingUser)?deletingUser:APILocator.systemUser();
        return this.getHostFactory().delete(site, user, respectFrontendRoles, runAsSeparatedThread);
    } // delete.

    @Override
    @WrapInTransaction
    public void archive(final Host site, final User user,
                        final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException,
            DotContentletStateException {

        final Contentlet siteAsContentlet = APILocator.getContentletAPI().find
                (site.getInode(), user, respectFrontendRoles);
        // Retrieve all Sites that have the specified site as tag storage site
        final List<Host> siteList = retrieveHostsPerTagStorage(site.getIdentifier(), user);
        for (final Host siteItem : siteList) {
            if (siteItem.getIdentifier() != null && !siteItem.getIdentifier().equals(site.getIdentifier())) {
                //prevents changing tag storage for archived site.
                //the tag storage will change for all hosts which tag storage is archived site
                // Apparently this code updates all other hosts setting their own self as tag storage
                siteItem.setTagStorage(siteItem.getIdentifier());
                //So In order to avoid an exception updating a site that could be archived we're gonna tell the API to skip validation.
                siteItem.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
                save(siteItem, user, true);
            }
        }

        siteAsContentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
        APILocator.getContentletAPI().archive(siteAsContentlet, user, respectFrontendRoles);
        this.flushAllCaches(site);

        HibernateUtil.addCommitListener(() -> this.sendArchiveSiteSystemEvent(siteAsContentlet), 1000);
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
    public void unarchive(final Host site, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException,
            DotContentletStateException {

        final Contentlet siteAsContentlet = APILocator.getContentletAPI()
                .find(site.getInode(), user, respectFrontendRoles);
        APILocator.getContentletAPI().unarchive(siteAsContentlet, user, respectFrontendRoles);
        this.flushAllCaches(site);
        HibernateUtil.addCommitListener(() -> this.sendUnArchiveSiteSystemEvent(siteAsContentlet), 1000);
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

    @CloseDBIfOpened
    private synchronized Host getOrCreateDefaultHost() throws DotDataException, DotSecurityException {

        final ContentType siteContentType = hostType();
        final List<Field> fields = siteContentType.fields();
        final Optional<Field> defaultField = fields.stream().filter(field -> "isDefault".equalsIgnoreCase(field.variable())).findFirst();

        if(defaultField.isEmpty()){
            final String message = "Unable to locate field `isDefault` in the ContentType Host.";
            Logger.error(HostAPIImpl.class, message);
            throw new DotDataException(message);
        }
        final Optional<Host> defaultHostOpt = this.getHostFactory().findDefaultHost(siteContentType.inode(), defaultField.get().dbColumn());
        if (defaultHostOpt.isPresent()) {
            return defaultHostOpt.get();
        }
        // If the Default Host doesn't exist or was removed, just go ahead and re-create it
        Host defaultHost = new Host();
        defaultHost.setDefault(true);
        defaultHost.setHostname("noDefault-"  + System.currentTimeMillis());
        for (final Field field : fields) {
            if (field.required() && UtilMethods.isSet(field.defaultValue())) {
                defaultHost.setProperty(field.variable(), field.defaultValue());
            }
        }
        defaultHost.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
        defaultHost = save(defaultHost, APILocator.systemUser(), false);
        sendNotification();
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
    @Override
    public void publish(final Host site, final User user, final boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
        final Contentlet siteAsContentlet = APILocator.getContentletAPI().find(site.getInode(), user, respectFrontendRoles);
        siteAsContentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(siteAsContentlet, user, respectFrontendRoles);
        this.flushAllCaches(site);
    }

    @WrapInTransaction
    @Override
    public void unpublish(final Host site, final User user, final boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
        final Contentlet siteAsContentlet = APILocator.getContentletAPI().find(site.getInode(), user, respectFrontendRoles);
        APILocator.getContentletAPI().unpublish(siteAsContentlet, user, respectFrontendRoles);
        this.flushAllCaches(site);
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
    public Host DBSearch(String id, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        if (!UtilMethods.isSet(id)) {
            return null;
        }
        return this.getHostFactory().DBSearch(id, respectFrontendRoles);
    }

    @Override
    public void updateCache(Host host) {
        hostCache.remove(host);
        hostCache.clearAliasCache();
        hostCache.add(new Host(host));
    }

    @Override
    public List<String> parseHostAliases(Host site) {
        final List<String> ret = new ArrayList<>();
        if (!UtilMethods.isSet(site.getAliases())) {
            return ret;
        }
        return parseSiteAliases(site.getAliases());
    }

    @Override
    @SuppressWarnings("unchecked")
    @WrapInTransaction
    public void updateMenuLinks(Host workinghost,Host updatedhost) throws DotDataException {

        String workingHostName = workinghost.getHostname();
        String updatedHostName = updatedhost.getHostname();
        if(!workingHostName.equals(updatedHostName)) {
            HibernateUtil dh = new HibernateUtil(Link.class);
            List<Link> resultList = new ArrayList<>();
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
    public List<Host> retrieveHostsPerTagStorage(final String tagStorageId, final User user) {
        final List<Host> siteList = new ArrayList<>();
        try {
            final List<Host> allSites = findAll(user, true);
            if (allSites.size() > 0) {
                for (final Host site: allSites) {
                    if (site.isSystemHost()) {
                        continue;
                    }
                    if (site.getTagStorage() != null && site.getTagStorage().equals(tagStorageId)) {
                        siteList.add(site);
                    }
                }
            }
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(this, String.format("An error occurred when retrieving Site by Tag Storage '%s': %s",
                    tagStorageId, e.getMessage()), e);
        }
        return siteList;
    }

    @CloseDBIfOpened
    @Override
    public PaginatedArrayList<Host> searchByStopped(final String filter, final boolean showStopped, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles) {
        return search(filter, false, showStopped, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public PaginatedArrayList<Host> search(final String filter, final boolean showArchived, final boolean
            showStopped, final boolean showSystemHost, final int limit, final int offset, final User user, final
                                           boolean respectFrontendRoles) {
        Optional<List<Host>> siteListOpt;
        if (!showStopped && !showArchived) {
            // Return live Sites
            siteListOpt = this.getHostFactory()
                    .findLiveSites(filter, limit, offset, showSystemHost, user, respectFrontendRoles);
            if (siteListOpt.isPresent()) {
                return convertToSitePaginatedList(siteListOpt.get());
            }
        }
        if (showStopped && !showArchived) {
            // Return stopped Sites, which should not include archived Sites, but should include live sites.
            siteListOpt = this.getHostFactory()
                    .findLiveAndStopped(filter, limit, offset, showSystemHost, user, respectFrontendRoles);
            if (siteListOpt.isPresent()) {
                return convertToSitePaginatedList(siteListOpt.get());
            }
        }
        if (showStopped && showArchived) {
            // Return archived Sites
            siteListOpt = this.getHostFactory()
                    .findArchivedSites(filter, limit, offset, showSystemHost, user, respectFrontendRoles);
            if (siteListOpt.isPresent()) {
                return convertToSitePaginatedList(siteListOpt.get());
            }
        }
        return new PaginatedArrayList<>();
    }

    @CloseDBIfOpened
    @Override
    public PaginatedArrayList<Host> search(String filter, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles){
        return search(filter, false, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public PaginatedArrayList<Host> search(final String filter, boolean showArchived, boolean showSystemHost, int
            limit, int offset, User user, boolean respectFrontendRoles) {
        final Optional<List<Host>> siteListOpt = showArchived ? this.getHostFactory()
                .findArchivedSites(filter, limit, offset, showSystemHost, user, respectFrontendRoles) :
                this.getHostFactory().findLiveSites(filter, limit, offset, showSystemHost, user, respectFrontendRoles);
        if (siteListOpt.isPresent()) {
            return convertToSitePaginatedList(siteListOpt.get());
        }
        return new PaginatedArrayList<>();
    }

    @CloseDBIfOpened
    @Override
    public long count(final User user, final boolean respectFrontendRoles) {
        try {
            return this.getHostFactory().count();
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when User '%s' attempted to get the total number " +
                    "of Sites: %s", user.getUserId(), e.getMessage());
            Logger.error(HostAPIImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Utility method used to convert a list of Sites into paginated Site objects.
     *
     * @param list The list of {@link Host} objects.
     *
     * @return The paginated list of {@link Host} objects.
     */
    private PaginatedArrayList<Host> convertToSitePaginatedList(final List<Host> list) {
        final PaginatedArrayList<Host> paginatedSites = new PaginatedArrayList<>();
        paginatedSites.addAll(list);
        paginatedSites.setTotalResults(list.size());
        return paginatedSites;
    }

    @Override
    public void flushAll() {
        hostCache.clearCache();
    }

    @Override
    public void flush(Host host) {
        hostCache.remove(host);
    }

    /**
     * Utility method that completely clears the Site Cache Region across all nodes in your environment.
     *
     * @param site The {@link Host} object that was modified, which triggers a complete flush of the Site Cache Region.
     */
    private void flushAllCaches(final Host site) {
        this.hostCache.remove(site);
        this.hostCache.clearCache();
    }

}
