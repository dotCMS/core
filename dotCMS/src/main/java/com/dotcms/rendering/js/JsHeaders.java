package com.dotcms.rendering.js;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyIterator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the key value Headers in a post request, in a Js context
 * @author jsanca
 */
public class JsHeaders implements Serializable {

    private final Map<Object, Object> headersMap = new HashMap<>();
    private static final long serialVersionUID = 1L;

    public JsHeaders() {
        super();
    }

    public JsHeaders(final Map<Object, Object> headersMap) {
        super();
        this.headersMap.putAll(headersMap);
    }

    @HostAccess.Export
    public JsHeaders append(final String name, final String value) {
        this.headersMap.put(name, value);
        return this;
    }

    @HostAccess.Export
    public void delete(final String name) {
        this.headersMap.remove(name);
    }

    @HostAccess.Export
    public String get(final String name) {
        final Object result = this.headersMap.get(name);
        return null != result ? result.toString() : null;
    }

    @HostAccess.Export
    public boolean has(final String name) {
        return this.headersMap.containsKey(name);
    }

    @HostAccess.Export
    public JsHeaders set(final String name, final String value) {
        // does not handle multiple values, set is the same of append
        this.append(name, value);
        return this;
    }

    @HostAccess.Export
    public String[] getAll(final String name) {

        return new String[] { this.get(name) };
    }

    @HostAccess.Export
    public String[] keys() {

        return this.headersMap.keySet().toArray(new String[this.headersMap.size()]);
    }

    @HostAccess.Export
    public String[] values() {

        return this.headersMap.values().toArray(new String[this.headersMap.size()]);
    }

    @HostAccess.Export
    public ProxyIterator entries() {
        return ProxyIterator.from(this.headersMap.entrySet().iterator());
    }

}
