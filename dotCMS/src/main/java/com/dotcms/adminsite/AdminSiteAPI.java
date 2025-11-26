package com.dotcms.adminsite;


import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public interface AdminSiteAPI {


    boolean isAdminSite(String host);

    boolean isAdminSite(HttpServletRequest request);

    boolean isAdminSiteUri(String uri);

    boolean isAdminSiteUri(HttpServletRequest request);

    boolean isAdminAllowed(HttpServletRequest request);


    Map<String,String> getAdminSiteHeaders();


}
