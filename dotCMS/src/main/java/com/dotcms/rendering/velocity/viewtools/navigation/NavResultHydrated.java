package com.dotcms.rendering.velocity.viewtools.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONIgnore;
import com.liferay.portal.model.User;

public final class NavResultHydrated extends NavResult{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    final NavResult navResult;
    final transient ViewContext context;

    public NavResultHydrated(final NavResult navResult, final ViewContext context) {
        super(navResult.getUnhydratedNavResult());
        this.navResult = navResult.getUnhydratedNavResult();
        this.context = context;
    }
    @Override
    @JSONIgnore
    public NavResult getUnhydratedNavResult() {
      return this.navResult;
    }
    

    public boolean isActive() {
        if (context != null && UtilMethods.isSet(navResult.getHref())) {
            HttpServletRequest req = (HttpServletRequest) context.getRequest();
            if (req != null) {
                // We exclude the page name from the Request URI so we can check if page's parent
                // object is the real active object
                String reqURI = req.getRequestURI();
                String parentPath = reqURI.substring(0, reqURI.lastIndexOf("/"));
                if (!parentPath.endsWith("/"))
                    // Adding a slash at the end of the path, so it avoids false positives
                    // when two or more paths from the same level starts with the same name
                    parentPath = parentPath + "/";
                // If the current item is a folder, we check if it's part of current URI

                // System.err.println(href + " : " + reqURI);
                if (isFolder() && !navResult.getHref()
                    .endsWith("/")) {
                    String tempHref = navResult.getHref() + "/";
                    return parentPath.startsWith(tempHref);
                } else {
                    // If it's a page, we check instead if it's the current URI
                    return !isCodeLink() && navResult.getHref()
                        .equalsIgnoreCase(reqURI);
                }
            }
        }
        return false;
    }
    @Override
    public String getCodeLink() {
        if (navResult.getCodeLink() != null && (navResult.getCodeLink()
            .contains("$")
                || navResult.getCodeLink()
                    .contains("#"))) {
            return UtilMethods.evaluateVelocity(navResult.getCodeLink(), context.getVelocityContext());
        } else {
            return navResult.getCodeLink();
        }
    }
    @Override
    public String getTitle() throws Exception {
        return navResult.getTitle();
    }

    @Override
    public String getHostId() {
        return navResult.getHostId();
    }

    @Override
    public String getHref() {
        return navResult.getHref();
    }

    @Override
    public boolean isShowOnMenu() {
        return navResult.isShowOnMenu();
    }

    @Override
    public boolean isCodeLink() {
        return navResult.isCodeLink();
    }

    @Override
    public int getOrder() {
        return navResult.getOrder();
    }


    @Override
    public boolean isFolder() {
        return navResult.isFolder();
    }

    @Override
    public List<? extends NavResult> getChildren() throws Exception {


        List<NavResultHydrated> list = this.navResult.getChildren().stream().map(result -> new NavResultHydrated(result, this.context)).collect(Collectors.toList());
        
        
        
        if (Config.getBooleanProperty("ENABLE_NAV_PERMISSION_CHECK", false)) {
            // now filtering permissions
            List<NavResult> allow = new ArrayList<NavResult>(list.size());

            HttpServletRequest req = (HttpServletRequest) context.getRequest();
            User currentUser = WebAPILocator.getUserWebAPI()
                .getLoggedInUser(req);
            if (currentUser == null)
                currentUser = APILocator.getUserAPI()
                    .getAnonymousUser();
            for (NavResult nv : list) {
                try {
                    if (APILocator.getPermissionAPI()
                        .doesUserHavePermission(nv, PermissionAPI.PERMISSION_READ, currentUser)) {
                        allow.add(nv);
                    }
                } catch (Exception ex) {
                    Logger.error(this, ex.getMessage(), ex);
                }
            }
            return allow;
        }
        return list;
    }

    @Override
    public String getParentPath() throws DotDataException, DotSecurityException {
        return navResult.getParentPath();
    }


    @Override
    public NavResult getParent() throws DotDataException, DotSecurityException {
        return navResult.getParent();
    }


    @Override
    public List<String> getChildrenFolderIds() {
        return navResult.getChildrenFolderIds();
    }

    @Override
    public String getType() {
        return navResult.getType();
    }


    @Override
    public String getTarget() {
        return navResult.getTarget();
    }

    @Override
    public String getOwner() {
        return navResult.getOwner();
    }



}
