package com.dotcms.concurrent;

import com.dotcms.jmx.DotMBean;

import java.util.Map;

/**
 * Encapsulates the MBean interfaces for the {@link DotConcurrentFactory}
 * @author jsanca
 */
public interface DotConcurrentFactoryMBean extends DotMBean {

    /**
     * Gets the stats for a particular submitter
     * @param name {@link String}
     * @return Map
     */
    public Map<String, Object> getStats (String name);
} // E:O:F:DotConcurrentFactoryMBean.
