package com.dotmarketing.business.cache.provider;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public abstract class CacheProvider implements Serializable {

    private static final long serialVersionUID = -2139235480907776009L;

    /**
     * Returns the human readable name for this Cache Provider
     *
     * @return
     */
    public abstract String getName ();

    /**
     * Returns a unique key for this Cache Provider
     *
     * @return
     */
    public abstract String getKey ();

    public abstract void init () throws Exception;

    public abstract void put ( String group, String key, final Object content );

    public abstract Object get ( String group, String key );

    public abstract void remove ( String group, String key );

    public abstract void remove ( String group );

    public abstract void removeAll ();

    public abstract Set<String> getKeys ( String group );

    public abstract Set<String> getGroups ();

    public abstract List<Map<String, Object>> getStats ();

    public abstract void shutdown ();

}