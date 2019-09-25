package com.dotcms.personalization.query;

import javax.servlet.http.HttpServletRequest;

public interface QueryPersonalizer {

    
    /**
     * takes a content query and adds personalization to it based
     * on a variety of factors,including visitor, tags and personas
     * @param query
     * @param request
     * @return
     */
    public String addPersonalizationToQuery(final String query, HttpServletRequest request);
    
    
    
    
    
    
    
}
