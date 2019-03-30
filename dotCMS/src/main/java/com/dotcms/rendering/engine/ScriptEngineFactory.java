package com.dotcms.rendering.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptEngineFactory {

    public static final String VELOCITY_ENGINE = "velocity";
    private final Map<String, ScriptEngine> instanceMap   = new ConcurrentHashMap<>();
    private final VelocityScriptEngine      defaultEngine = new VelocityScriptEngine();

    {
        instanceMap.put(VELOCITY_ENGINE, defaultEngine);
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
