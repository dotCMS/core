package com.dotcms.rest.api.v1.apps.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewStack {

    private static final ThreadLocal<StackContext> threadLocal = new ThreadLocal<>();

    static void createStack(final Map<String, Object> app) {

        threadLocal.set(new StackContext(app));
    }

    static StackContext getCurrentStack() {
        return threadLocal.get();
    }

    static void pushSite(final String siteId, Map<String, Object> map) {
        final StackContext currentStack = getCurrentStack();
        if(null == currentStack){
            throw new IllegalStateException("Create stack hasn't been called yet.");
        }
        currentStack.currentSite = siteId;
        currentStack.sites.put(siteId, map);
    }

    static void pushSecret(final Map<String, Object> map) {
        final StackContext currentStack = getCurrentStack();
        if(null == currentStack){
            throw new IllegalStateException("Create stack hasn't been called yet.");
        }
        if (null == currentStack.currentSite) {
            throw new IllegalStateException(
                    "Site must be pushed prior to pushing a secret.");
        }
        currentStack.secretsBySite.computeIfAbsent(currentStack.currentSite, k -> new ArrayList<>()).add(map);
    }

    static void dispose() {
        threadLocal.remove();
    }

    public static class StackContext {

        final Map<String, List<Map<String,Object>>> secretsBySite = new HashMap<>();
        final Map<String, Map> sites = new HashMap<>();

        final Map<String, Object> app;

        String currentSite;

        StackContext(final Map<String, Object> app) {
            this.app = app;
        }

    }

}
