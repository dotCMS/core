package com.dotcms.system;

import com.dotcms.util.CollectionsUtils;

import java.util.Map;

/**
 * Simple Implementation based on a map.
 * @author jsanca
 */
public class SimpleMapAppContext implements AppContext {

    private final Map context = CollectionsUtils.map();

    @Override
    public <T> T getAttribute(String attributeName) {
        return (T)this.context.get(attributeName);
    }

    @Override
    public <T> void setAttribute(String attributeName, T attributeValue) {

        this.context.put(attributeName, attributeValue);
    }
} // E:O:F:SimpleMapAppContext.
