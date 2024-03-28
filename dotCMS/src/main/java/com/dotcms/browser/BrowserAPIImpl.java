package com.dotcms.browser;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotFolderTransformerBuilder;
import com.dotmarketing.portlets.contentlet.transform.DotMapViewTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation for the {@link BrowserAPI} class.
 *
 * @author Jonathan Sanchez
 * @since Apr 28th, 2020
 */
public class BrowserAPIImpl implements BrowserAPI {

    private final UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
    private final FolderAPI folderAPI = APILocator.getFolderAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private static final StringBuilder POSTGRES_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("-> 'fields' -> ").append("'fileName' ->> 'value' ");
    private static final StringBuilder POSTGRES_BINARY_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("-> 'fields' -> ").append("'asset' ->> 'value' ");

    private static final StringBuilder MSSQL_ASSETNAME_COLUMN = new StringBuilder("JSON_VALUE(c.").append
            (ContentletJsonAPI.CONTENTLET_AS_JSON).append(", '$.fields.").append("fileName.").append("value')" +
            " ");

    private static final StringBuilder ASSET_NAME_LIKE = new StringBuilder().append("LOWER(%s) LIKE ? ");

    private static final StringBuilder ASSET_NAME_EQ = new StringBuilder().append("LOWER(%s) = ? ");

    /**
     * Returns a collection of contentlets based on specific filtering criteria specified via the
     * {@link BrowserQuery} class, such as: Parent folder, Site, archived/non-archived status, base Content Types,
     * language, among many others. After that, the resulting list is filtered based on {@code READ} permissions.
     *
     * @param browserQuery The {@link BrowserQuery} object specifying the filtering criteria.
     *
     * @return The list of filtered contentlets.
     */
    @Override
    @CloseDBIfOpened
    public List<Contentlet> getContentUnderParentFromDB(final BrowserQuery browserQuery) {
        final Tuple2<String, List<Object>> sqlQuery = this.selectQuery(browserQuery);
        final DotConnect dc = new DotConnect().setSQL(sqlQuery._1);
        sqlQuery._2.forEach(dc::addParam);
        try {
            final List<Map<String,String>> inodesMapList =  dc.loadResults();
            final Set<String> inodes =
                    inodesMapList.stream().map(data -> data.get("inode")).collect(Collectors.toSet());

            final List<Contentlet> contentlets = APILocator.getContentletAPI().findContentlets(new ArrayList<>(inodes));
            return permissionAPI.filterCollection(contentlets,
                    PermissionAPI.PERMISSION_READ, true, browserQuery.user);
        } catch (final Exception e) {
            final String folderPath = UtilMethods.isSet(browserQuery.folder) ? browserQuery.folder.getPath() : "N/A";
            final String siteName = UtilMethods.isSet(browserQuery.site) ? browserQuery.site.getHostname() : "N/A";
            final String errorMsg = String.format("Failed to load contents from folder '%s' in Site '%s': %s",
                    folderPath, siteName, e.getMessage());
            Logger.warnAndDebug(this.getClass(), errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Returns a collection of contentlets, folders, links based on diff attributes of the BrowserQuery
     * object. The collection is filtered based on the user's permissions respecting front-end roles
     * @param browserQuery {@link BrowserQuery}
     * @return list of treeable (folders, content, links)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    public List<Treeable> getFolderContentList(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {
      return getFolderContentList(browserQuery, true);
    }

    /**
     * Returns a collection of contentlets, folders, links based on diff attributes of the BrowserQuery
     * @param browserQuery {@link BrowserQuery}
     * @param respectFrontEndRoles if true, the method will respect the front end roles
     * @return list of treeable (folders, content, links)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    @CloseDBIfOpened
    public List<Treeable> getFolderContentList(final BrowserQuery browserQuery, final boolean respectFrontEndRoles) throws DotSecurityException, DotDataException{

        final List<Contentlet> contentlets = browserQuery.showContent ? getContentUnderParentFromDB(browserQuery)
                : Collections.emptyList();
        final List<Treeable> returnList = new ArrayList<>(contentlets);

        if (browserQuery.showFolders) {
            List<Folder> folders = folderAPI.findSubFoldersByParent(browserQuery.directParent, userAPI.getSystemUser(), false);
            if (browserQuery.showMenuItemsOnly) {
                folders.removeIf(folder -> !folder.isShowOnMenu());
            }
            returnList.addAll(folders);
        }

        if (browserQuery.showLinks) {
            List<Link> links = this.getLinks(browserQuery);
            if(browserQuery.showMenuItemsOnly){
                links.removeIf(link -> !link.isShowOnMenu());
            }
            returnList.addAll(links);
        }

        return permissionAPI.filterCollection(returnList, PERMISSION_READ, respectFrontEndRoles, browserQuery.user);
    }

    @Override
    @CloseDBIfOpened
    public Map<String, Object> getFolderContent(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {
        List<Map<String, Object>> returnList = new ArrayList<>();
        final Role[] roles = APILocator.getRoleAPI().loadRolesForUser(browserQuery.user.getUserId()).toArray(new Role[0]);

        if (browserQuery.showFolders) {
            returnList.addAll(this.getFolders(browserQuery,  roles));
        }

        if (browserQuery.showLinks) {
            returnList.addAll(this.includeLinks(browserQuery));
        }

        //Get Content
        final List<Contentlet> contentlets = browserQuery.showContent ? getContentUnderParentFromDB(browserQuery)
                : Collections.emptyList();

        for (final Contentlet contentlet : contentlets) {
            Map<String, Object> contentMap;
            if (contentlet.getBaseType().get() == BaseContentType.FILEASSET) {
                final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
                contentMap = fileAssetMap(fileAsset);
            } else if (contentlet.getBaseType().get() == BaseContentType.DOTASSET) {
                contentMap = dotAssetMap(contentlet);
            } else if (contentlet.getBaseType().get() == BaseContentType.HTMLPAGE) {
                final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
                contentMap = htmlPageMap(page);
            } else {
                contentMap = dotContentMap(contentlet);
            }
            if (browserQuery.showShorties) {
                contentMap.put("shortyIdentifier", this.shortyIdAPI.shortify(contentlet.getIdentifier()));
                contentMap.put("shortyInode", this.shortyIdAPI.shortify(contentlet.getInode()));
            }
            final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(contentlet, roles, browserQuery.user);
            final WfData wfdata = new WfData(contentlet, permissions, browserQuery.user, browserQuery.showArchived);
            contentMap.put("wfActionMapList", wfdata.wfActionMapList);
            contentMap.put("contentEditable", wfdata.contentEditable);
            contentMap.put("permissions", permissions);
            returnList.add(contentMap);
        }

        // Filtering
        returnList = this.filterReturnList(browserQuery,returnList);

        // Sorting
        Collections.sort(returnList, new WebAssetMapComparator(browserQuery.sortBy, browserQuery.sortByDesc));

        int offset     = browserQuery.offset;
        int maxResults = browserQuery.maxResults;
        // Offsetting
        if (offset < 0) {
            offset = 0;
        }

        if (maxResults <= 0) {
            maxResults = returnList.size() - offset;
        }

        if (maxResults + offset > returnList.size()) {
            maxResults = returnList.size() - offset;
        }

        final Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("total", returnList.size());
        returnMap.put("list",
                offset > returnList.size() ? Collections.emptyList() : returnList.subList(offset, offset + maxResults));
        return returnMap;
    }

    private List<Map<String, Object>> filterReturnList(final BrowserQuery browserQuery, final List<Map<String, Object>> returnList) {

        final List<Map<String, Object>> filteredList = new ArrayList<>();
        for (final Map<String, Object> asset : returnList) {

            String name = (String) asset.get("name");
            name = name == null ? StringPool.BLANK : name;

            String description = (String) asset.get("description");
            description = description == null ? StringPool.BLANK : description;

            String mimeType = (String) asset.get("mimeType");
            mimeType = mimeType == null ? StringPool.BLANK : mimeType;

            if (browserQuery.mimeTypes != null && browserQuery.mimeTypes.size() > 0) {

                boolean match = false;
                for (final String mType : browserQuery.mimeTypes) {
                    if (mimeType.contains(mType)) {
                        match = true;
                    }
                }

                if (!match) {
                    continue;
                }
            }

            if (browserQuery.extensions != null && browserQuery.extensions.size() > 0) {

                boolean match = false;
                for (final String extension : browserQuery.extensions) {
                    if (((String) asset.get("extension")).contains(extension)) {
                        match = true;
                    }
                }

                if (!match) {
                    continue;
                }
            }

            filteredList.add(asset);
        }

        return filteredList;
    }

    /**
     * Generates the SQL query that will be used to search for contents under a given folder path
     * and filtered by the criteria specified via the {@link BrowserQuery} parameter.
     *
     * @param browserQuery The filtering criteria set via the {@link BrowserQuery}.
     * @return The {@link Tuple3} object containing (1) the SQL query, and (2) the parameters that
     * must be provided for the queries.
     */
    private Tuple2<String, List<Object>> selectQuery(final BrowserQuery browserQuery) {

        final String workingLiveInode = browserQuery.showWorking || browserQuery.showArchived ?
                "working_inode" : "live_inode";

        final StringBuilder sqlQuery = new StringBuilder(
                buildBaseQuery(browserQuery, workingLiveInode)
        );

        final List<Object> parameters = new ArrayList<>();

        if (browserQuery.languageId > 0) {
            appendLanguageQuery(sqlQuery, browserQuery.languageId,
                    browserQuery.showDefaultLangItems);
        }
        if (browserQuery.site != null) {
            appendSiteQuery(sqlQuery, browserQuery.site.getIdentifier(), parameters);
        }
        if (browserQuery.folder != null) {
            appendFolderQuery(sqlQuery, browserQuery.folder.getPath(), parameters);
        }
        if (UtilMethods.isSet(browserQuery.filter)) {
            appendFilterQuery(sqlQuery, browserQuery.filter, parameters);
        }
        if (UtilMethods.isSet(browserQuery.fileName)) {
            appendFileNameQuery(sqlQuery, browserQuery.fileName, parameters);
        }
        if (browserQuery.showMenuItemsOnly) {
            appendShowOnMenuQuery(sqlQuery);
        }
        if (!browserQuery.showArchived) {
            appendExcludeArchivedQuery(sqlQuery);
        }

        return new Tuple2<>(sqlQuery.toString(), parameters);
    }

    /**
     * Builds the base SQL query for content retrieval based on specific filtering criteria.
     *
     * @param browserQuery     The {@link BrowserQuery} object specifying the filtering criteria.
     * @param workingLiveInode The identifier of the working live inode.
     * @return The base SQL query as a {@code String}.
     */
    private String buildBaseQuery(final BrowserQuery browserQuery, final String workingLiveInode) {

        final StringBuilder baseQuery = new StringBuilder(
                "select cvi." + workingLiveInode + " as inode "
                        + " from contentlet_version_info cvi, identifier id, structure struc, contentlet c "
                        + " where cvi.identifier = id.id and struc.velocity_var_name = id.asset_subtype and  "
                        + " c.inode = cvi." + workingLiveInode + " and cvi.variant_id='"
                        + DEFAULT_VARIANT.name() + "' ");

        final boolean showAllBaseTypes = browserQuery.baseTypes.contains(BaseContentType.ANY);
        if (!showAllBaseTypes) {
            final List<String> baseTypes =
                    browserQuery.baseTypes.stream().map(t -> String.valueOf(t.getType()))
                            .collect(Collectors.toList());
            baseQuery.append(" and struc.structuretype in (").
                    append(String.join(" , ", baseTypes)).append(") ");
        }

        return baseQuery.toString();
    }

    /**
     * Appends the language query to the given SQL query to filter content by language.
     *
     * @param sqlQuery             The StringBuilder object representing the SQL query.
     * @param languageId           The ID of the language to filter by.
     * @param showDefaultLangItems Whether to include default language items in the filter.
     */
    private void appendLanguageQuery(StringBuilder sqlQuery, long languageId,
            boolean showDefaultLangItems) {

        sqlQuery.append(" and cvi.lang in (").append(languageId);

        final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (showDefaultLangItems && languageId != defaultLang) {
            sqlQuery.append(",").append(defaultLang);
        }
        sqlQuery.append(")");
    }

    /**
     * Appends the query to filter by site identifier to the given SQL query and adds the site
     * identifier to the parameters list.
     *
     * @param sqlQuery       The StringBuilder object representing the SQL query to be appended.
     * @param siteIdentifier The site identifier to filter by.
     * @param parameters     The list of parameters to add the site identifier to.
     */
    private void appendSiteQuery(StringBuilder sqlQuery, String siteIdentifier,
            List<Object> parameters) {

        sqlQuery.append(" and (id.host_inode = ?) ");
        parameters.add(siteIdentifier);
    }

    /**
     * Appends the query to filter by a specific folder path to the given SQL query and adds the
     * folder path to the list of parameters.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param folderPath The folder path to filter by.
     * @param parameters The list of parameters to add the folder path to.
     */
    private void appendFolderQuery(StringBuilder sqlQuery, String folderPath,
            List<Object> parameters) {

        sqlQuery.append(" and id.parent_path=? ");
        parameters.add(folderPath);
    }

    /**
     * Appends the query to filter by a specific text filter to the given SQL query and adds the
     * necessary parameters for the filter.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param filter     The filter string to match against. Case-insensitive.
     * @param parameters The list of parameters to add the filter values to.
     */
    private void appendFilterQuery(StringBuilder sqlQuery, String filter,
            List<Object> parameters) {

        final String filterText = filter.toLowerCase().trim();
        final String[] splitter = filterText.split(" ");

        sqlQuery.append(" and (");
        for (int indx = 0; indx < splitter.length; indx++) {
            final String token = splitter[indx];
            if (token.equals(StringPool.BLANK)) {
                continue;
            }
            sqlQuery.append(" LOWER(c.title) like ?");
            parameters.add("%" + token + "%");
            if (indx + 1 < splitter.length) {
                sqlQuery.append(" and");
            }
        }
        sqlQuery.append(" OR ");
        sqlQuery.append(getAssetNameColumn(ASSET_NAME_LIKE.toString()));
        sqlQuery.append(" OR ");
        sqlQuery.append(getBinaryAssetNameColumn(ASSET_NAME_LIKE.toString()));
        sqlQuery.append(" ) ");
        parameters.add("%" + filterText + "%");
        parameters.add("%" + filterText + "%");
    }

    /**
     * Appends the query to filter by filename to the given SQL query and adds the filename to the
     * parameters list.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param fileName   The filename to filter by.
     * @param parameters The list of parameters to add the filename to.
     */
    private void appendFileNameQuery(StringBuilder sqlQuery, String fileName,
            List<Object> parameters) {

        final String matchText = fileName.toLowerCase().trim();
        sqlQuery.append(" and (");
        sqlQuery.append(" LOWER(id.asset_name) = ?");
        sqlQuery.append(" ) ");
        parameters.add(matchText);
    }

    /**
     * Appends the query to filter by show_on_menu flag to the given SQL query. Adds the necessary
     * conditions to the query based on the show_on_menu property
     *
     * @param sqlQuery The StringBuilder object representing the SQL query to be appended.
     */
    private void appendShowOnMenuQuery(StringBuilder sqlQuery) {
        sqlQuery.append(" and c.show_on_menu = ").append(DbConnectionFactory.getDBTrue());
    }

    /**
     * Appends the query to exclude archived content to the given SQL query.
     *
     * @param sqlQuery The StringBuilder object representing the SQL query to be appended.
     */
    private void appendExcludeArchivedQuery(StringBuilder sqlQuery) {
        sqlQuery.append(" and cvi.deleted = ").append(DbConnectionFactory.getDBFalse());
    }

    /**
     * Returns the appropriate column for the {@code Asset Name} field depending on the database that dotCMS is running
     * on. That is, if the value is inside the "Content as JSON" column, or the legacy "text" column.
     *
     * @param baseQuery The base SQL query whose column name will be replaced.
     *
     * @return The appropriate database column for the Asset Name field.
     */
    public static String getAssetNameColumn(final String baseQuery) {
        String sql = baseQuery;
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            if (DbConnectionFactory.isPostgres()) {
                sql = String.format(sql, POSTGRES_ASSETNAME_COLUMN);
            } else {
                sql = String.format(sql, MSSQL_ASSETNAME_COLUMN);
            }
        }
        return sql;
    }
    /**
     * Retrieve the column that corresponds to the {@code Asset Name} of a binary field.
     * The value that is inside the "Content as JSON" column called "asset".
     *
     * @param baseQuery The base SQL query whose column name will be replaced.
     *
     * @return The appropriate database column for the Asset Name field.
     */
    private String getBinaryAssetNameColumn(final String baseQuery){
        String sql = baseQuery;
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            sql = String.format(sql, POSTGRES_BINARY_ASSETNAME_COLUMN);
        }
        return sql;
    }

    private List<Map<String, Object>> includeLinks(final BrowserQuery browserQuery)
            throws DotDataException, DotSecurityException {

        List<Map<String, Object>> returnList = new ArrayList<>();

        for (final Link link : getLinks(browserQuery)) {

            final List<Integer> permissions2 =
                    permissionAPI.getPermissionIdsFromRoles(link, browserQuery.roles, browserQuery.user);

            if (permissions2.contains(PERMISSION_READ)) {

                final Map<String, Object> linkMap = link.getMap();
                linkMap.put("permissions", permissions2);
                linkMap.put("mimeType", "application/dotlink");
                linkMap.put("name", link.getTitle());
                linkMap.put("title", link.getName());
                linkMap.put("description", link.getFriendlyName());
                linkMap.put("extension", "link");
                linkMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(link));
                linkMap.put("statusIcons", UtilHTML.getStatusIcons(link));
                linkMap.put("hasTitleImage", "");
                linkMap.put("__icon__", "linkIcon");
                returnList.add(linkMap);

            }
        }
        return returnList;
    } // includeLinks.


    private List<Link> getLinks(final BrowserQuery browserQuery) throws DotDataException, DotSecurityException {
        if (browserQuery.directParent instanceof Host) {
            return folderAPI.getLinks((Host) browserQuery.directParent,
                    browserQuery.showWorking, browserQuery.showArchived, browserQuery.user,
                    false);
        }

        if (browserQuery.directParent instanceof Folder) {
            return folderAPI
                    .getLinks((Folder) browserQuery.directParent, browserQuery.showWorking, browserQuery.showArchived,
                            browserQuery.user,
                            false);
        }
        return Collections.emptyList();
    }



    private  List<Map<String, Object>> getFolders(final BrowserQuery browserQuery, final Role[] roles) throws DotDataException, DotSecurityException {

        if (browserQuery.directParent != null) {

            List<Folder> folders = Collections.emptyList();
            try {

                folders = folderAPI.findSubFoldersByParent(browserQuery.directParent, userAPI.getSystemUser(),false).stream()
                        .sorted(Comparator.comparing(Folder::getName)).collect(Collectors.toList());

            } catch (Exception e1) {

                Logger.error(this, "Could not load folders : ", e1);
            }


            if(browserQuery.showMenuItemsOnly) {
                folders.removeIf(f->!f.isShowOnMenu());
            }


            final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(folders)
                    .withUserAndRoles(browserQuery.user, roles).build();
            final List<Map<String, Object>> mapViews = transformer.toMaps();
            return mapViews;

        }
        return ImmutableList.of();
    } // getFolders.

    private Map<String,Object> htmlPageMap(final HTMLPageAsset page) throws DotStateException {
        return new DotTransformerBuilder().webAssetOptions().content(page).build().toMaps().get(0);
    } // htmlPageMap.

    private Map<String,Object> fileAssetMap(final FileAsset fileAsset) throws DotStateException {
        return new DotTransformerBuilder().webAssetOptions().content(fileAsset).build().toMaps().get(0);
    } // fileAssetMap.

    private Map<String,Object> dotAssetMap(final Contentlet dotAsset) throws DotStateException {
        return new DotTransformerBuilder().dotAssetOptions().content(dotAsset).build().toMaps().get(0);
    } // dotAssetMap.

    private Map<String, Object> dotContentMap(final Contentlet dotAsset) throws DotStateException {
        return new DotTransformerBuilder().defaultOptions().content(dotAsset).build().toMaps().get(0);
    } // dotAssetMap.


    protected class WfData {

        List<WorkflowAction> wfActions = new ArrayList<>();
        boolean contentEditable = false;
        List<Map<String, Object>> wfActionMapList = new ArrayList<>();
        boolean skip=false;

        public WfData(final Contentlet contentlet, final List<Integer> permissions, final User user, final boolean showArchived)
                throws DotStateException, DotDataException, DotSecurityException {

            if(null==contentlet) {
                return;
            }

            wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user, WorkflowAPI.RenderMode.LISTING);

            if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user) && contentlet.isLocked()) {

                final Optional<String> lockedUserId = APILocator.getVersionableAPI().getLockedBy(contentlet);
                contentEditable = lockedUserId.isPresent() && user.getUserId().equals(lockedUserId.get());
            } else {

                contentEditable = false;
            }

            if (permissions.contains(PERMISSION_READ)) {

                if (!showArchived && contentlet.isArchived()) {
                    skip=true;
                    return;
                }

                final boolean showScheme = wfActions!=null?
                        wfActions.stream().collect(Collectors.groupingBy(WorkflowAction::getSchemeId)).size()>1 : false;

                for (final WorkflowAction action : wfActions) {

                    final WorkflowScheme wfScheme         = APILocator.getWorkflowAPI().findScheme(action.getSchemeId());
                    final Map<String, Object> wfActionMap = new HashMap<>();
                    wfActionMap.put("name", action.getName());
                    wfActionMap.put("id",   action.getId());
                    wfActionMap.put("icon", action.getIcon());
                    wfActionMap.put("assignable",  action.isAssignable());
                    wfActionMap.put("commentable", action.isCommentable() || UtilMethods.isSet(action.getCondition()));
                    if (action.hasMoveActionletActionlet() && !action.hasMoveActionletHasPathActionlet()) {

                        wfActionMap.put("moveable", "true");
                    }

                    final String actionName = Try.of(() -> LanguageUtil.get(user, action.getName())).getOrElse(action.getName());
                    final String schemeName = Try.of(() ->LanguageUtil.get(user,wfScheme.getName())).getOrElse(wfScheme.getName());
                    final String actionNameStr = showScheme? actionName +" ( "+schemeName+" )" : actionName;

                    wfActionMap.put("wfActionNameStr",actionNameStr);
                    wfActionMap.put("hasPushPublishActionlet", action.hasPushPublishActionlet());
                    wfActionMapList.add(wfActionMap);
                }
            }
        }
    } // WfData

}