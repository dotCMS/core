package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.velocity.viewtools.dotcache.DotCacheTool;
import com.dotcms.util.CollectionsUtils;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Map;

/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.dotcache.DotCacheTool} (categories) into the JS context.
 *
 * @author jsanca
 */
public class CacheJsViewTool implements JsViewTool{

    private final DotCacheTool dotCacheTool = new DotCacheTool();

    @Override
    public SCOPE getScope() {
        return SCOPE.APPLICATION;
    }

    @Override
    public String getName() {
        return "dotcache";
    }

    @HostAccess.Export
    /**
     * Gets a given Serializable object cached in memory based on the specified key.
     *
     * @param key The key associated to the cached value.
     *
     * @return The cached object.
     */
    public Object get(final String key) {
        return JsProxyFactory.createProxy(this.dotCacheTool.get(key));
    }

    @HostAccess.Export
    /**
     * Puts a given value in the cache memory based on the specified key.
     *
     * @param key   The key associated to a given object.
     * @param value The object being cached.
     */
    public void put(final String key, final Object value) {

        final Object unwrapValue = JsProxyFactory.unwrap(value);
        if (null != unwrapValue) {

            this.dotCacheTool.put(key, (unwrapValue instanceof Serializable)? unwrapValue:
                    makeSerializable(unwrapValue));
        }
    }

    private Serializable makeSerializable(final Object unwrapValue) {

        if (unwrapValue instanceof Map && !(unwrapValue instanceof Serializable)) {

            return CollectionsUtils.toSerializableMap((Map)unwrapValue);
        }

        return unwrapValue instanceof Serializable? (Serializable)unwrapValue: unwrapValue.toString();
    }


    @HostAccess.Export
    /**
     * Puts a given value in the cache memory based on the specified key for a specific amount of time.
     *
     * @param key   The key associated to a given object.
     * @param value The object being cached.
     * @param ttl   The Time-To-Live for this entry.
     */
    public void put(final String key, final Object value, final int ttl) {
        this.dotCacheTool.put(key, JsProxyFactory.unwrap(value), ttl);
    }

    @HostAccess.Export
    /**
     * This puts into the cache once a second
     * @param key
     * @param value
     * @param ttl
     */
    public void putDebounce(final String key, final Object value, final int ttl) {
        this.dotCacheTool.putDebounce(key, JsProxyFactory.unwrap(value), ttl);
    }

    @HostAccess.Export
    /**
     * Removes an object from the cache memory based on its key.
     *
     * @param key The key matching a specific object.
     */
    public void remove(final String key) {
        this.dotCacheTool.remove(key);
    }

    @HostAccess.Export
    /**
     * Clears all objects from the cache memory associated to this ViewTool.
     */
    public void clear() {
        this.dotCacheTool.clear();
    }
}
