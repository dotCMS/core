package com.dotcms.management;

import javax.servlet.http.HttpServletRequest;

public interface ManagementAPI {

    String[] getManagementUris();

    String[] getManagementHosts();

    boolean managementHostRequired(String uri);
    
    boolean managementHostRequired(HttpServletRequest request);

    boolean isManagementHost(HttpServletRequest request);
    
    
    
    
}
