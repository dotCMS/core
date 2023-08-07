package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
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
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

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
 * SQL-based implementation class for {@link HostFactory}.
 *
 * @author Jose Castro
 * @version 22.4
 * @since Mar 15, 2022
 */
public class HostFactoryImpl implements HostFactory {

    private HostCache siteCache = CacheLocator.getHostCache();
    private ContentletFactory contentFactory;
    private ContentletAPI contentletAPI;

    private final String WHERE = " WHERE ";
    private final String AND = " AND ";
    private final String ORDER_BY = " ORDER BY ? ";

    private static final String SELECT_SYSTEM_HOST = "SELECT id FROM identifier WHERE id = '"+ Host.SYSTEM_HOST+"' ";

    private static final String FROM_JOINED_TABLES = "INNER JOIN identifier i " +
            "ON c.identifier = i.id AND i.asset_subtype = '" + Host.HOST_VELOCITY_VAR_NAME + "' " +
            "INNER JOIN contentlet_version_info cvi " +
            "ON c.inode = cvi.working_inode ";

    private static final String SELECT_SITE_INODE = "SELECT c.inode FROM contentlet" +
            " c " +
            FROM_JOINED_TABLES;
    private static final String SELECT_SITE_INODE_AND_ALIASES = "SELECT c.inode, %s" +
            " AS aliases FROM contentlet c " +
            FROM_JOINED_TABLES;

    private static final String POSTGRES_ALIASES_COLUMN = ContentletJsonAPI
            .CONTENTLET_AS_JSON +
            "->'fields'->'" + Host.ALIASES_KEY + "'->>'value' ";

    private static final String MSSQL_ALIASES_COLUMN = "JSON_VALUE(c." +
            ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + Host.ALIASES_KEY + ".value') ";
    private static final String ALIASES_COLUMN = "c.text_area1";

    private static final String SITE_NAME_LIKE = "LOWER(%s) LIKE ? ";

    private static final String SITE_NAME_EQUALS ="LOWER(%s) = ? ";

    private static final String POSTGRES_SITENAME_COLUMN = ContentletJsonAPI
            .CONTENTLET_AS_JSON +
            "->'fields'->'" + Host.HOST_NAME_KEY + "'->>'value' ";

    private static final String MSSQL_SITENAME_COLUMN = "JSON_VALUE(c." +
            ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + Host.HOST_NAME_KEY + ".value')" +
            " ";

    private static final String SITENAME_COLUMN = "c.text1";

    private static final String ALIAS_LIKE = "LOWER(%s) LIKE ? ";

    private static final String EXCLUDE_SYSTEM_HOST = "i.id <> '" + Host
            .SYSTEM_HOST +
            "' ";

    private static final String SITE_IS_LIVE = "cvi.live_inode IS NOT NULL";
    @VisibleForTesting
    protected static final String SITE_IS_LIVE_OR_STOPPED = "cvi.live_inode IS NOT null or " +
            "(cvi.live_inode IS NULL AND cvi.deleted = false )";
    private static final String SITE_IS_STOPPED = "cvi.live_inode IS NULL AND cvi" +
            ".deleted = " +
            getDBFalse();

    private static final String SITE_IS_STOPPED_OR_ARCHIVED = "cvi.live_inode IS NULL";

    private static final String SITE_IS_ARCHIVED = "cvi.live_inode IS NULL AND cvi" +
            ".deleted = " +
            getDBTrue();

    private static final String SELECT_SITE_COUNT = "SELECT COUNT(cvi.working_inode) " +
            "FROM contentlet_version_info cvi, identifier i " + "WHERE i.asset_subtype = '" +
            Host.HOST_VELOCITY_VAR_NAME + "' " + " AND cvi.identifier = i.id ";

    // query that Exact matches should be at the top of the search results.
    private static final String PRIORITIZE_EXACT_MATCHES =
            "ORDER BY CASE WHEN LOWER(%s) = ? THEN 0 ELSE 1 END";

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

    @Override
    public Host bySiteName(final String siteName) {
        Host site = siteCache.get(siteName);
        if (null == site || !UtilMethods.isSet(site.getIdentifier())) {
            final DotConnect dc = new DotConnect();
            final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE)
                    .append(WHERE);
            sqlQuery.append(getSiteNameColumn(SITE_NAME_EQUALS));
            dc.setSQL(sqlQuery.toString());
            dc.addParam(siteName.toLowerCase());
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
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
            } catch (final Exception e) {
                final String errorMsg = String.format("An error occurred when retrieving Site by name '%s': %s",
                        siteName, e.getMessage());
                throw new DotRuntimeException(errorMsg, e);
            }
        }
        return site;
    }

    @Override
    public Host byAlias(String alias) {
        Host site = this.siteCache.getHostByAlias(alias);
        if (null == site) {
            final DotConnect dc = new DotConnect();
            final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE_AND_ALIASES)
                    .append(WHERE)
                    .append(ALIAS_LIKE);
            String sql = sqlQuery.toString();
            if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
                if (DbConnectionFactory.isPostgres()) {
                    sql = String.format(sql, POSTGRES_ALIASES_COLUMN, POSTGRES_ALIASES_COLUMN);
                } else {
                    sql = String.format(sql, MSSQL_ALIASES_COLUMN, MSSQL_ALIASES_COLUMN);
                }
            } else {
                sql = String.format(sql, ALIASES_COLUMN, ALIASES_COLUMN);
            }
            dc.setSQL(sql);
            dc.addParam("%" + alias.toLowerCase() + "%");
            try {
                final List<Map<String, String>> dbResults = dc.loadResults();
                if (dbResults.isEmpty()) {
                    return null;
                }
                if (dbResults.size() == 1) {
                    final Set<String> siteAliases = new HashSet<>(parseSiteAliases(dbResults.get(0).get("aliases")));
                    if (siteAliases.contains(alias)) {
                        site = new Host(this.getContentletFactory().find(dbResults.get(0).get("inode")));
                    }
                } else {
                    final List<Contentlet> siteAsContentletList = new ArrayList<>();
                    for (final Map<String, String> siteInfo : dbResults) {
                        final Set<String> siteAliases = new HashSet<>(parseSiteAliases(siteInfo.get("aliases")));
                        if (siteAliases.contains(alias)) {
                            siteAsContentletList.add(this.getContentletFactory().find(siteInfo.get("inode")));
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
                }
                this.siteCache.add(site);
                this.siteCache.addHostAlias(alias, site);
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

    @CloseDBIfOpened
    @Override
    public List<Host> findAll(final int limit, final int offset, final String orderBy, final boolean includeSystemHost) throws DotDataException, DotSecurityException {
        final DotConnect dc = new DotConnect();
        final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE)
                .append(WHERE)
                .append(" true ");
        if (!includeSystemHost) {
            sqlQuery.append(AND);
            sqlQuery.append(EXCLUDE_SYSTEM_HOST);
        }
        final String sanitizedSortBy = SQLUtil.sanitizeSortBy(orderBy);
        if (UtilMethods.isSet(sanitizedSortBy)) {
            sqlQuery.append(ORDER_BY);
        }
        dc.setSQL(sqlQuery.toString());
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
        return this.convertDbResultsToSites(dbResults);
    }

    @Override
    public Host findSystemHost(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Host systemHost = this.siteCache.get(Host.SYSTEM_HOST);
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
            }
        }
        return site;
    }

    @WrapInTransaction
    private Boolean innerDeleteHost(final Host site, final User user, final boolean respectFrontendRoles) {
        try {
            deleteHost(site, user, respectFrontendRoles);
            HibernateUtil.addCommitListener
                    (() -> generateNotification(site, user));
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
                    "'%s': %s", user.getUserId(), site, e.getMessage());
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

    public void deleteHost(final Host site, final User user, final boolean respectFrontendRoles) throws Exception {
        if(site != null){
            siteCache.remove(site);
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

    @Override
    public Optional<Future<Boolean>> delete(final Host site, final User user, final boolean respectFrontendRoles,
                                            final boolean runAsSeparatedThread) {
        Optional<Future<Boolean>> future = Optional.empty();

        class DeleteHostThread implements Callable<Boolean> {

            @Override
            public Boolean call() {
                return innerDeleteHost(site, user, respectFrontendRoles);
            }
        }

        final DeleteHostThread deleteHostThread = new DeleteHostThread();

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
    public Optional<Host> findDefaultHost(final String contentTypeId, final String columnName) throws DotDataException, DotSecurityException {
        Host defaultHost = this.siteCache.getDefaultHost();
        if (null != defaultHost) {
            return Optional.of(defaultHost);
        }
        final DotConnect dotConnect = new DotConnect();
        String inode = null;

        if (APILocator.getContentletJsonAPI().isPersistContentAsJson()) {

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
            dotConnect.addParam(contentTypeId);
            inode = Try.of(()->dotConnect.getString("working_inode")).onFailure(throwable -> {
                Logger.warnAndDebug(HostAPIImpl.class,"An Error occurred while fetching the default host. ", throwable);
            }).getOrNull();
        }
        if (UtilMethods.isNotSet(inode)) {
            dotConnect
                    .setSQL("select working_inode from contentlet_version_info join contentlet on (contentlet.inode = contentlet_version_info.working_inode) "
                            + " where " + columnName  + " = ? and structure_inode =?");
            dotConnect.addParam(true);
            dotConnect.addParam(contentTypeId);
            inode = dotConnect.getString("working_inode");
        }
        if (UtilMethods.isNotSet(inode)) {
            return Optional.empty();
        }
        defaultHost = new Host(APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false));
        this.siteCache.add(defaultHost);
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
        dc.setSQL(SELECT_SITE_COUNT);
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
        final DotConnect dc = new DotConnect();
        final StringBuilder sqlQuery = new StringBuilder().append(SELECT_SITE_INODE);
        sqlQuery.append(WHERE);
        sqlQuery.append("cvi.identifier = i.id");
        if (UtilMethods.isSet(siteNameFilter)) {
            sqlQuery.append(AND);
            sqlQuery.append(getSiteNameColumn(SITE_NAME_LIKE));
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
            sqlQuery.append(getSiteNameColumn(PRIORITIZE_EXACT_MATCHES));
        }

        dc.setSQL(sqlQuery.toString());
        if (UtilMethods.isSet(siteNameFilter)) {
            // Add the site name filter parameter
            dc.addParam("%" + siteNameFilter.trim() + "%");
            // Add the site name filter parameter again, but this time for the exact match
            dc.addParam(siteNameFilter.trim().replace("%", ""));
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
                return Optional.empty();
            }
            List<Host> siteList = convertDbResultsToSites(dbResults);
            siteList = APILocator.getPermissionAPI().filterCollection(siteList, PermissionAPI
                    .PERMISSION_READ, respectFrontendRoles, user);
            return Optional.of(siteList);
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
     * @param dbResults Data representing a Site.
     *
     * @return The list of {@link Host} objects.
     */
    private List<Host> convertDbResultsToSites(final List<Map<String, String>> dbResults) {
        return dbResults.stream().map(siteData -> {
            try {
                final Contentlet contentlet = this.getContentletFactory().find(siteData.get("inode"));
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
     * Returns the appropriate column for the {@code Site Name} field depending on the database that dotCMS is running
     * on. That is, if the value is inside the "Content as JSON" column, or the legacy "text" column.
     *
     * @param baseQuery The base SQL query whose column name will be replaced.
     *
     * @return The appropriate database column for the Site Name field.
     */
    private static String getSiteNameColumn(final String baseQuery) {
        String sql = baseQuery;
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            if (DbConnectionFactory.isPostgres()) {
                sql = String.format(sql, POSTGRES_SITENAME_COLUMN);
            } else {
                sql = String.format(sql, MSSQL_SITENAME_COLUMN);
            }
        } else {
            sql = String.format(sql, SITENAME_COLUMN);
        }
        return sql;
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
