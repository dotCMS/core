package com.dotcms.concurrent;

import com.dotcms.jmx.DotMBean;

import java.util.List;
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

    /**
     * Shuts down a particular submitter
     * @param name
     * @return boolean
     */
    public Boolean shutdown(String name);

    /**
     * Shutdown and Destroy all the concurrent stuff
     * such ast thread pools, queues, etc.
     */
    public void shutdownAndDestroy();

    /**
     * Returns a list of all the available Thread Pools at the time.
     * @return
     */
    public List<String> list();

} // E:O:F:DotConcurrentFactoryMBean.
