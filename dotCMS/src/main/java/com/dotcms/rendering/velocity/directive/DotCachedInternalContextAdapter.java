package com.dotcms.rendering.velocity.directive;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.context.ChainedInternalContextAdapter;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.introspection.IntrospectionCacheData;
import com.dotmarketing.util.Logger;

public class DotCachedInternalContextAdapter extends ChainedInternalContextAdapter {

    private final Map<String, Serializable> cachedContext;


    public DotCachedInternalContextAdapter(InternalContextAdapter inner, Map<String, Serializable> cachedContext) {
        super(inner);
        this.cachedContext = cachedContext;
    }


    @Override
    public Object get(String key) {

        return cachedContext.containsKey(key) ? cachedContext.get(key) : super.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null || value == null) {
            return null;
        } else if (value instanceof Serializable) {

            cachedContext.put(key, (Serializable) value);
        } else {
            Logger.warn(getClass(), " DotCachedInternalContextAdapter " + key + " is not serializable");
            cachedContext.put(key, value.toString());
        }

        return super.put(key, value);
    }


    @Override
    public boolean containsKey(Object key) {
        return cachedContext.containsKey(key) || super.containsKey(key);
    }


    @Override
    public Object remove(Object key) {
        Object obj = cachedContext.remove(key);
        Object obj2 = super.remove(key);
        return obj == null ? obj2 : obj;
    }


    @Override
    public Context getInternalUserContext() {
        // TODO Auto-generated method stub
        return super.getInternalUserContext();
    }


    @Override
    public InternalContextAdapter getBaseContext() {
        // TODO Auto-generated method stub
        return super.getBaseContext();
    }


    @Override
    public Object[] getKeys() {
        // TODO Auto-generated method stub
        return super.getKeys();
    }


    @Override
    public void pushCurrentTemplateName(String s) {
        // TODO Auto-generated method stub
        super.pushCurrentTemplateName(s);
    }


    @Override
    public void popCurrentTemplateName() {
        // TODO Auto-generated method stub
        super.popCurrentTemplateName();
    }


    @Override
    public String getCurrentTemplateName() {
        // TODO Auto-generated method stub
        return super.getCurrentTemplateName();
    }


    @Override
    public Object[] getTemplateNameStack() {
        // TODO Auto-generated method stub
        return super.getTemplateNameStack();
    }


    @Override
    public void pushCurrentMacroName(String s) {
        // TODO Auto-generated method stub
        super.pushCurrentMacroName(s);
    }


    @Override
    public void popCurrentMacroName() {
        // TODO Auto-generated method stub
        super.popCurrentMacroName();
    }


    @Override
    public String getCurrentMacroName() {
        // TODO Auto-generated method stub
        return super.getCurrentMacroName();
    }


    @Override
    public int getCurrentMacroCallDepth() {
        // TODO Auto-generated method stub
        return super.getCurrentMacroCallDepth();
    }


    @Override
    public Object[] getMacroNameStack() {
        // TODO Auto-generated method stub
        return super.getMacroNameStack();
    }


    @Override
    public IntrospectionCacheData icacheGet(Object key) {
        // TODO Auto-generated method stub
        return super.icacheGet(key);
    }


    @Override
    public Object localPut(String key, Object value) {
        // TODO Auto-generated method stub
        return super.localPut(key, value);
    }


    @Override
    public void icachePut(Object key, IntrospectionCacheData o) {
        // TODO Auto-generated method stub
        super.icachePut(key, o);
    }


    @Override
    public void setMacroLibraries(List macroLibraries) {
        // TODO Auto-generated method stub
        super.setMacroLibraries(macroLibraries);
    }


    @Override
    public List getMacroLibraries() {
        // TODO Auto-generated method stub
        return super.getMacroLibraries();
    }


    @Override
    public EventCartridge attachEventCartridge(EventCartridge ec) {
        // TODO Auto-generated method stub
        return super.attachEventCartridge(ec);
    }


    @Override
    public EventCartridge getEventCartridge() {
        // TODO Auto-generated method stub
        return super.getEventCartridge();
    }


    @Override
    public void setCurrentResource(Resource r) {
        // TODO Auto-generated method stub
        super.setCurrentResource(r);
    }


    @Override
    public Resource getCurrentResource() {
        // TODO Auto-generated method stub
        return super.getCurrentResource();
    }



}
