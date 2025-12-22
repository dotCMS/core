package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;
import static com.dotmarketing.db.DbConnectionFactory.getDBTrue;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactory;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

/**
 * SQL-based implementation class for {@link HostFactory}.
 *
 * @author Jose Castro
 * @version 22.4
 * @since Mar 15, 2022
 */
public class HostFactoryImpl implements HostFactory {

    private final HostCache siteCache;
    private ContentletFactory contentFactory;
    private HostVariableFactory siteVariableFactory;
    private ContentletAPI contentletAPI;

    private final String WHERE = " WHERE ";
    private final String AND = " AND ";
    private final String OR = " OR ";
    private final String ORDER_BY = " ORDER BY ? ";

    private static final String SELECT_SYSTEM_HOST = "SELECT id FROM identifier WHERE id = '"+ Host.SYSTEM_HOST+"' ";

    private static final String FROM_JOINED_TABLES = "INNER JOIN identifier i " +
            "ON c.identifier = i.id " +
            "INNER JOIN contentlet_version_info cvi " +
            "ON c.inode = cvi.working_inode " +
            "LEFT JOIN contentlet clive " +
            "ON clive.inode = cvi.live_inode " +
            "WHERE c.structure_inode = ? ";

    private static final String SELECT_SITE_INODE =
            "SELECT c.inode, cvi.live_inode FROM contentlet c " +
            FROM_JOINED_TABLES;
    private static final String SELECT_SITE_INODE_AND_ALIASES =
            "SELECT c.inode, cvi.live_inode, " +
            "%s AS aliases, %s AS live_aliases FROM contentlet c " +
            FROM_JOINED_TABLES;

    private static final String POSTGRES_ALIASES_COLUMN = "%s." +
            ContentletJsonAPI.CONTENTLET_AS_JSON +
            "->'fields'->'" + Host.ALIASES_KEY + "'->>'value' ";

    private static final String MSSQL_ALIASES_COLUMN = "JSON_VALUE(%s." +
            ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + Host.ALIASES_KEY + ".value') ";
    private static final String ALIASES_COLUMN = "%s.text_area1";

    private static final String SITE_NAME_LIKE = "%s ILIKE ? ";

    private static final String SITE_NAME_EQUALS ="%s ILIKE ? ";

    private static final String POSTGRES_SITENAME_COLUMN = "%s." +
            ContentletJsonAPI.CONTENTLET_AS_JSON +
            "->'fields'->'" + Host.HOST_NAME_KEY + "'->>'value' ";

    private static final String MSSQL_SITENAME_COLUMN = "JSON_VALUE(%s." +
            ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + Host.HOST_NAME_KEY + ".value')" +
            " ";

    private static final String SITENAME_COLUMN = "%s.text1";

    private static final String ALIAS_LIKE = "%s ILIKE ? ";

    private static final String EXCLUDE_SYSTEM_HOST = "i.id <> '" + Host
            .SYSTEM_HOST +
            "' ";

    private static final String SITE_IS_LIVE = "cvi.live_inode IS NOT NULL";
    @VisibleForTesting
    protected static final String SITE_IS_LIVE_OR_STOPPED = "cvi.deleted = false";
    private static final String SITE_IS_STOPPED = "cvi.live_inode IS NULL AND cvi" +
            ".deleted = " +
            getDBFalse();

    private static final String SITE_IS_STOPPED_OR_ARCHIVED = "cvi.live_inode IS NULL";

    private static final String SITE_IS_ARCHIVED = "cvi.live_inode IS NULL AND cvi" +
            ".deleted = " +
            getDBTrue();

    private static final String SELECT_SITE_COUNT = "SELECT COUNT(cvi.working_inode) " +
            "FROM contentlet_version_info cvi, identifier i, contentlet c " +
            "WHERE c.structure_inode = ? AND c.identifier = i.id AND cvi.identifier = i.id ";

    // query that Exact matches should be at the top of the search results.
    private static final String PRIORITIZE_EXACT_MATCHES =
            " ORDER BY length(%s), %s  ";

    /**
     * Default class constructor.
     */
    public HostFactoryImpl() {
        this.siteCache = CacheLocator.getHostCache();
    }

    /**
     * Lazy initialization of the Contentlet Factory service. This helps prevent startup issues when several factories
     * or APIs are initialized during the initialization phase of the Host Factory.
     *
     * @return An instance of the {@link ContentletFactory} service.
     */
    protected ContentletFactory getContentletFactory() {
        if (null == this.contentFactory) {
            this.contentFactory = FactoryLocator.getContentletFactory();
        }
        return this.contentFactory;
    }

    /**
     * Lazy initialization of the Host Variable Factory service. This helps prevent startup issues
     * when several factories or APIs are initialized during the initialization phase of the Host
     * Factory.
     *
     * @return An instance of the {@link HostVariableFactory} service.
     */
    protected HostVariableFactory getHostVariableFactory() {
        if (null == this.siteVariableFactory) {
            this.siteVariableFactory = FactoryLocator.getHostVariableFactory();
        }
        return this.siteVariableFactory;
    }

    /**
     * Lazy initialization of the Contentlet API service. This helps prevent startup issues when several factories or
     * APIs are initialized during the initialization phase of the Host Factory.
     *
     * @return An instance of the {@link ContentletAPI} service.
     */
    protected ContentletAPI getContentletAPI() {
        if (null == this.contentletAPI) {
            this.contentletAPI = APILocator.getContentletAPI();
        }
        return this.contentletAPI;
    }

    /**
     * Returns the inode (structure_inode) of the Host content type.
     * This is used for queries that filter contentlets by content type.
     *
     * @return The inode of the Host content type.
     * @throws DotRuntimeException if the Host content type cannot be found.
     */
    private String getHostContentTypeInode() {
        try {
            final ContentType hostContentType = APILocator.getContentTypeAPI(APILocator.systemUser(), false)
                    .find(Host.HOST_VELOCITY_VAR_NAME);
            if (hostContentType == null) {
                throw new DotRuntimeException("Host content type not found");
            }
            return hostContentType.inode();
        } catch (final DotDataException | DotSecurityException e) {
            throw new DotRuntimeException("Error retrieving Host content type: " + e.getMessage(), e);
        }
    }

    @Override
    public Host bySiteName(final String siteName, boolean retrieveLiveVersion) {
        Host site;
        final Host cachedSiteByName = siteCache.getByName(siteName, retrieveLiveVersion);
        if (UtilMethods.isSet(() -> cachedSiteByName.getIdentifier())) {
            if (HostCache.CACHE_404_HOST.equals(cachedSiteByName.getIdentifier())) {
                return null;
            }
            site = cachedSiteByName;
        } else {
            final DotConnect dc = new DotConnect();
            final String hostTypeInode = getHostContentTypeInode();
            final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE)
                .append(AND)
                .append(getSiteNameOrAliasColumn(SITE_NAME_EQUALS, true, "c"))
                .append(OR)
                .append(getSiteNameOrAliasColumn(SITE_NAME_EQUALS, true, "clive"));
            dc.setSQL(sqlQuery.toString());
            dc.addParam(hostTypeInode);
            dc.addParam(siteName);
            dc.addParam(siteName);
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
                    // Check if siteName is a UUID and try to find by ID
                    final Host siteById = findSiteByIdIfUUID(siteName, retrieveLiveVersion);
                    if (siteById != null) {
                        return siteById;
                    }
                    // Site not found by name or ID, add to 404 cache
                    siteCache.add404HostByName(siteName);
                    return null;
                }
                final String siteInode = dbResults.get(0).get("inode");
                if (dbResults.size() > 1) {
                    // This situation should NOT happen at all
                    final StringBuilder warningMsg = new StringBuilder().append("ERROR: ").append(dbResults.size())
                            .append(" Sites have the same name '").append(siteName).append("':\n");
                    for (final Map<String, String> siteInfo : dbResults) {
                        warningMsg.append("-> Inode = ").append(siteInfo.get("inode")).append("\n");
                    }
                    warningMsg.append("Defaulting to Site '").append(siteInode).append("'");
                    Logger.fatal(this, warningMsg.toString());
                }
                final Contentlet siteAsContentlet = this.contentFactory.find(siteInode);
                site = new Host(siteAsContentlet);
                this.siteCache.add(site);

                if (UtilMethods.isSet(() -> dbResults.get(0).get("live_inode"))) {
                    final String liveInode = dbResults.get(0).get("live_inode");
                    if (!siteInode.equals(liveInode)) {
                        final Host liveHost = new Host(this.contentFactory.find(
                                dbResults.get(0).get("live_inode")));
                        this.siteCache.add(liveHost);
                        if (retrieveLiveVersion) {
                            site = liveHost;
                        }
                    }
                }
            } catch (final Exception e) {
                final String errorMsg = String.format("An error occurred when retrieving Site by name '%s': %s",
                        siteName, e.getMessage());
                throw new DotRuntimeException(errorMsg, e);
            }
        }
        return site;
    }

    @Override
    public Host byAlias(String alias, boolean retrieveLiveVersion) {
        Host site = null;
        Host cachedSiteByAlias = this.siteCache.getHostByAlias(alias, retrieveLiveVersion);
        if (UtilMethods.isSet(() -> cachedSiteByAlias.getIdentifier())) {
            if (HostCache.CACHE_404_HOST.equals(cachedSiteByAlias.getIdentifier())) {
                return null;
            }
            site = cachedSiteByAlias;
        } else {
            final DotConnect dc = new DotConnect();
            final String hostTypeInode = getHostContentTypeInode();
            final StringBuilder sqlQuery = new StringBuilder()
                    .append(getSiteNameOrAliasColumn(SELECT_SITE_INODE_AND_ALIASES, false, "c", "clive"))
                    .append(AND)
                    .append(getSiteNameOrAliasColumn(ALIAS_LIKE, false, "c"))
                    .append(OR)
                    .append(getSiteNameOrAliasColumn(ALIAS_LIKE, false, "clive"));
            dc.setSQL(sqlQuery.toString());
            dc.addParam(hostTypeInode);
            dc.addParam("%" + alias + "%");
            dc.addParam("%" + alias + "%");
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
                    siteCache.addHostAlias(alias, HostCache.cache404Contentlet);
                    return null;
                }

                final List<Contentlet> siteAsContentletList = new ArrayList<>();
                final Map<String, Contentlet>   liveSiteMap = new HashMap<>();
                for (final Map<String, String> siteInfo : dbResults) {
                    final Set<String> siteAliases = new HashSet<>(parseSiteAliases(siteInfo.get("aliases")));
                    if (UtilMethods.isSet(() -> siteInfo.get("live_aliases"))) {
                        siteAliases.addAll(parseSiteAliases(siteInfo.get("live_aliases")));
                    }
                    if (siteAliases.contains(alias)) {
                        final String siteInode = siteInfo.get("inode");
                        siteAsContentletList.add(this.getContentletFactory().find(siteInode));
                        if (UtilMethods.isSet(() -> siteInfo.get("live_inode"))) {
                            final String liveInode = siteInfo.get("live_inode");
                            if (!siteInode.equals(liveInode)) {
                                liveSiteMap.put(siteInode, this.getContentletFactory().find(liveInode));
                            }
                        }
                    }
                }
                if (siteAsContentletList.isEmpty()) {
                    siteCache.addHostAlias(alias, HostCache.cache404Contentlet);
                    return null;
                } else if (siteAsContentletList.size() == 1) {
                    site = new Host(siteAsContentletList.get(0));
                } else {
                    for (final Contentlet siteAsContentlet : siteAsContentletList) {
                        if (Boolean.class.cast(siteAsContentlet.get(Host.IS_DEFAULT_KEY))) {
                            site = new Host(siteAsContentlet);
                            break;
                        }
                    }
                    if (null == site) {
                        site = new Host(siteAsContentletList.get(0));
                    }
                    final StringBuilder warningMsg = new StringBuilder().append("ERROR: ").append(siteAsContentletList.size())
                            .append(" Sites have the same alias '").append(alias).append("':\n");
                    for (final Contentlet siteAsContentlet : siteAsContentletList) {
                        warningMsg.append("-> Inode = ").append(siteAsContentlet.getInode()).append("\n");
                    }
                    warningMsg.append("Defaulting to Site '").append(site.getInode()).append("'");
                    Logger.fatal(this, warningMsg.toString());
                }

                this.siteCache.add(site);
                this.siteCache.addHostAlias(alias, site);

                if (!liveSiteMap.isEmpty()) {
                    final String siteInode = site.getInode();
                    if (liveSiteMap.containsKey(siteInode)) {
                        final Host liveHost = new Host(liveSiteMap.get(siteInode));
                        this.siteCache.add(liveHost);
                        if (retrieveLiveVersion) {
                            site = liveHost;
                        }
                    }
                }
            } catch (final Exception e) {
                throw new DotRuntimeException(String.format("An error occurred when retrieving Site with alias '%s': " +
                        "%s", alias, e.getMessage()), e);
            }
        }
        return site;
    }

    @Override
    public List<Host> findAll() throws DotDataException, DotSecurityException {
        return findAll(0, 0, null);
    }

    @Override
    public List<Host> findAll(final int limit, final int offset, final String orderBy) throws DotDataException, DotSecurityException {
        return findAll(limit,offset, orderBy, true);
    }

    @Override
    public List<Host> findAll(final int limit, final int offset, final String orderBy, final boolean includeSystemHost) throws DotDataException, DotSecurityException {
        return findAll(limit, offset, orderBy, includeSystemHost, false);
    }

    @CloseDBIfOpened
    @Override
    public List<Host> findAll(final int limit, final int offset, final String orderBy,
                              final boolean includeSystemHost, final boolean retrieveLiveVersion) throws DotDataException, DotSecurityException {
        final DotConnect dc = new DotConnect();
        final String hostTypeInode = getHostContentTypeInode();
        final StringBuilder sqlQuery = new StringBuilder()
                .append(SELECT_SITE_INODE);
        if (!includeSystemHost) {
            sqlQuery.append(AND);
            sqlQuery.append(EXCLUDE_SYSTEM_HOST);
        }
        final String sanitizedSortBy = SQLUtil.sanitizeSortBy(orderBy);
        if (UtilMethods.isSet(sanitizedSortBy)) {
            sqlQuery.append(ORDER_BY);
        }
        dc.setSQL(sqlQuery.toString());
        dc.addParam(hostTypeInode);
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
        return this.convertDbResultsToSites(dbResults, retrieveLiveVersion);
    }

    @Override
    public Host findSystemHost(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host systemHost = this.siteCache.getById(Host.SYSTEM_HOST, false);
        if (null != systemHost) {
            return systemHost;
        }
        final DotConnect dc = new DotConnect();
        final StringBuffer sqlQuery = new StringBuffer().append(SELECT_SYSTEM_HOST);
        dc.setSQL(sqlQuery.toString());
        final List<Map<String, String>> dbResults = dc.loadResults();
        if (dbResults.isEmpty()) {
            return systemHost;
        }
        final String systemHostId = dbResults.get(0).get("id");
        systemHost = DBSearch(systemHostId, respectFrontendRoles);
        if (dbResults.size() > 1) {
            Logger.fatal(this, "ERROR: There's more than one working version of the System Host!!");
        }
        this.siteCache.add(systemHost);
        return systemHost;
    }

    @Override
    public synchronized Host createSystemHost() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final DotConnect dc = new DotConnect();
        final StringBuffer sqlQuery = new StringBuffer().append(SELECT_SYSTEM_HOST);
        dc.setSQL(sqlQuery.toString());
        final List<Map<String, String>> dbResults = dc.loadResults();
        Host systemHost;
        if (dbResults.isEmpty()) {
            systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            systemHost.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            systemHost = new Host(this.getContentletFactory().save(systemHost));
            systemHost.setIdentifier(Host.SYSTEM_HOST);
            systemHost.setModDate(new Date());
            systemHost.setModUser(systemUser.getUserId());
            systemHost.setOwner(systemUser.getUserId());
            systemHost.setHost(null);
            systemHost.setFolder(null);
            this.getContentletFactory().save(systemHost);
            APILocator.getVersionableAPI().setWorking(systemHost);
        } else {
            systemHost = DBSearch(dbResults.get(0).get("id"), false);
        }
        return systemHost;
    }

    @Override
    public Host DBSearch(final String id, final boolean respectFrontendRoles) throws
            DotDataException, DotSecurityException {
        Host site = null;
        final List<ContentletVersionInfo> versionInfos = APILocator.getVersionableAPI().findContentletVersionInfos(id);
        if (!versionInfos.isEmpty()) {
            final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
            final ContentletVersionInfo versionInfo = versionInfos.stream()
                    .filter(contentletVersionInfo -> contentletVersionInfo.getLang() == defaultLang
                            .getId()).findFirst().orElseGet(() -> {
                        Logger.warn(HostAPIImpl.class, String.format("Unable to find ContentletVersionInfo for Site " +
                                "'%s' using default language [%s]. Fall back to first entry found.", id, defaultLang
                                .getId()));
                        return versionInfos.get(0);
                    });
            final User systemUser = APILocator.systemUser();
            final String siteInode = versionInfo.getWorkingInode();
            final Contentlet siteAsContentlet = this.getContentletAPI().find(siteInode, systemUser, respectFrontendRoles);
            final ContentType hostContentType = APILocator.getContentTypeAPI(systemUser, respectFrontendRoles).find(Host.HOST_VELOCITY_VAR_NAME);
            if (siteAsContentlet.getContentType().id().equals(hostContentType.inode())) {
                site = new Host(siteAsContentlet);
                this.siteCache.add(site);

                if (UtilMethods.isSet(versionInfo::getLiveInode)) {
                    final String liveInode = versionInfo.getLiveInode();
                    if (!siteInode.equals(liveInode)) {
                        final Host liveHost = new Host(this.getContentletAPI().find(
                                liveInode, systemUser, respectFrontendRoles));
                        this.siteCache.add(liveHost);
                        if (respectFrontendRoles) {
                            site = liveHost;
                        }
                    }
                }
            }
        }
        if (null == site && !Host.SYSTEM_HOST.equals(id)) {
            this.siteCache.add404HostById(id);
            Logger.warn(HostAPIImpl.class, String.format("Site with id '%s' not found", id));
        }
        return site;
    }

    /**
     * This method is called inside a separate thread and takes care of deleting the specified Site
     * in a process in the background. Once it's done, a notification will be generated as well,
     * which is particularly useful to the UI layer.
     *
     * @param site                 The {@link Host} object to be deleted.
     * @param user                 The {@link User} object that is requesting the deletion.
     * @param respectFrontendRoles If the User executing this action has the front-end role, or if
     *                             front-end roles must be validated against this user, set to
     *                             {@code true}.
     *
     * @return If the Site was deleted successfully, returns {@code true}.
     *
     * @throws DotRuntimeException The specified Site failed to be deleted.
     */
    @WrapInTransaction
    private Boolean innerDeleteSite(final Host site, final User user, final boolean respectFrontendRoles) {
        try {
            deleteSite(site, user, respectFrontendRoles);
            HibernateUtil.addCommitListener
                    (() -> generateNotification(site, user));
        } catch (final Exception e) {
            try {
                APILocator.getNotificationAPI().generateNotification(
                        new I18NMessage("notification.hostapi.delete.error.title"), // title = Host Notification
                        new I18NMessage("notifications_host_deletion_error", site.getHostname(), ExceptionUtil.getErrorMessage(e)),
                        null, // no actions
                        NotificationLevel.ERROR,
                        NotificationType.GENERIC,
                        user.getUserId(),
                        user.getLocale()
                );
            } catch (final DotDataException e1) {
                Logger.error(HostAPIImpl.class, String.format("An error occurred when saving Site Deletion " +
                        "Notification for site '%s': %s", site, ExceptionUtil.getErrorMessage(e)), e);
            }
            final String errorMsg = String.format("An error occurred when User '%s' tried to delete Site " +
                    "'%s': %s", user.getUserId(), site, ExceptionUtil.getErrorMessage(e));
            Logger.error(HostAPIImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        return Boolean.TRUE;
    }

    private void generateNotification(final Host site, final User user) {
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

    /**
     * Deletes the specified Site from the content repository. There are a lot of different objects
     * that must be deleted when a Site is removed, such as Menu Links, Contentlets, Folders, and so
     * on. This means that the deletion process is quite complex and may take a very long time to
     * finish.
     *
     * @param site                 The {@link Host} object to be deleted.
     * @param user                 The {@link User} object that is requesting the deletion.
     * @param respectFrontendRoles If the User executing this action has the front-end role, or if
     *                             front-end roles must be validated against this user, set to
     *                             {@code true}.
     *
     * @throws Exception An error occurred when deleting the specified Site.
     */
    public void deleteSite(final Host site, final User user, final boolean respectFrontendRoles) throws Exception {
        if (null == site || UtilMethods.isNotSet(site.getIdentifier())) {
            return;
        } else {
            siteCache.remove(site);
        }
        Logger.info(this, "======================================================================");
        Logger.info(this, String.format("  Start deleting Site '%s' ...", site.getHostname()));
        Logger.info(this, "======================================================================");
        final DotConnect dc = new DotConnect();

        final int steps = 15;

        final MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
        final List<Link> links = linkAPI.findLinks(user, true, null, site.getIdentifier(), null, null, null, 0, -1, null);
        Logger.info(this,
                String.format("-> (Step 1/%d) Deleting %d Menu Links from Site '%s'", steps,
                        links.size(), site.getHostname()));
        for (final Link link : links) {
            linkAPI.delete(link, user, respectFrontendRoles);
        }

        Logger.info(this, String.format("-> (Step 2/%d) Deleting all Contentlets from Site '%s'",
                steps, site.getHostname()));
        final ContentletAPI contentAPI = APILocator.getContentletAPI();
        contentAPI.deleteByHost(site, APILocator.systemUser(), respectFrontendRoles);

        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final List<Folder> folders = folderAPI.findFoldersByHost(site, user, respectFrontendRoles);
        Logger.info(this,
                String.format("-> (Step 3/%d) Deleting %d Folders from Site '%s'", steps,
                        folders.size(), site.getHostname()));
        for (final Folder folder : folders) {
            folderAPI.delete(folder, user, respectFrontendRoles);
        }

        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final List<Template> templates = templateAPI.findTemplatesAssignedTo(site, true);
        Logger.info(this, String.format("-> (Step 4/%d) Deleting %d Templates from Site '%s'",
                steps, templates.size(), site.getHostname()));
        for (final Template template : templates) {
            dc.setSQL("delete from template_containers where template_id = ?");
            dc.addParam(template.getIdentifier());
            dc.loadResult();

            if (!template.isDeleted()) {
                templateAPI.unpublishTemplate(template, user, respectFrontendRoles);
                templateAPI.archive(template, user, respectFrontendRoles);
            }

            templateAPI.deleteTemplate(template, user, respectFrontendRoles);
        }

        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .includeArchived(true)
                .siteId(site.getIdentifier())
                .offset(0)
                .limit(-1).build();
        final List<Container> containers = containerAPI.findContainers(user, searchParams);
        Logger.info(this, String.format("-> (Step 5/%d) Deleting %d Containers from Site '%s'",
                steps, containers.size(), site.getHostname()));
        for (final Container container : containers) {
            containerAPI.delete(container, user, respectFrontendRoles);
        }

        final List<ContentType> types = APILocator.getContentTypeAPI(user, respectFrontendRoles)
                .search(" host = '" + site.getIdentifier() + "'");
        Logger.info(this, String.format("-> (Step 6/%d) Deleting %d Content Types from Site '%s'",
                steps, types.size(), site.getHostname()));
        for (final ContentType type : types) {
            final List<Contentlet> contentsByType = contentAPI.findByStructure(new StructureTransformer(type)
                    .asStructure(), APILocator.systemUser(), false, 0, 0);
            for (final Contentlet contentlet : contentsByType) {
                //We are deleting a site/host, we don't need to validate anything.
                contentlet.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                contentAPI.delete(contentlet, user, respectFrontendRoles);
            }

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, respectFrontendRoles);
            //Validate if are allow to delete this content type
            if (!type.system() && !type.defaultType()) {
                contentTypeAPI.deleteSync(type);
            } else {
                Logger.info(this, String.format(
                        "---> (Step 6/%d) Content Type '%s' cannot be deleted, and must be moved to SYSTEM_HOST",
                        steps, type.variable()));
                //If we can not delete it we need to change the host to SYSTEM_HOST
                final ContentType clonedContentType = ContentTypeBuilder.builder(type).from(type)
                        .folder(Folder.SYSTEM_FOLDER)
                        .folderPath(Folder.SYSTEM_FOLDER_PATH)
                        .host(Host.SYSTEM_HOST)
                        .siteName(Host.SYSTEM_HOST_NAME).build();
                contentTypeAPI.save(clonedContentType);
            }
        }

        // wipe bad old containers
        Logger.info(this, String.format("-> (Step 7/%d) Deleting invalid Containers from Site '%s'",
                steps, site.getHostname()));
        dc.setSQL("delete from container_structures where exists (select * from identifier where host_inode=? and container_structures.container_id=id)");
        dc.addParam(site.getIdentifier());
        dc.loadResult();

        Logger.info(this, String.format(
                "-> (Step 8/%d) Deleting all remaining Containers, Templates, and Links from Site "
                        + "'%s'", steps, site.getHostname()));
        final Inode.Type[] assets = {Inode.Type.CONTAINERS, Inode.Type.TEMPLATE, Inode.Type.LINKS};
        for (final Inode.Type asset : assets) {
            dc.setSQL("select inode from "+asset.getTableName()+" where exists (select * from identifier where host_inode=? and id="+asset.getTableName()+".identifier)");
            dc.addParam(site.getIdentifier());
            for (final Map<String, Object> row : (List<Map<String, Object>>)dc.loadResults()) {
                dc.setSQL("delete from "+asset.getVersionTableName()+" where working_inode=? or live_inode=?");
                dc.addParam(row.get("inode"));
                dc.addParam(row.get("inode"));
                dc.loadResult();

                dc.setSQL("delete from "+asset.getTableName()+" where inode=?");
                dc.addParam(row.get("inode"));
                dc.loadResult();
            }
        }

        Logger.info(this, String.format("-> (Step 9/%d) Deleting all Tags from Site '%s'",
                steps, site.getHostname()));
        APILocator.getTagAPI().deleteTagsByHostId(site.getIdentifier());

        // Double-check that ALL contentlets are effectively removed before using dotConnect to kill bad identifiers
        final List<Contentlet> remainingContenlets = contentAPI
                .findContentletsByHost(site, user, respectFrontendRoles);
        Logger.info(this, String.format(
                "-> (Step 10/%d) Deleting (double-checking) %d Contentlets from Site " +
                        "'%s'", steps,
                UtilMethods.isSet(remainingContenlets) ? remainingContenlets.size() : 0,
                site.getHostname()));
        if (UtilMethods.isSet(remainingContenlets)) {
            contentAPI.deleteByHost(site, user, respectFrontendRoles);
        }

        // kill bad identifiers pointing to the site
        Logger.info(this,
                String.format("-> (Step 11/%d) Deleting all invalid Identifiers from Site '%s'",
                        steps, site.getHostname()));
        dc.setSQL("delete from identifier where host_inode=?");
        dc.addParam(site.getIdentifier());
        dc.loadResult();

        Logger.info(this,
                String.format("-> (Step 12/%d) Deleting the Site '%s' itself", steps,
                        site.getHostname()));
        final Contentlet siteAsContent = contentAPI.find(site.getInode(), user, respectFrontendRoles);
        contentAPI.delete(siteAsContent, user, respectFrontendRoles);

        try {
            Logger.info(this, String.format("-> (Step 13/%d) Deleting all Secrets from Site '%s'",
                    steps, site.getHostname()));
            APILocator.getAppsAPI().removeSecretsForSite(site, APILocator.systemUser());
        } catch (final Exception e) {
            Logger.warn(HostAPIImpl.class, String.format("An error occurred when removing secrets for Site " +
                    "'%s': %s", site, ExceptionUtil.getErrorMessage(e)), e);
        }

        Logger.info(this, String.format("-> (Step 14/%d) Deleting site variables from Site '%s'",
                steps, site.getHostname()));
        getHostVariableFactory().deleteAllVariablesForSite(site.getIdentifier());

        Logger.info(this,
                String.format("-> (Step 15/%d) Flushing all caches after deleting Site '%s'",
                        steps, site.getHostname()));
        flushAllCaches(site);

        Logger.info(this, "======================================================================");
        Logger.info(this, String.format("  Site '%s' has been deleted successfully!", site.getHostname()));
        Logger.info(this, "======================================================================");
    }

    @Override
    public Optional<Future<Boolean>> delete(final Host site, final User user, final boolean respectFrontendRoles,
                                            final boolean runAsSeparatedThread) {
        class DeleteSiteThread implements Callable<Boolean> {

            @Override
            public Boolean call() {
                return innerDeleteSite(site, user, respectFrontendRoles);
            }

        }

        final DeleteSiteThread deleteHostThread = new DeleteSiteThread();
        Optional<Future<Boolean>> future;
        if(runAsSeparatedThread) {
            final DotConcurrentFactory concurrentFactory = DotConcurrentFactory.getInstance();
            future = Optional.of(concurrentFactory.getSubmitter
                    (DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL).submit(deleteHostThread));
        } else {
            future = Optional.of(ConcurrentUtils.constantFuture(deleteHostThread.call()));
        }

        return future;
    }

    @Override
    public Optional<Host> findDefaultHost(final String contentTypeId, final String columnName,
                                          final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host defaultHost = this.siteCache.getDefaultHost(respectFrontendRoles);
        if (null != defaultHost) {
            return Optional.of(defaultHost);
        }
        final DotConnect dotConnect = new DotConnect();
        String workingInode = null;
        String liveInode = null;

        if (APILocator.getContentletJsonAPI().isPersistContentAsJson()) {

            String sql = null;

            if(DbConnectionFactory.isPostgres()) {
                sql = "SELECT cvi.working_inode,cvi.live_inode\n"
                        + "  FROM contentlet_version_info cvi join contentlet c on (c.inode = cvi.working_inode) \n"
                        + "  WHERE c.contentlet_as_json @> '{ \"fields\":{\"isDefault\":{ \"value\":true }} }' \n"
                        + "  and c.structure_inode = ?";
            }
            if(DbConnectionFactory.isMsSql()){
                sql = "SELECT cvi.working_inode,cvi.live_inode\n"
                        + "  FROM contentlet_version_info cvi join contentlet c on (c.inode = cvi.working_inode) \n"
                        + "  WHERE JSON_VALUE(c.contentlet_as_json, '$.fields.isDefault.value') = 'true'  \n"
                        + "  and c.structure_inode = ?";
            }
            if(null == sql) {
                throw new IllegalStateException("Unable to determine what db with json support we're running on!");
            }
            dotConnect.setSQL(sql);
            dotConnect.addParam(contentTypeId);
            workingInode = Try.of(()->dotConnect.getString("working_inode")).onFailure(throwable -> {
                Logger.warnAndDebug(HostAPIImpl.class,"An Error occurred while fetching the working inode for the default host. ", throwable);
            }).getOrNull();
            liveInode = Try.of(()->dotConnect.getString("live_inode")).onFailure(throwable -> {
                Logger.warnAndDebug(HostAPIImpl.class,"An Error occurred while fetching the live inode for the default host. ", throwable);
            }).getOrNull();
        }
        if (UtilMethods.isNotSet(workingInode)) {
            dotConnect
                    .setSQL("select working_inode,live_inode"
                            + " from contentlet_version_info join contentlet"
                            + " on (contentlet.inode = contentlet_version_info.working_inode) "
                            + " where " + columnName  + " = ? and structure_inode =?");
            dotConnect.addParam(true);
            dotConnect.addParam(contentTypeId);
            workingInode = dotConnect.getString("working_inode");
            liveInode = dotConnect.getString("live_inode");
        }
        if (UtilMethods.isNotSet(workingInode)) {
            return Optional.empty();
        }
        defaultHost = new Host(APILocator.getContentletAPI().find(workingInode, APILocator.systemUser(), false));
        this.siteCache.add(defaultHost);
        if (UtilMethods.isSet(liveInode) && !StringUtils.equals(workingInode,liveInode)) {
            final Host liveDefaultHost = new Host(APILocator.getContentletAPI().find(
                    liveInode, APILocator.systemUser(), false));
            this.siteCache.add(liveDefaultHost);
            if (respectFrontendRoles) {
                defaultHost = liveDefaultHost;
            }
        }
        return Optional.of(defaultHost);
    }

    @Override
    public Optional<List<Host>> findLiveSites(final String siteNameFilter, final int limit, final int offset,
                                              final boolean showSystemHost, final User user, final boolean respectFrontendRoles) {
        return search(siteNameFilter, SITE_IS_LIVE, showSystemHost, limit, offset, user,
                respectFrontendRoles);
    }

    @Override
    public Optional<List<Host>> findStoppedSites(final String siteNameFilter, final boolean includeArchivedSites,
                                                 final int limit, final int offset, final boolean showSystemHost,
                                                 final User user, final boolean respectFrontendRoles) {
        final String condition =
                includeArchivedSites ? SITE_IS_STOPPED_OR_ARCHIVED : SITE_IS_STOPPED;
        return search(siteNameFilter, condition, showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public Optional<List<Host>> findLiveAndStopped(final String siteNameFilter,
                                                   final int limit, final int offset, final boolean showSystemHost,
                                                   final User user, boolean respectFrontendRoles) {

        final StringBuilder sqlQuery = new StringBuilder();
        
        if (!showSystemHost) {
            sqlQuery.append(EXCLUDE_SYSTEM_HOST);
            sqlQuery.append(AND);
        }
        sqlQuery.append(SITE_IS_LIVE_OR_STOPPED);
        return search(siteNameFilter, sqlQuery.toString(), showSystemHost, limit, offset, user, respectFrontendRoles);
    }

    @Override
    public Optional<List<Host>> findArchivedSites(final String siteNameFilter, final int limit, final int offset,
                                                  final boolean showSystemHost, final User user,
                                                  final boolean respectFrontendRoles) {
        return search(siteNameFilter, SITE_IS_ARCHIVED, showSystemHost, limit, offset, user,
                respectFrontendRoles);
    }

    @Override
    public long count() throws DotDataException {
        final DotConnect dc = new DotConnect();
        final String hostTypeInode = getHostContentTypeInode();
        dc.setSQL(SELECT_SITE_COUNT);
        dc.addParam(hostTypeInode);
        final List<Map<String, String>> dbResults = dc.loadResults();
        final String total = dbResults.get(0).get("count");
        return ConversionUtils.toLong(total, 0L);
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
     * @param siteNameFilter       The initial part or full name of the Site you need to look up. If not required, set
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
    @VisibleForTesting
    protected Optional<List<Host>> search(final String siteNameFilter, final String condition, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles) {
        return search(siteNameFilter,condition,showSystemHost,limit,offset,user,respectFrontendRoles,new ArrayList<>());
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
     * @param siteNameFilter       The initial part or full name of the Site you need to look up. If not required, set
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
    @VisibleForTesting
    protected Optional<List<Host>> search(final String siteNameFilter, final String condition, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles, List<Host> hostList) {
        final DotConnect dc = new DotConnect();
        final String hostTypeInode = getHostContentTypeInode();
        final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE);
        sqlQuery.append(AND);
        sqlQuery.append("cvi.identifier = i.id");
        if (UtilMethods.isSet(siteNameFilter)) {
            sqlQuery.append(AND);
            sqlQuery.append(getSiteNameOrAliasColumn(SITE_NAME_LIKE, true, "c"));
        }
        if (UtilMethods.isSet(condition)) {
            sqlQuery.append(AND);
            sqlQuery.append(condition);
        }
        if (!showSystemHost) {
            sqlQuery.append(AND);
            sqlQuery.append(EXCLUDE_SYSTEM_HOST);
        }
        if (UtilMethods.isSet(siteNameFilter)) {
            sqlQuery.append(getSiteNameOrAliasColumn(PRIORITIZE_EXACT_MATCHES, true, "c", "c"));
        }

        dc.setSQL(sqlQuery.toString());
        dc.addParam(hostTypeInode);
        if (UtilMethods.isSet(siteNameFilter)) {
            // Add the site name filter parameter
            dc.addParam(("%" + siteNameFilter.trim() + "%").replace("%%", "%"));

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
                return Optional.of(hostList);
            }
            final List<Host> siteList = convertDbResultsToSites(dbResults, respectFrontendRoles);
            if(user.isAdmin()){
                return Optional.of(siteList);
            }
            hostList.addAll(APILocator.getPermissionAPI().filterCollection(siteList, PermissionAPI
                    .PERMISSION_READ, respectFrontendRoles, user));
            if (limit > 0) {
                hostList = hostList.stream().limit(limit).collect(Collectors.toList());
            }

            //We want to include the system host in the list of hosts
            final Host systemHost = APILocator.systemHost();
            //Check if we need to include it, if we have the permissions and if it is in the list.
            final boolean sysHostPermission = showSystemHost
                    && !hostList.contains(systemHost)
                    && APILocator.getPermissionAPI().doesSystemHostHavePermissions(systemHost, user, respectFrontendRoles, Host.class.getCanonicalName());

            if(sysHostPermission){
                hostList.add(systemHost);
            }

            if (limit == 0 || hostList.size() == limit || siteList.size() < limit) {//reached the amount of sites requested or there is no anymore sites
                return Optional.of(hostList);
            } else {
                return search(siteNameFilter, condition, showSystemHost,
                        limit,offset+limit, user, respectFrontendRoles, hostList);
            }
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when searching for Sites based on the following " +
                    "criteria: filter[ %s ], condition[ %s ], showSystemHost [ %s ]: %s", siteNameFilter, condition,
                    showSystemHost, e.getMessage()), e);
            throw new DotRuntimeException(String.format("An error occurred when searching for Sites: %s", e.getMessage
                    ()), e);
        }
    }

    /**
     * Utility method used to convert Site data from the data source into a list of Sites.
     *
     * @param dbResults           Data representing a Site.
     * @param retrieveLiveVersion If the live version of the Site must be retrieved.
     * @return The list of {@link Host} objects.
     */
    private List<Host> convertDbResultsToSites(final List<Map<String, String>> dbResults,
                                               final boolean retrieveLiveVersion) {
        return dbResults.stream().map(siteData -> {
            try {
                final String inode = retrieveLiveVersion && UtilMethods.isSet(() -> siteData.get("live_inode")) ?
                        siteData.get("live_inode") : siteData.get("inode");
                final Contentlet contentlet = this.getContentletFactory().find(inode);
                return new Host(contentlet);
            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Contentlet with Inode '%s' could not be retrieved from Content " +
                        "Factory: %s", siteData.get("inode"), e.getMessage()));
                return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * Utility method used to transform a list of Site Aliases into a list. Site Aliases can be separated by either
     * commas, blank spaces, or line breaks, so this method (1) replaces the separation characters with blank spaces,
     * and then (2) adds each element to a list.
     *
     * @param aliases The list of Site Aliases for a given Site.
     *
     * @return A {@link List} with every unique Site Alias.
     */
    private List<String> parseSiteAliases(final String aliases) {
        final List<String> result = new ArrayList<>();
        final StringTokenizer tok = new StringTokenizer(aliases, ", \n\r\t");
        while (tok.hasMoreTokens()) {
            result.add(tok.nextToken());
        }
        return result;
    }

    /**
     * Returns the appropriate column for the {@code Site Name} or {@code Alias} field depending
     * on the database that dotCMS is running on. That is, if the value is inside
     * the "Content as JSON" column, or the legacy "text" column.
     *
     * @param baseQuery     The base SQL query whose column name will be replaced.
     * @param getSiteColumn If the column is for the Site Name field, set to {@code true}. Otherwise, set to {@code false}.
     * @param tableAliases  The aliases of the tables where the Site Name or Alias fields are located.
     * @return The appropriate database column for the Site Name or Alias field.
     */
    private static String getSiteNameOrAliasColumn(final String baseQuery,
                                                   final boolean getSiteColumn, final String... tableAliases) {

        final Object [] fields = new Object[tableAliases.length];
        for (int i = 0; i < tableAliases.length; i++) {
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                if (DbConnectionFactory.isPostgres()) {
                    fields[i] = String.format(getSiteColumn ?
                        POSTGRES_SITENAME_COLUMN : POSTGRES_ALIASES_COLUMN, tableAliases[i]);
                } else {
                    fields[i] = String.format(getSiteColumn ?
                        MSSQL_SITENAME_COLUMN : MSSQL_ALIASES_COLUMN, tableAliases[i]);
                }
            } else {
                fields[i] = String.format(getSiteColumn ?
                        SITENAME_COLUMN : ALIASES_COLUMN, tableAliases[i]);
            }
        }
        return String.format(baseQuery, fields);
    }

    /**
     * Attempts to find a site by ID if the provided siteName is a valid UUID.
     *
     * @param siteName The site name that may be a UUID identifier.
     * @param retrieveLiveVersion If the live version of the Site must be retrieved.
     * @return The {@link Host} object if found by ID, otherwise {@code null}.
     */
    private Host findSiteByIdIfUUID(final String siteName, final boolean retrieveLiveVersion) {
        if (UUIDUtil.isUUID(siteName)) {
            try {
                // siteName is a valid UUID, try to find by ID
                final Host siteById = DBSearch(siteName, retrieveLiveVersion);
                if (siteById != null) {
                    Logger.debug(this, () -> String.format("Site found by ID '%s'", siteName));
                    return siteById;
                }
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Error searching for site by ID '%s'", siteName), e);
            }
        }
        return null;
    }

    /**
     * Utility method that completely clears the Site Cache Region across all nodes in your environment.
     *
     * @param site The {@link Host} object that was modified, which triggers a complete flush of the Site Cache Region.
     */
    private void flushAllCaches(final Host site) {
        this.siteCache.remove(site);
        this.siteCache.clearCache();
    }

}
