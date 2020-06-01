package com.dotcms.browser;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotFolderTransformerBuilder;
import com.dotmarketing.portlets.contentlet.transform.DotMapViewTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Default implementation
 * @author jsanca
 */
public class BrowserAPIImpl implements BrowserAPI {

    private final LanguageAPI langAPI       = APILocator.getLanguageAPI();
    private final UserWebAPI userAPI       = WebAPILocator.getUserWebAPI();
    private final FolderAPI folderAPI     = APILocator.getFolderAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    @Override
    @CloseDBIfOpened
    public Map<String, Object> getFolderContent(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {

        final MutableInt countItems                     = new MutableInt(0);
        final boolean respectFrontEndRoles = false;
        List<Map<String, Object>> returnList = new ArrayList<>();
        final Role[] roles = APILocator.getRoleAPI().loadRolesForUser(browserQuery.user.getUserId()).toArray(new Role[0]);

        // gets folder parent
        final Folder parent = folderAPI.find(browserQuery.hostFolderId, browserQuery.user, respectFrontEndRoles);

        Host host = null;
        if (parent == null) {// If we didn't find a parent folder lets verify if
            // this is a host
            host = APILocator.getHostAPI().find(browserQuery.hostFolderId, browserQuery.user, respectFrontEndRoles);

            if (host == null) {

                Logger.error(this, "Folder ID doesn't belong to a Folder nor a Host, id: " + browserQuery.hostFolderId
                        + ", maybe the Folder was modified in the background.");
                throw new NotFoundInDbException("Folder ID doesn't belong to a Folder nor a Host, id: " + browserQuery.hostFolderId);
            }
        }

        if (browserQuery.showFolders) {

            this.includeFolders(browserQuery, returnList, roles, parent, countItems);
        }

        if (browserQuery.showLinks) {

            this.includeLinks(browserQuery, returnList, roles, parent, host, countItems);
        }

        final String luceneQuery = this.createQuery(browserQuery, parent, host);
        final String esSortBy    = ("name".equals(browserQuery.sortBy) ? "title" : browserQuery.sortBy)
                + (browserQuery.sortByDesc ? " desc" : StringPool.BLANK);

        final PaginatedArrayList<Contentlet> contentlets = (PaginatedArrayList)APILocator.getContentletAPI().search(luceneQuery,
                browserQuery.maxResults + browserQuery.offset, 0, esSortBy, browserQuery.user, true);

        for (final Contentlet contentlet : contentlets) {

            Map<String, Object> contentMap = null;
            if (contentlet.getBaseType().get() == BaseContentType.FILEASSET) {

                final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
                contentMap = fileAssetMap(fileAsset, browserQuery.user, browserQuery.showArchived);
            }

            if (contentlet.getBaseType().get() == BaseContentType.DOTASSET) {

                contentMap = dotAssetMap(contentlet, browserQuery.user, browserQuery.showArchived);
            }

            if (contentlet.getBaseType().get() == BaseContentType.HTMLPAGE) {

                final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
                contentMap = htmlPageMap(page, browserQuery.user, browserQuery.showArchived, browserQuery.languageId);
            }

            final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(contentlet, roles, browserQuery.user);
            final WfData wfdata             = new WfData(contentlet, permissions, browserQuery.user, browserQuery.showArchived);
            contentMap.put("wfActionMapList", wfdata.wfActionMapList);
            contentMap.put("contentEditable", wfdata.contentEditable);
            contentMap.put("permissions",     permissions);
            returnList.add(contentMap);
        }

        // Filtering
        returnList = this.filterReturnList(browserQuery, returnList);

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
        returnMap.put("total", contentlets.getTotalResults() + countItems.getValue());
        returnMap.put("list",  offset > returnList.size()? Collections.emptyList(): returnList.subList(offset, offset + maxResults));
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

    private String createQuery(final BrowserQuery browserQuery, final Folder parent, final Host host) {

        final StringBuilder luceneQuery = new StringBuilder();
        final List<String> baseTypes = new ArrayList<>();

        if(browserQuery.showDotAssets) {

            baseTypes.add(String.valueOf(BaseContentType.DOTASSET.getType()));
        }

        if(browserQuery.showFiles) {

            baseTypes.add(String.valueOf(BaseContentType.FILEASSET.getType()));
        }

        if(browserQuery.showPages) {

            baseTypes.add(String.valueOf(BaseContentType.HTMLPAGE.getType()));
        }

        if(baseTypes.size()==0) {

            baseTypes.add(String.valueOf(BaseContentType.ANY.getType()));
        }

        luceneQuery.append("+basetype:(" + String.join(" OR ", baseTypes) + ") ");
        luceneQuery.append(browserQuery.languageId > 0? " +languageid:" + browserQuery.languageId : StringPool.BLANK);
        luceneQuery.append(host != null?                " +conhost:"    + host.getIdentifier() + " +confolder:" + Folder.SYSTEM_FOLDER : StringPool.BLANK);
        luceneQuery.append(parent != null?              " +confolder:"  + parent.getInode()       : StringPool.BLANK);

        if (UtilMethods.isSet(browserQuery.filter)) {

            final String[] spliter = browserQuery.filter.split(" ");
            for (final String token : spliter) {

                luceneQuery.append(" +title:" + token + "*");
            }
        }

        luceneQuery.append(
                browserQuery.showArchived
                        ? " +(working:true OR deleted:true) "
                        : browserQuery.showWorking
                        ? " +working:true -deleted:true"
                        : " +live:true");
        return luceneQuery.toString();
    }

    private void includeLinks(final BrowserQuery browserQuery, final List<Map<String, Object>> returnList,
                              final Role[] roles, final Folder parent, final Host host, final MutableInt countItems) throws DotDataException, DotSecurityException {
        // Getting the links directly under the parent folder or host
        List<Link> links = new ArrayList<>();

        if (parent != null) {

            if (browserQuery.showWorking) {

                links.addAll(folderAPI.getLinks(parent, true, false, browserQuery.user, false));
            } else {

                links.addAll(folderAPI.getLiveLinks(parent, browserQuery.user, false));
            }

            if (browserQuery.showArchived) {

                links.addAll(folderAPI.getLinks(parent, true, browserQuery.showArchived, browserQuery.user, false));
            }
        } else {

            links = folderAPI.getLinks(host, true, browserQuery.showArchived, browserQuery.user, false);
        }

        for (final Link link : links) {

            final List<Integer> permissions2 = permissionAPI.getPermissionIdsFromRoles(link, roles, browserQuery.user);

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
                countItems.increment();
            }
        }
    } // includeLinks.

    private void includeFolders(final BrowserQuery browserQuery,
                                final List<Map<String, Object>> returnList,
                                final Role[] roles,
                                final Folder parent,
                                final MutableInt countItems) throws DotDataException, DotSecurityException {

        if (parent != null) {

            List<Folder> folders = Collections.emptyList();
            try {

                folders = folderAPI.findSubFolders(parent, userAPI.getSystemUser(), false);
            } catch (Exception e1) {

                Logger.error(this, "Could not load folders : ", e1);
            }

            final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(folders).withUserAndRoles(browserQuery.user, roles).build();
            final List<Map<String, Object>> mapViews = transformer.toMaps();
            countItems.add(mapViews.size());
            returnList.addAll(mapViews);

        }
    } // includeFolders.

    private Map<String,Object> htmlPageMap(final HTMLPageAsset page,
                                           final User user,
                                           final boolean showArchived,
                                           final long languageId) throws DotDataException, DotStateException, DotSecurityException {
        return new DotTransformerBuilder().webAssetOptions().content(page).build().toMaps().get(0);
    } // htmlPageMap.

    private Map<String,Object> fileAssetMap(final FileAsset fileAsset,
                                            final User user,
                                            final boolean showArchived) throws DotDataException, DotStateException, DotSecurityException {
        return new DotTransformerBuilder().webAssetOptions().content(fileAsset).build().toMaps().get(0);
    } // fileAssetMap.

    private Map<String,Object> dotAssetMap(final Contentlet dotAsset,
                                           final User user,
                                           final boolean showArchived) throws DotDataException, DotStateException, DotSecurityException {

        return new DotTransformerBuilder().dotAssetOptions().content(dotAsset).build().toMaps().get(0);
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

                final String lockedUserId = APILocator.getVersionableAPI().getLockedBy(contentlet);
                contentEditable = user.getUserId().equals(lockedUserId);
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
