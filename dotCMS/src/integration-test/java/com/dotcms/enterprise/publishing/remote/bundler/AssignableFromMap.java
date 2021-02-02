package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class AssignableFromMap<T> {

    private Map<Class, T> map;

    public AssignableFromMap() {
        map = new LinkedHashMap<>();
    }


    public void put(final Class clazz, final T value) {
        map.put(clazz, value);
    }


    public T get(final Class clazzToFind){
        return get(clazzToFind, null);
    }

    public T get(final Class clazzToFind, final T defaultValue) {
        try {
            final Class key = getKey(clazzToFind);
            return this.map.get(key);
        }catch (NotFoundException e) {
            if (defaultValue == null) {
                throw new IllegalArgumentException("Invalid Asset Type: " + clazzToFind);
            } else {
                return defaultValue;
            }
        }
    }

    public Class getKey(final Class clazzToFind){

        Class couldReturn = null;

        for (Class clazz : map.keySet()) {
            if (clazz.equals(clazzToFind)) {
                return clazz;
            }

            if (clazz.isAssignableFrom(clazzToFind)) {
                couldReturn = clazz;
            }
        }

        if (couldReturn != null) {
            return couldReturn;
        }

        throw new NotFoundException("Invalid Asset Type: "+ clazzToFind);
    }

    public Set<Class> keySet() {
        return map.keySet();
    }

    public void addOrUpdate(Class<?> aClass, T startValue, Function<T, T> updateFunction) {
        T oldValue = null;

        try {
            final Class key = getKey(aClass);

            if (!Contentlet.class.equals(key) || aClass.equals(key)) {
                oldValue = get(aClass);
            }
        } catch (NotFoundException e) {
            //ignore
        }

        final T newValue;

        newValue = (oldValue == null) ? startValue : updateFunction.apply(oldValue);
        map.put(aClass, newValue);
    }
}
