package com.dotcms.util;


import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Very simple wrapper of map for http request
 * @author jsanca
 */
public class HttpRequestMapWrapper  implements Map<String, Object>, Serializable {

    private final HttpServletRequest request;

    public HttpRequestMapWrapper(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return this.containsKey(key)?
                get(key):defaultValue;
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object replace(String key, Object value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object computeIfPresent(String key,  BiFunction<? super String, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object merge(String key, Object value,  BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public int size() {
        return request.getParameterMap().size();
    }

    @Override
    public boolean isEmpty() {
        return request.getParameterMap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return null != this.request.getParameter(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object get(Object key) {
        return request.getParameter(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Set<String> keySet() {
        return request.getParameterMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }
}
