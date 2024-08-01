package com.dotcms.browser;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
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
import io.vavr.control.Try;

import java.util.*;
import java.util.stream.Collectors;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

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

    private static final StringBuilder POSTGRES_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("->> 'title' ");

    private static final StringBuilder MSSQL_ASSETNAME_COLUMN = new StringBuilder("JSON_VALUE(c.").append
            (ContentletJsonAPI.CONTENTLET_AS_JSON).append(", '$.title");

    private static final StringBuilder ASSET_NAME_LIKE = new StringBuilder().append("LOWER(%s) LIKE ? ");

    private static final StringBuilder POSTGRES_BINARY_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("-> 'fields' -> ").append("'asset' ->> 'value' ");

    private final String OR = " OR ";

    /**
     * Returns a collection of contentlets based on diff attributes of the BrowserQuery
     * e.g folder, host, archived, baseTypes, language.
     * After that filters the list of contentlets based on permissions.
     * @param browserQuery {@link BrowserQuery}
     * @return list of contentlets
     */
    @Override
    @CloseDBIfOpened
    public List<Contentlet> getContentUnderParentFromDB(final BrowserQuery browserQuery) {

        final Tuple2<String, List<Object>> sqlQuery = this.selectQuery(browserQuery);


        final DotConnect dc = new DotConnect().setSQL(sqlQuery._1);

        sqlQuery._2.forEach(o -> dc.addParam(o));

        try {
            final List<Map<String,String>> inodesMapList =  dc.loadResults();

            final List<String> inodes = new ArrayList<>();
            for (final Map<String, String> versionInfoMap : inodesMapList) {
                inodes.add(versionInfoMap.get("inode"));
            }

            final List<Contentlet> cons  = APILocator.getContentletAPI().findContentlets(inodes);

            return permissionAPI.filterCollection(cons,
                    PermissionAPI.PERMISSION_READ, true, browserQuery.user);

        } catch (Exception e) {
            Logger.warnAndDebug(getClass(), "Unable to load browser" + e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Returns a collection of contentlets, folders, links based on diff attributes of the BrowserQuery
     * @param browserQuery {@link BrowserQuery}
     * @return list of treeable (folders, content, links)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    @CloseDBIfOpened
    public List<Treeable> getFolderContentList(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {

        final List<Treeable> returnList = new ArrayList<>();

        final List<Contentlet> contentlets = browserQuery.showContent ? getContentUnderParentFromDB(browserQuery)
                : Collections.emptyList();
        returnList.addAll(contentlets);

        if (browserQuery.showFolders) {
            List<Folder> folders = folderAPI.findSubFoldersByParent(browserQuery.directParent, userAPI.getSystemUser(), false);
            if (browserQuery.showMenuItemsOnly) {
                folders.removeIf(folder -> !folder.isShowOnMenu());
            }
            folders.forEach(folder -> returnList.add(folder));
        }

        if (browserQuery.showLinks) {
            List<Link> links = this.getLinks(browserQuery);
            if(browserQuery.showMenuItemsOnly){
                links.removeIf(link -> !link.isShowOnMenu());
            }
            links.forEach(link -> returnList.add(link));
        }

        return permissionAPI.filterCollection(returnList,PERMISSION_READ,true, browserQuery.user);
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

        final List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>>();
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

    private Tuple2<String, List<Object>> selectQuery(final BrowserQuery browserQuery) {

        final String workingLiveInode = browserQuery.showWorking || browserQuery.showArchived ? "working_inode" : "live_inode";

        final boolean showAllBaseTypes = browserQuery.baseTypes.contains(BaseContentType.ANY);
        final List<Object> parameters = new ArrayList<>();

        final StringBuilder sqlQuery = new StringBuilder("select cvi." + workingLiveInode + " as inode "
                + " from contentlet_version_info cvi, identifier id, structure struc, contentlet c "
                + " where cvi.identifier = id.id and struc.velocity_var_name = id.asset_subtype and  "
                + " c.inode = cvi." + workingLiveInode);

        if (!showAllBaseTypes) {
            List<String> baseTypes =
                    browserQuery.baseTypes.stream().map(t -> String.valueOf(t.getType())).collect(Collectors.toList());
            sqlQuery.append(" and struc.structuretype in (" + String.join(" , ", baseTypes) + ") ");
        }
        if (browserQuery.languageId > 0) {
            sqlQuery.append(" and cvi.lang in (" + browserQuery.languageId);

            final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            if(browserQuery.showDefaultLangItems && browserQuery.languageId != defaultLang){
                sqlQuery.append("," + defaultLang);
            }

            sqlQuery.append(")");
        }
        if (browserQuery.site != null) {
            sqlQuery.append(" and id.host_inode = ? ");
            parameters.add(browserQuery.site.getIdentifier());
        }
        if (browserQuery.folder != null) {
            sqlQuery.append(" and id.parent_path=? ");
            parameters.add(browserQuery.folder.getPath());
        }

        if (UtilMethods.isSet(browserQuery.filter)) {
            final String filterText = browserQuery.filter.toLowerCase().trim();
            final String[] spliter = filterText.split(" ");

            sqlQuery.append(" and (");
            for (int indx = 0; indx < spliter.length; indx++) {
                final String token = spliter[indx];
                if(token.equals(StringPool.BLANK)){
                    continue;
                }
                sqlQuery.append(" LOWER(c.text2) like ?");
                parameters.add("%" + token + "%");
                if(indx + 1 < spliter.length){
                    sqlQuery.append(" and");
                }
            }

            sqlQuery.append(OR);
            sqlQuery.append(getAssetNameColumn(ASSET_NAME_LIKE.toString()));
            sqlQuery.append(" OR ");
            sqlQuery.append(getBinaryAssetNameColumn(ASSET_NAME_LIKE.toString()));
            sqlQuery.append(" ) ");

            parameters.add("%" + filterText + "%");
            parameters.add("%" + filterText + "%");
        }

        if(browserQuery.showMenuItemsOnly) {
            sqlQuery.append(" and c.show_on_menu = " + DbConnectionFactory.getDBTrue());
        }

        if (!browserQuery.showArchived) {
            sqlQuery.append(" and cvi.deleted = " + DbConnectionFactory.getDBFalse());
        }

        return Tuple.of(sqlQuery.toString(), parameters);
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
