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
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.web.UpdateContainersPathsJob;
import com.dotmarketing.portlets.contentlet.business.web.UpdatePageTemplatePathJob;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;
import static com.dotmarketing.db.DbConnectionFactory.getDBTrue;

/**
 * This API allows developers to access information related to Sites objects in your dotCMS content repository.
 * <p>
 * A single dotCMS instance may manage multiple different web sites. Each “site” is actually a separate website, but all
 * sites are managed and the content for all sites is served from a single dotCMS instance. A single dotCMS server can
 * manage literally hundreds of sites. </p>
 *
 * @author jtesser
 * @author david torres
 */
public class HostAPIImpl implements HostAPI, Flushable<Host> {

    private ContentletFactory contentletFactory = FactoryLocator.getContentletFactory();
    private HostCache hostCache = CacheLocator.getHostCache();
    private Host systemHost;
    private final SystemEventsAPI systemEventsAPI;
    private final DotConcurrentFactory concurrentFactory = DotConcurrentFactory.getInstance();
    private LanguageAPI languageAPI;

    public HostAPIImpl() {
        this(APILocator.getSystemEventsAPI(),APILocator.getLanguageAPI());
    }

    @VisibleForTesting
    HostAPIImpl(final SystemEventsAPI systemEventsAPI, final LanguageAPI languageAPI) {
        this.systemEventsAPI = systemEventsAPI;
        this.languageAPI = languageAPI;
    }

    private ContentType hostType() throws DotDataException, DotSecurityException{
        return APILocator.getContentTypeAPI(APILocator.systemUser()).find(Host.HOST_VELOCITY_VAR_NAME);
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
        Host host = hostCache.getHostByAlias(serverName);
        User systemUser = APILocator.systemUser();

        if(host == null){

            try {
                final Optional<Host> optional = resolveHostNameWithoutDefault(serverName, systemUser, respectFrontendRoles);
                host = optional.isPresent() ? optional.get() : findDefaultHost(systemUser, respectFrontendRoles);
            } catch (Exception e) {
                host = findDefaultHost(systemUser, respectFrontendRoles);
            }

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

            if(host != null){
                hostCache.addHostAlias(serverName, host);
            }
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
        Host site = hostCache.get(siteName);
        if (null == site) {
            final DotConnect dc = new DotConnect();
            final StringBuilder sqlQuery = new StringBuilder().append("SELECT c.inode FROM contentlet c ");
            sqlQuery.append("INNER JOIN identifier i ");
            sqlQuery.append("ON c.identifier = i.id AND i.asset_subtype = ? ");
            sqlQuery.append("INNER JOIN contentlet_version_info cvi ");
            sqlQuery.append("ON c.inode = cvi.working_inode ");
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                sqlQuery.append("WHERE LOWER(contentlet_as_json->'fields'->'hostName'->>'value') = ?");
            } else {
                sqlQuery.append("WHERE LOWER(c.text1) = ?");
            }
            dc.setSQL(sqlQuery.toString());
            dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
            dc.addParam(siteName.toLowerCase());
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
                    return null;
                }
                if (dbResults.size() > 1) {
                    // This situation should not happen at all
                    final StringBuilder warningMsg = new StringBuilder().append("ERROR: ").append(dbResults.size())
                            .append(" Sites have the same name '").append(siteName).append("':\n");
                    for (final Map<String, String> siteInfo : dbResults) {
                        warningMsg.append("-> Inode = ").append(siteInfo.get("inode")).append("\n");
                    }
                    Logger.fatal(this, warningMsg.toString());
                }
                final Contentlet siteAsContentlet = APILocator.getContentletAPI().find(dbResults.get(0).get("inode"),
                        user, respectFrontendRoles);
                site = new Host(siteAsContentlet);
                this.hostCache.add(site);
            } catch (final Exception e) {
                throw new DotRuntimeException(String.format("An error occurred when retrieving non-default Site '%s': %s",
                        siteName, e.getMessage()), e);
            }
        }
        return site;
    }

    @Override
    public Host findByAlias(final String alias, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host site = this.hostCache.getHostByAlias(alias);
        if (null == site) {
            final DotConnect dc = new DotConnect();
            final StringBuilder sqlQuery = new StringBuilder().append("SELECT c.inode, ");
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                sqlQuery.append("c.contentlet_as_json->'fields'->'aliases'->>'value' ");
            } else {
                sqlQuery.append("c.text_area1 ");
            }
            sqlQuery.append("AS data FROM contentlet c ");
            sqlQuery.append("INNER JOIN identifier i ");
            sqlQuery.append("ON c.identifier = i.id AND i.asset_subtype = ? ");
            sqlQuery.append("INNER JOIN contentlet_version_info cvi ");
            sqlQuery.append("ON c.inode = cvi.working_inode ");
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                sqlQuery.append("WHERE LOWER(c.contentlet_as_json->'fields'->'aliases'->>'value') LIKE ?");
            } else {
                sqlQuery.append("WHERE LOWER(c.text_area4) LIKE ?");
            }
            dc.setSQL(sqlQuery.toString());
            dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
            dc.addParam("%" + alias.toLowerCase() + "%");
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
                    return null;
                }
                if (dbResults.size() == 1) {
                    final Set<String> siteAliases = new HashSet<>(parseSiteAliases(dbResults.get(0).get("data")));
                    if (siteAliases.contains(alias)) {
                        site = new Host(APILocator.getContentletAPI().find(dbResults.get(0).get("inode"), user,
                                respectFrontendRoles));
                    }
                } else {
                    final List<Contentlet> siteAsContentletList = new ArrayList<>();
                    for (final Map<String, String> siteInfo : dbResults) {
                        final Set<String> siteAliases = new HashSet<>(parseSiteAliases(siteInfo.get("data")));
                        if (siteAliases.contains(alias)) {
                            siteAsContentletList.add(APILocator.getContentletAPI().find(siteInfo.get("inode"), user, respectFrontendRoles));
                        }
                    }
                    if (siteAsContentletList.size() == 1) {
                        site = new Host(siteAsContentletList.get(0));
                    } else {
                        for (final Contentlet siteAsContentlet : siteAsContentletList) {
                            if (Boolean.class.cast(siteAsContentlet.get(Host.IS_DEFAULT_KEY))) {
                                site = new Host(siteAsContentlet);
                                break;
                            }
                        }
                        if (null == site) {
                            final StringBuilder warningMsg = new StringBuilder().append("ERROR: ").append(siteAsContentletList.size())
                                    .append(" Sites have the same alias '").append(alias).append("':\n");
                            for (final Contentlet siteAsContentlet : siteAsContentletList) {
                                warningMsg.append("-> Inode = ").append(siteAsContentlet.getInode()).append("\n");
                            }
                            Logger.fatal(this, warningMsg.toString());
                            site = new Host(siteAsContentletList.get(0));
                        }
                    }
                }
                this.hostCache.add(site);
                this.hostCache.addHostAlias(alias, site);
            } catch (final Exception e) {
                throw new DotRuntimeException(String.format("An error occurred when retrieving Site with alias '%s': " +
                        "%s", alias, e.getMessage()), e);
            }
        }
        return site;
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

        Host site  = hostCache.get(id);

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
    @CloseDBIfOpened
    public List<Host> findAllFromDB(final User user, final boolean respectFrontendRoles) throws DotDataException,
            DotSecurityException {
        return this.findPaginatedSitesFromDB(user, 0, 0, null, respectFrontendRoles);
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
        final StringBuffer sqlQuery = new StringBuffer();
        sqlQuery.append("SELECT cvi.working_inode FROM contentlet_version_info cvi, identifier i ");
        sqlQuery.append("WHERE i.asset_subtype = ? AND cvi.identifier = i.id AND i.id <> ? ");
        final String sanitizedSortBy = SQLUtil.sanitizeSortBy(sortBy);
        if (UtilMethods.isSet(sanitizedSortBy)) {
            sqlQuery.append("ORDER BY ? ");
        }
        final DotConnect dc = new DotConnect();
        dc.setSQL(sqlQuery.toString());
        dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
        dc.addParam(Host.SYSTEM_HOST);
        if (UtilMethods.isSet(sanitizedSortBy)) {
            dc.addParam(sanitizedSortBy);
        }
        if (limit > 0) {
            dc.setMaxRows(limit);
        }
        if (offset > 0) {
            dc.setStartRow(offset);
        }
        final List<Map<String, String>> dbResults = dc.loadResults();
        final List<Host> siteList = new ArrayList<>();
        for (final Map<String, String> siteInfo : dbResults) {
            final Contentlet contentlet = APILocator.getContentletAPI().find(siteInfo.get("working_inode"), user,
                    respectFrontendRoles);
            siteList.add(new Host(contentlet));
        }

        List<Host> collect = dbResults.stream().map(data -> {
            try {
                final Contentlet contentlet = APILocator.getContentletAPI().find(data.get("working_inode"), user,
                        respectFrontendRoles);
                return new Host(contentlet);
            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Contentlet with Inode '%s' could not be retrieved from Content API: " +
                        "%s", data.get("working_inode"), e.getMessage()));
                return null;
            }
        }).collect(Collectors.toList());

        return collect;
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
    public List<Host> getHostsWithPermission(int permissionType, boolean includeArchived, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        List<Host> siteList = new ArrayList<>();
        List<Contentlet> siteAsContentletList = new ArrayList<>();
        String sql = "SELECT working_inode FROM contentlet_version_info cvi, identifier i WHERE i" +
                ".asset_subtype = ? AND cvi.identifier = i.id ";
        if (includeArchived) {
            sql += "AND cvi.live_inode IS NULL AND cvi.deleted IS TRUE ";
        }
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
        final List<Map<String,String>> dbResults = dc.loadResults();
        if (!UtilMethods.isSet(dbResults)) {
            return siteList;
        }
        for (final Map<String, String> siteInfo : dbResults) {
            final Contentlet siteAsContentlet = APILocator.getContentletAPI().find(siteInfo.get("working_inode"),
                    APILocator.systemUser(), respectFrontendRoles);
            siteAsContentletList.add(siteAsContentlet);
        }
        siteAsContentletList = APILocator.getPermissionAPI().filterCollection(siteAsContentletList, permissionType,
                respectFrontendRoles, user);
        siteList = this.convertToHostList(siteAsContentletList);
        if (!includeArchived) {
            siteList = siteList.stream().filter(site -> {
                try {
                    return !site.isArchived();
                } catch (DotDataException | DotSecurityException e) {
                    Logger.warn(this, String.format("An error occurred when checking permissions on Site '%s': %s",
                            site, e.getMessage()));
                    return false;
                }
            }).collect(Collectors.toList());
            return siteList;
        }
        return siteList;
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
    public Optional<Future<Boolean>> delete(final Host site, final User deletingUser,
                                   final boolean respectFrontendRoles,
                                   final boolean runAsSeparatedThread) {

        Optional<Future<Boolean>> future = Optional.empty();
        try {
            Logger.info(this, ()-> "Deleting Site: " + site);
            APILocator.getPermissionAPI().checkPermission(site, PermissionLevel.PUBLISH, deletingUser);
        } catch (final DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when User '%s' tried to delete Site '%s': %s",
                    deletingUser.getUserId(), site, e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
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
                                new I18NMessage("notifications_host_deletion_error", site.getHostname(), e.getMessage()),
                                null, // no actions
                                NotificationLevel.ERROR,
                                NotificationType.GENERIC,
                                user.getUserId(),
                                user.getLocale()
                        );

                    } catch (final DotDataException e1) {
                        Logger.error(HostAPIImpl.class, String.format("An error occurred when saving Site Deletion " +
                                "Notification for site '%s': %s", site, e.getMessage()), e);
                    }
                    final String errorMsg = String.format("An error occurred when User '%s' tried to delete Site " +
                            "'%s': %s", deletingUser.getUserId(), site, e.getMessage());
                    Logger.error(HostAPIImpl.class, errorMsg, e);
                    throw new DotRuntimeException(errorMsg, e);
                }

                return Boolean.TRUE;
            }

            private void generateNotification() {
                try {

                    APILocator.getNotificationAPI().generateNotification(
                            new I18NMessage("message.host.delete.title"), // title = Host Notification
                            new I18NMessage("message.host.delete",
                                    "Site deleted: " + site, site.getHostname()),
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
                if(site != null){
                    hostCache.remove(site);
                }

                final DotConnect dc = new DotConnect();

                // Remove Links
                MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
                List<Link> links = linkAPI.findLinks(user, true, null, site.getIdentifier(), null, null, null, 0, -1, null);
                for (Link link : links) {
                    linkAPI.delete(link, user, respectFrontendRoles);
                }

                // Remove Contentlet
                ContentletAPI contentAPI = APILocator.getContentletAPI();
                contentAPI.deleteByHost(site, APILocator.systemUser(), respectFrontendRoles);

                // Remove Folders
                FolderAPI folderAPI = APILocator.getFolderAPI();
                List<Folder> folders = folderAPI.findFoldersByHost(site, user, respectFrontendRoles);
                for (Folder folder : folders) {
                    folderAPI.delete(folder, user, respectFrontendRoles);
                }

                // Remove Templates
                TemplateAPI templateAPI = APILocator.getTemplateAPI();
                List<Template> templates = templateAPI.findTemplatesAssignedTo(site, true);
                for (Template template : templates) {
                    dc.setSQL("delete from template_containers where template_id = ?");
                    dc.addParam(template.getIdentifier());
                    dc.loadResult();

                    templateAPI.delete(template, user, respectFrontendRoles);
                }

                // Remove Containers
                ContainerAPI containerAPI = APILocator.getContainerAPI();
                List<Container> containers = containerAPI.findContainers(user, true, null, site.getIdentifier(), null, null, null, 0, -1, null);
                for (Container container : containers) {
                    containerAPI.delete(container, user, respectFrontendRoles);
                }

                // Remove Structures
                List<ContentType> types = APILocator.getContentTypeAPI(user, respectFrontendRoles).search(" host = '" + site.getIdentifier() + "'");

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
                dc.addParam(site.getIdentifier());
                dc.loadResult();

                Inode.Type[] assets = {Inode.Type.CONTAINERS, Inode.Type.TEMPLATE, Inode.Type.LINKS};
                for(Inode.Type asset : assets) {
                    dc.setSQL("select inode from "+asset.getTableName()+" where exists (select * from identifier where host_inode=? and id="+asset.getTableName()+".identifier)");
                    dc.addParam(site.getIdentifier());
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
                APILocator.getTagAPI().deleteTagsByHostId(site.getIdentifier());

                // Double-check that ALL contentlets are effectively removed  
                // before using dotConnect to kill bad identifiers
                List<Contentlet> remainingContenlets = contentAPI
                        .findContentletsByHost(site, user, respectFrontendRoles);
                if (remainingContenlets != null
                        && remainingContenlets.size() > 0) {
                    contentAPI.deleteByHost(site, user, respectFrontendRoles);
                }
                
                // kill bad identifiers pointing to the host
                dc.setSQL("delete from identifier where host_inode=?");
                dc.addParam(site.getIdentifier());
                dc.loadResult();

                // Remove Host
                Contentlet c = contentAPI.find(site.getInode(), user, respectFrontendRoles);
                contentAPI.delete(c, user, respectFrontendRoles);

                try {
                    APILocator.getAppsAPI().removeSecretsForSite(site, APILocator.systemUser());
                } catch (final Exception e) {
                    Logger.warn(HostAPIImpl.class, String.format("An error occurred when removing secrets for site " +
                            "'%s': %s", site, e.getMessage()), e);
                }
                flushAllCaches(site);
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
        site.setModDate(new Date ());
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
        site.setModDate(new Date ());
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

        if(!defaultField.isPresent()){
            final String message = "Unable to locate field `isDefault` in the ContentType Host.";
            Logger.error(HostAPIImpl.class, message);
            throw new DotDataException(message);
        }

        final DotConnect dotConnect = new DotConnect();
        String inode = null;

        if(APILocator.getContentletJsonAPI().isPersistContentAsJson()){

          String sql = null;

          if(DbConnectionFactory.isPostgres()) {
                sql = "SELECT cvi.working_inode\n"
                        + "  FROM contentlet_version_info cvi join contentlet c on (c.inode = cvi.working_inode) \n"
                        + "  WHERE c.contentlet_as_json @> '{ \"fields\":{\"isDefault\":{ \"value\":true }} }' \n"
                        + "  and c.structure_inode = ?";
          }
          if(DbConnectionFactory.isMsSql()){
                   sql = "SELECT cvi.working_inode\n"
                        + "  FROM contentlet_version_info cvi join contentlet c on (c.inode = cvi.working_inode) \n"
                        + "  WHERE JSON_VALUE(c.contentlet_as_json, '$.fields.isDefault.value') = 'true'  \n"
                        + "  and c.structure_inode = ?";
          }
          if(null == sql) {
              throw new IllegalStateException("Unable to determine what db with json support we're running on!");
          }
             dotConnect.setSQL(sql);
             dotConnect.addParam(siteContentType.inode());
             inode = Try.of(()->dotConnect.getString("working_inode")).onFailure(throwable -> {
                 Logger.warnAndDebug(HostAPIImpl.class,"An Error occurred while fetching the default host. ", throwable);
             }).getOrNull();
        }

       if(UtilMethods.isNotSet(inode)) {
           dotConnect
                   .setSQL("select working_inode from contentlet_version_info join contentlet on (contentlet.inode = contentlet_version_info.working_inode) "
                           + " where " + defaultField.get().dbColumn()  + " = ? and structure_inode =?");
           dotConnect.addParam(true);
           dotConnect.addParam(siteContentType.inode());
           inode = dotConnect.getString("working_inode");
       }

        Host defaultHost;

        if(UtilMethods.isSet(inode)) {

            defaultHost = new Host(APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false));
            hostCache.add(defaultHost);
        } else {
            defaultHost = new Host();
            defaultHost.setDefault(true);
            defaultHost.setHostname("noDefault-"  + System.currentTimeMillis());

            for(final Field field : fields) {

                if(field.required() && UtilMethods.isSet(field.defaultValue())) {
                    defaultHost.setProperty(field.variable(), field.defaultValue());
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
            systemHost.setLanguageId(languageAPI.getDefaultLanguage().getId());
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
            this.systemHost = DBSearch(systemHostId, systemUser, false);
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

        Host host = null;

        final List<ContentletVersionInfo> verInfos = APILocator.getVersionableAPI()
                .findContentletVersionInfos(id);
        if (!verInfos.isEmpty()) {
            final Language defaultLang = languageAPI.getDefaultLanguage();

            final ContentletVersionInfo versionInfo = verInfos.stream()
                    .filter(contentletVersionInfo -> contentletVersionInfo.getLang() == defaultLang
                            .getId()).findFirst().orElseGet(() -> {
                        Logger.warn(HostAPIImpl.class,
                                String.format(
                                        "Unable to find ContentletVersionInfo for site with id [%s] using default language [%s]. fallback to first entry found.",
                                        id, defaultLang.getId()));
                        return verInfos.get(0);
                    });

            final User systemUser = APILocator.systemUser();
            final String hostInode = versionInfo.getWorkingInode();
            final Contentlet cont = APILocator.getContentletAPI()
                    .find(hostInode, systemUser, respectFrontendRoles);
            final ContentType type = APILocator.getContentTypeAPI(systemUser, respectFrontendRoles)
                    .find(Host.HOST_VELOCITY_VAR_NAME);
            if (cont.getStructureInode().equals(type.inode())) {
                host = new Host(cont);
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

    @Override
    public PaginatedArrayList<Host> searchByStopped(final String filter, final boolean showStopped, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles) {
        final String condition = showStopped ? "AND cvi.live_inode IS NULL " : "AND cvi.live_inode IS NOT NULL ";
        return search(filter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public PaginatedArrayList<Host> search(final String filter, final boolean showArchived, final boolean
            showStopped, final boolean showSystemHost, final int limit, final int offset, final User user, final
                                           boolean respectFrontendRoles) {
        final String showArchivedCondition = showArchived ? "cvi.deleted = " + getDBTrue() : "cvi.deleted = " +
                getDBFalse();
        final String showStoppedCondition = showStopped ? "cvi.live_inode IS NULL" : "cvi.live_inode IS NOT NULL";
        final String condition = "AND " + showArchivedCondition + " AND " + showStoppedCondition + " ";
        return search(filter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public PaginatedArrayList<Host> search(String filter, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles){
        return search(filter, StringUtils.EMPTY, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public PaginatedArrayList<Host> search(final String filter, boolean showArchived, boolean showSystemHost, int
            limit, int offset, User user, boolean respectFrontendRoles) {
        String showArchivedCondition = "AND cvi.deleted = ";
        showArchivedCondition += showArchived ? getDBTrue() + " " : getDBFalse() + " ";
        return search(filter, showArchivedCondition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    /**
     * Returns the list of Sites – with pagination capabilities – that match the specified search criteria. This method
     * allows users to specify three search parameters:
     * <ol>
     *  <li>{@code filter}: Finds Sites whose name starts with the specified String.</li>
     *  <li>{@code condition}: Determines if live, stopped, or archived Sites are returned in the result set.</li>
     *  <li>{@code showSystemHost}: Determines whether the System Host must be returned or not.</li>
     * </ol>
     *
     * @param filter               The initial part or full name of the Site you need to look up. If not required, set
     *                             as {@code null} or empty.
     * @param condition            The status of the Site determined via SQL conditions.
     * @param showSystemHost       If the System Host object must be returned, set to {@code true}. Otherwise, se to
     *                             {@code false}.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes.  If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the specified search criteria.
     */
    @CloseDBIfOpened
    private PaginatedArrayList<Host> search(final String filter, final String condition, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles) {
        Logger.debug(this, () -> "");
        final DotConnect dc = new DotConnect();
        final StringBuilder sqlQuery = new StringBuilder().append("SELECT c.inode FROM contentlet c ");
        sqlQuery.append("INNER JOIN identifier i ");
        sqlQuery.append("ON c.identifier = i.id AND i.asset_subtype = ? ");
        sqlQuery.append("INNER JOIN contentlet_version_info cvi ");
        sqlQuery.append("ON c.inode = cvi.working_inode ");
        sqlQuery.append("WHERE cvi.identifier = i.id ");
        if (UtilMethods.isSet(filter)) {
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                sqlQuery.append("AND LOWER(contentlet_as_json->'fields'->'hostName'->>'value') LIKE ? ");
            } else {
                sqlQuery.append("AND LOWER(c.text1) LIKE ? ");
            }
        }
        if (UtilMethods.isSet(condition)) {
            sqlQuery.append(condition);
        }
        if (!showSystemHost) {
            sqlQuery.append("AND i.id <> ?");
        }
        dc.setSQL(sqlQuery.toString());
        dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
        if (UtilMethods.isSet(filter)) {
            dc.addParam("%" + filter.trim() + "%");
        }
        if (!showSystemHost) {
            dc.addParam(Host.SYSTEM_HOST);
        }
        if (limit > 0) {
            dc.setMaxRows(limit);
        }
        if (offset > 0) {
            dc.setStartRow(offset);
        }
        try {
            final List<Map<String, String>> dbResults = dc.loadResults();
            if (dbResults.isEmpty()) {
                return new PaginatedArrayList<>();
            }
            List<Contentlet> siteAsContentletList = new ArrayList<>();
            for (final Map<String, String> siteInfo : dbResults) {
                final Contentlet siteAsContentlet = APILocator.getContentletAPI().find(siteInfo.get("inode"), user,
                        respectFrontendRoles);
                siteAsContentletList.add(siteAsContentlet);
            }
            siteAsContentletList = APILocator.getPermissionAPI().filterCollection(siteAsContentletList, PermissionAPI
                    .PERMISSION_READ, respectFrontendRoles, user);
            return convertToSitePaginatedArrayList(siteAsContentletList);
        } catch (final Exception e) {
            Logger.error(HostAPIImpl.class, String.format("An error occurred when searching for Sites based on the " +
                    "following criteria: filter[ %s ], condition[ %s ], showSystemHost [ %s ]: %s", filter,
                    condition, showSystemHost, e.getMessage()), e);
            throw new DotRuntimeException(String.format("An error occurred when searching for Sites: %s", e.getMessage
                    ()), e);
        }
    }

    @CloseDBIfOpened
    @Override
    public long count(final User user, final boolean respectFrontendRoles) {
        final DotConnect dc = new DotConnect();
        final StringBuilder sqlQuery = new StringBuilder().append("SELECT COUNT(cvi.working_inode) ");
        sqlQuery.append("FROM contentlet_version_info cvi, identifier i ");
        sqlQuery.append("WHERE i.asset_subtype = ? AND cvi.identifier = i.id ");
        dc.setSQL(sqlQuery.toString());
        dc.addParam(Host.HOST_VELOCITY_VAR_NAME);
        try {
            final List<Map<String, String>> dbResults = dc.loadResults();
            final String total = dbResults.get(0).get("count");
            return ConversionUtils.toLong(total, 0L);
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when User '%s' attempted to get the total number " +
                    "of Sites: %s", user.getUserId(), e.getMessage());
            Logger.error(HostAPIImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Utility method used to convert a list of Sites as Contentlets into paginated Site objects.
     *
     * @param list The list of {@link Contentlet} objects representing a Site.
     *
     * @return The paginated list of {@link Host} objects.
     */
    private PaginatedArrayList<Host> convertToSitePaginatedArrayList(final List<Contentlet> list) {
		final PaginatedArrayList<Host> paginatedSites = new PaginatedArrayList<>();
		paginatedSites.addAll(list.stream().map(content -> new Host(content)).collect(Collectors.toList()));
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
