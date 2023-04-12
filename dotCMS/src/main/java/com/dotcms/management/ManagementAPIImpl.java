package com.dotcms.management;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class ManagementAPIImpl implements ManagementAPI{

    
    private final String managementHost;
    
    @VisibleForTesting
    public ManagementAPIImpl(String managementHost) {
        this.managementHost = managementHost ;

    }
    
    public ManagementAPIImpl() {
        this(resolveManagementHost());
        
        
    }
    
    private static final String resolveManagementHost() {
        final String portalUrlString = APILocator.getCompanyAPI().getDefaultCompany().getPortalURL().contains("://")
                        ? APILocator.getCompanyAPI().getDefaultCompany().getPortalURL()
                        : "https://" + APILocator.getCompanyAPI().getDefaultCompany().getPortalURL();


                    URL portalUrl = Try.of(() -> new URL(portalUrlString)).getOrElseThrow(DotRuntimeException::new);

        return portalUrl.getHost().toLowerCase();
    }
    
    // Default list of disallowed paths
    private static final String[] MANAGEMENT_REQUEST_REQUIRED_URIS = {
            "/html/",
            "/c/",
            "/servlets/",
            "/categoriesservlet/",
            "/dwr/",
            "/dotajaxdirector",
            "/dotscheduledjobs",
            "/dotadmin/",
            "/jsontags/",
            "/edit/",
            "/servlet/"};



    // Default list of Allowed Domains
    private static final String[] MANAGEMENT_REQUEST_DOMAINS = {
            "dotcms.com",
            "dotcms.site",
            "dotcms.io",
            "dotcms.host",
            "dotcms.cloud",
            "dotcmscloud.com"};


    private String[] resolveManagementUris() {
        Set<String> uris = new HashSet<>(Arrays.asList(Config.getStringArrayProperty("MANAGEMENT_REQUEST_REQUIRED_URIS", MANAGEMENT_REQUEST_REQUIRED_URIS)));
        
        uris.addAll(Arrays.asList(Config.getStringArrayProperty("EXTRA_MANAGEMENT_REQUEST_REQUIRED_URIS", new String[]{})));
        
        return uris.toArray(new String[uris.size()]);

    }
    
    private String[] resolveManagementHosts() {
        Set<String> allowedHosts = new HashSet<>(
                        Arrays.asList(Config.getStringArrayProperty("MANAGEMENT_REQUEST_DOMAINS", MANAGEMENT_REQUEST_DOMAINS)));

        allowedHosts.addAll(Arrays.asList(Config.getStringArrayProperty("EXTRA_MANAGEMENT_REQUEST_DOMAINS", new String[]{})));
        
        allowedHosts.add(managementHost);
        
        return allowedHosts.toArray(new String[allowedHosts.size()]);

    }
    
    
    
    
    private final Lazy<String[]> managementUris = Lazy.of(this::resolveManagementUris);
    private final Lazy<String[]> managementHosts = Lazy.of(this::resolveManagementHosts);
    
    @Override
    public String[] getManagementUris() {
        return managementUris.get();
    }
    
    
    @Override
    public String[] getManagementHosts() {
        return managementHosts.get();
    }

    @Override 
    public boolean managementHostRequired(HttpServletRequest request) {
        return managementHostRequired(request.getRequestURI());
    }
    
    @Override 
    public boolean managementHostRequired(String uri) {

         uri = UtilMethods.isSet(uri) ? uri.toLowerCase() : "";

        for (String test : this.getManagementUris()) {
            if (uri.startsWith(test)) {
                return true;
            }
        }
        return false;
    }

    @Override 
    public boolean isManagementHost(HttpServletRequest request) {

        if (!request.isSecure()) {
            Logger.warn(getClass(),
                            "Requests to the dotCMS backend can only be made through a port marked secure, e.g. 8082 or 8443");
            return false;
            
        }


        final String urlString = request.getRequestURL().toString();

        URL url = Try.of(() -> new URL(urlString)).getOrElseThrow(DotRuntimeException::new);

        for (String managementDomain : this.getManagementHosts()) {
            if (url.getHost().endsWith(managementDomain)) {
                request.setAttribute(WebKeys.MANAGEMENT_REQUEST_VALIDATED, true);
                return true;
            }
        }
        Logger.warn(getClass(),
                        url.getHost() + " is not a valid dotCMS management host.  Please set your management host by adding the env varible DOT_ADMIN_PORTAL_URL e.g. DOT_ADMIN_PORTAL_URL=https://testing.dotcms.cloud");

        return false;
    }

    
}
