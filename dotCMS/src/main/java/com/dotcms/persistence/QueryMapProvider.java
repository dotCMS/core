package com.dotcms.persistence;

import java.io.Serializable;
import java.util.Map;

/**
 * A query map provider basically encapsulates the logic to fetch the queries generic and specific vendor queries
 * (such as MySQL, MsSQL, etc) for a class.
 * @author jsanca
 */
public interface QueryMapProvider extends Serializable {

    /**
     * Get the query map for a class, it depends on the strategies and configuration.
     * Usually it will look up the generic queries and them, overrides with the specific vendor.
     * @param persistanceClass Class
     * @return Map
     */
    Map<String, String> getQueryMap (Class persistanceClass);
} // E:O:F:QueryMapProvider.
