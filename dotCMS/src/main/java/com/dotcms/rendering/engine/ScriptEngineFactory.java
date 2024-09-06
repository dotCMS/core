package com.dotcms.rendering.engine;

import com.dotcms.rendering.js.JsEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Factory provides the Velocity or Javascript engine implementations
 * @author jsanca
 */
public class ScriptEngineFactory {

    public static final String VELOCITY_ENGINE = "velocity";
    public static final String JAVASCRIPT_ENGINE = "javascript";
    private final Map<String, ScriptEngine> instanceMap   = new ConcurrentHashMap<>();
    private final VelocityScriptEngine      defaultEngine = new VelocityScriptEngine();
    private final JsEngine jsScriptEngine = new JsEngine();

    {
        instanceMap.put(VELOCITY_ENGINE, defaultEngine);
        instanceMap.put(JAVASCRIPT_ENGINE, jsScriptEngine);
    }

    private static class SingletonHolder {
        private static final ScriptEngineFactory INSTANCE = new ScriptEngineFactory();
    }
    /**
     * Get the instance.
     * @return ScriptEngineFactory
     */
    public static ScriptEngineFactory getInstance() {

        return ScriptEngineFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    public void addEngine (final String engineName, final ScriptEngine scriptEngine) {

        this.instanceMap.put(engineName, scriptEngine);
    }

    public ScriptEngine getEngine (final String engineName) {

        return this.instanceMap.getOrDefault(engineName, this.defaultEngine);
    }
}
