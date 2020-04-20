package com.dotcms.rendering.velocity.viewtools.navigation;



import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.google.common.annotations.VisibleForTesting;



import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;

import com.liferay.util.StringPool;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.liferay.portal.model.User;

public class NavTool implements ViewTool {

    private Host currenthost = null;
    private static User systemUser = null;
    private HttpServletRequest request = null;
    private long currentLanguage = 0;
    private ViewContext context;
    @VisibleForTesting
    final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    static {

      systemUser = APILocator.systemUser();

    }

    @Override
    public void init(Object initData) {
        context = (ViewContext) initData;
        try {
            this.request = context.getRequest();
            this.currenthost = WebAPILocator.getHostWebAPI()
                .getCurrentHost(context.getRequest());
            this.currentLanguage = WebAPILocator.getLanguageWebAPI()
                .getLanguage(this.request)
                .getId();
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }

    protected NavResult getNav(Host host, String path) throws DotDataException, DotSecurityException {
        return getNav(host, path, this.currentLanguage, this.systemUser);
    }

    protected NavResultHydrated getNav(Host host, String path, long languageId, User systemUserParam)
            throws DotDataException, DotSecurityException {

        if (path != null && path.contains(".")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }

        final Folder originalFolder = !path.equals("/") ? APILocator.getFolderAPI()
            .findFolderByPath(path, host, systemUserParam, true)
                : APILocator.getFolderAPI()
                    .findSystemFolder();

        if (originalFolder == null || !UtilMethods.isSet(originalFolder.getIdentifier())) {
            return null;
        }

        Folder folder = getDefensiveCopyOfFolder(originalFolder);

        NavResult result = CacheLocator.getNavToolCache()
            .getNav(host.getIdentifier(), folder.getInode(), languageId);
        if (result != null) {

            return new NavResultHydrated(result, this.context);
        } else {
            String parentId;
            if (!folder.getInode()
                .equals(FolderAPI.SYSTEM_FOLDER)) {
                Identifier ident = APILocator.getIdentifierAPI()
                    .find(folder);
                parentId = ident.getParentPath()
                    .equals("/") ? FolderAPI.SYSTEM_FOLDER
                            : APILocator.getFolderAPI()
                                .findFolderByPath(ident.getParentPath(), host, systemUserParam, false)
                                .getInode();
            } else {
                //set hostId to systemfolder, so when looking items only brings items of the selected site
                folder.setHostId(host.getIdentifier());
                parentId = null;
            }
            result = new NavResult(parentId, host.getIdentifier(), folder.getInode(), languageId);

            Identifier ident = APILocator.getIdentifierAPI()
                .find(folder);
            result.setHref(ident.getURI());
            result.setTitle(folder.getTitle());
            result.setOrder(folder.getSortOrder());
            result.setType("folder");
            result.setPermissionId(folder.getPermissionId());
            List<NavResult> children = new ArrayList<NavResult>();
            List<String> folderIds = new ArrayList<String>();
            result.setChildren(children);
            result.setChildrenFolderIds(folderIds);
            result.setShowOnMenu(folder.isShowOnMenu());

            List<?> menuItems = APILocator.getFolderAPI()
                    .findMenuItems(folder, systemUserParam, true);
            List<Folder> folders = new ArrayList<>();
            if (path.equals("/")) {
                folders.addAll(APILocator.getFolderAPI()
                        .findSubFolders(host, true));
            }

            for (Folder itemFolder : folders) {
                addFolderToNav(host, languageId, folder, children, folderIds, itemFolder);
            }


            for (Object item : menuItems) {
                if (item instanceof Folder) {
                    Folder itemFolder = (Folder) item;
                    if(!folders.contains(itemFolder)) {
                        addFolderToNav(host, languageId, folder, children, folderIds, itemFolder);
                    }
                } else if (item instanceof IHTMLPage) {
                    IHTMLPage itemPage = (IHTMLPage) item;

                    if (itemPage.getLanguageId() == languageId || shouldAddHTMLPageInAnotherLang(menuItems, itemPage, languageId)){
                        final String httpProtocol = "http://";
                        final String httpsProtocol = "https://";

                        ident = APILocator.getIdentifierAPI()
                            .find(itemPage);

                        String redirectUri = itemPage.getRedirect();
                        NavResult nav =  new NavResult(folder.getInode(), host.getIdentifier(), itemPage.getLanguageId());

                        nav.setTitle(itemPage.getTitle());
                        if (UtilMethods.isSet(redirectUri) && !redirectUri.startsWith("/")) {
                            if (redirectUri.startsWith(httpsProtocol) || redirectUri.startsWith(httpProtocol)) {
                                nav.setHref(redirectUri);
                            } else {
                                if (itemPage.isHttpsRequired())
                                    nav.setHref(httpsProtocol + redirectUri);
                                else
                                    nav.setHref(httpProtocol + redirectUri);
                            }

                        } else {
                            nav.setHref(ident.getURI());
                        }
                        nav.setOrder(itemPage.getMenuOrder());
                        nav.setType("htmlpage");
                        nav.setPermissionId(itemPage.getPermissionId());
                        nav.setShowOnMenu(itemPage.isShowOnMenu());
                        children.add(nav);
                    }
                } else if (item instanceof Link) {
                    Link itemLink = (Link) item;
                    NavResult nav = new NavResult(folder.getInode(), host.getIdentifier(), languageId);

                    if (itemLink.getLinkType()
                        .equals(LinkType.CODE.toString()) && LinkType.CODE.toString() != null) {
                        nav.setCodeLink(itemLink.getLinkCode());
                    } else {
                        nav.setHref(itemLink.getWorkingURL());
                    }
                    nav.setTitle(itemLink.getTitle());
                    nav.setOrder(itemLink.getSortOrder());
                    nav.setType("link");
                    nav.setTarget(itemLink.getTarget());
                    nav.setPermissionId(itemLink.getPermissionId());
                    nav.setShowOnMenu(itemLink.isShowOnMenu());
                    children.add(nav);
                } else if (item instanceof IFileAsset) {
                    IFileAsset itemFile = (IFileAsset) item;

                    if ( itemFile.getLanguageId() == languageId || shouldAddFileInAnotherLang(menuItems, itemFile, languageId)) {
                        ident = APILocator.getIdentifierAPI()
                            .find(itemFile.getPermissionId());
                        NavResult nav = new NavResult(folder.getInode(), host.getIdentifier(), itemFile.getLanguageId());

                        nav.setTitle(itemFile.getFriendlyName());
                        nav.setHref(ident.getURI());
                        nav.setOrder(itemFile.getMenuOrder());
                        nav.setType("file");
                        nav.setPermissionId(itemFile.getPermissionId());
                        nav.setShowOnMenu(itemFile.isShowOnMenu());
                        children.add(nav);
                    }
                }
            }

            CacheLocator.getNavToolCache()
                .putNav(host.getIdentifier(), folder.getInode(), result, languageId);

            return new NavResultHydrated(result, this.context);
        }
    }

    /**
     * Makes a defensive copy of the given folder to avoid mutating cached version
     * @param originalFolder folder to make the defensive copy from
     * @return defensive copy
     */
    private Folder getDefensiveCopyOfFolder(Folder originalFolder) {
        Folder folder = new Folder();
        try {
            BeanUtils.copyProperties(folder, originalFolder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            folder = originalFolder;
            Logger.warnAndDebug(NavTool.class,"Defensive copy failed. Using original object", e);
        }
        return folder;
    }

    private void addFolderToNav(Host host, long languageId, Folder folder, List<NavResult> children,
            List<String> folderIds, Folder itemFolder) throws DotDataException {
        Identifier ident;
        ident = APILocator.getIdentifierAPI()
                .find(itemFolder);
        NavResult nav = new NavResult(folder.getInode(), host.getIdentifier(),
                itemFolder.getInode(), languageId);

        nav.setTitle(itemFolder.getTitle());
        nav.setHref(ident.getURI());
        nav.setOrder(itemFolder.getSortOrder());
        nav.setType("folder");
        nav.setPermissionId(itemFolder.getPermissionId());
        nav.setShowOnMenu(itemFolder.isShowOnMenu());

        // it will load lazily its children
        folderIds.add(itemFolder.getInode());
        children.add(nav);
    }

    @VisibleForTesting
    boolean shouldAddHTMLPageInAnotherLang(List<?> menuItems, IHTMLPage itemPage, long languageId) {
        return (APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage() &&
            itemPage.getLanguageId()  == defaultLanguage &&
            !doesHTMLPageInRequestedLanguageExists(menuItems,itemPage.getIdentifier(),languageId));
    }

    @VisibleForTesting
    boolean shouldAddFileInAnotherLang(final List<?> menuItems, final IFileAsset itemFile, final long languageId) {
        return (APILocator.getLanguageAPI().canDefaultFileToDefaultLanguage() &&
                itemFile.getLanguageId() == defaultLanguage &&
                !doesFileAssetInRequestedLanguageExists(menuItems, itemFile.getPermissionId(), languageId));
    }

    /**
     * Pass the level of the nav you wish to retrieve, based on the current path, level 0 being the
     * root
     * 
     * @param level
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public NavResult getNav(int level) throws DotDataException, DotSecurityException {
        if (level < 1)
            return getNav("/");


        String reqPath = getNav().getHref();

        String[] levels = reqPath.split("/");


        if (level + 1 > levels.length)
            return null;

        StringBuffer sw = new StringBuffer();

        for (int i = 1; i <= level; i++) {
            sw.append("/");
            sw.append(levels[i]);
        }
        String path = sw.toString();

        return getNav(path);
    }

    public NavResult getNav() throws DotDataException, DotSecurityException {
        return getNav(request.getRequestURI().replace("/api/v1/page/render/", StringPool.SLASH));
    }

    public NavResult getNav(String path) throws DotDataException, DotSecurityException {
        if(path.contains("/api/v1/containers")){
            final String folderInode = ((BrowserAjax)this.request.getSession().getAttribute("BrowserAjax")).getActiveFolderInode();
            final String folderIdentifier = APILocator.getFolderAPI().find(folderInode,systemUser,false).getIdentifier();
            path = APILocator.getIdentifierAPI().find(folderIdentifier).getPath();
        }

        Host host = getHostFromPath(path);

        if (host == null)
            host = currenthost;

        return getNav(host, path);
    }

    public NavResult getNav(String path, long languageId) throws DotDataException, DotSecurityException {

        Host host = getHostFromPath(path);

        if (host == null)
            host = currenthost;

        return getNav(host, path, languageId, systemUser);
    }

    private Host getHostFromPath(String path) throws DotDataException, DotSecurityException {
        if (path.startsWith("//")) {
            List<RegExMatch> find = RegEX.find(path, "^//(\\w+)/(.+)");
            if (find.size() == 1) {
                String hostname = find.get(0)
                    .getGroups()
                    .get(0)
                    .getMatch();
                path = "/" + find.get(0)
                    .getGroups()
                    .get(1)
                    .getMatch();
                return APILocator.getHostAPI()
                    .findByName(hostname, systemUser, false);
            }
        }

        return null;
    }

    private boolean doesHTMLPageInRequestedLanguageExists(final List<?> items, final String identifier, final long language){
        return items.stream().anyMatch((item)->(item instanceof IHTMLPage
            && ((IHTMLPage) item).getIdentifier().equals(identifier) && ((IHTMLPage) item).getLanguageId() == language));
    }

    private boolean doesFileAssetInRequestedLanguageExists(final List<?> items, final String identifier, final long language){
        return items.stream().anyMatch((item)->(item instanceof IFileAsset
            && ((IFileAsset) item).getPermissionId().equals(identifier) && ((IFileAsset) item).getLanguageId() == language));
    }

}
