package com.dotcms.elasticsearch.plugin;

import java.util.Collection;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

public class RelationshipSortOrderPlugin extends Plugin implements ScriptPlugin {
    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new RelationshipSortOrderScriptFactory();
    }
}