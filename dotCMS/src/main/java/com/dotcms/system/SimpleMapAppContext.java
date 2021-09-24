package com.dotcms.system;

import com.dotcms.util.CollectionsUtils;

import com.dotmarketing.db.commands.DatabaseCommand.QueryReplacements;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;

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
        return (this.getAttribute(QueryReplacements.DO_NOTHING_ON_CONFLICT) != null)
                && ((Boolean)this.getAttribute(QueryReplacements.DO_NOTHING_ON_CONFLICT));
    }

    public void setColumnFormatFunctions(final Map<String, Function<String, String>> columnFormatFunctions){
        this.setAttribute(QueryReplacements.COLUMNS_FORMAT, columnFormatFunctions);
    }

    public Map<String, Function<String, String>> getColumnsFormatFunction() {
        final Object columns = this.getAttribute(QueryReplacements.COLUMNS_FORMAT);
        return columns != null ? (Map<String, Function<String, String>>) columns
                : ImmutableMap.of();
    }

} // E:O:F:SimpleMapAppContext.
