package com.dotcms.system;

import com.dotcms.util.CollectionsUtils;

import com.dotmarketing.db.commands.DatabaseCommand.QueryReplacements;
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

    public <T> T getAttribute(Enum attribute) {
        return (T)this.context.get(attribute.toString());
    }

    public <T> void setAttribute(Enum attribute, T attributeValue) {

        this.context.put(attribute.toString(), attributeValue);
    }

    public boolean doNothingOnConflict(){
        return (this.getAttribute(QueryReplacements.DO_NOTHING) != null)
                && ((Boolean)this.getAttribute(QueryReplacements.DO_NOTHING));
    }

} // E:O:F:SimpleMapAppContext.
