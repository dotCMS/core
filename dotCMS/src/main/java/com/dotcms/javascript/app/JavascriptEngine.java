package com.dotcms.javascript.app;

import javax.script.*;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class JavascriptEngine {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    /**
     * In case you need to add globals objects to the context
     * @param globalContext {@link Map}
     */
    public void addGlobalObjects (final Map<String, Object> globalContext) {
        // todo: lock me
        final ScriptEngine engine = this.getEngine();
        for (final Map.Entry<String, Object> entry : globalContext.entrySet()) {
            engine.getContext().setAttribute(entry.getKey(), entry.getValue(), ScriptContext.GLOBAL_SCOPE);
        }
    }

    /**
     * In case you want to evaluate a javascript
     * @param script String
     * @return Optional Object the result of the operation
     */
    public Optional<Object> evaluate(final CharSequence script) {

        return this.evaluate(script, Collections.emptyMap());
    }

    public Optional<Object> evaluate(final CharSequence script, final Map<String, Object> context) {

        return this.evaluate(new StringReader(script.toString()), context);
    }

    public Optional<Object> evaluate(final Reader reader) {

        return this.evaluate(reader, Collections.emptyMap());
    }

    public Optional<Object> evaluate(final Reader reader, final Map<String, Object> context) {

        try {

            final ScriptEngine engine = this.getEngine();
            for (final Map.Entry<String, Object> entry : context.entrySet()) {
                engine.getContext().setAttribute(entry.getKey(), entry.getValue(), ScriptContext.ENGINE_SCOPE);
            }
            return Optional.ofNullable(engine.eval(reader));
        } catch (ScriptException e) {
            e.printStackTrace(); // todo handle in a better way
        }

        return Optional.empty();
    }

    public Optional<Object> invokeFunction (final CharSequence script, final String functionName, Object... functionArguments) {

        return this.invokeFunction(new StringReader(script.toString()), functionName, functionArguments);
    }

    public Optional<Object> invokeFunction (final Reader reader, final String functionName, Object... functionArguments) {

        try {

            final ScriptEngine engine = this.getEngine();
            engine.eval(reader);
            final Invocable invocable = (Invocable)engine;
            return Optional.ofNullable(invocable.invokeFunction(functionName, functionArguments));
        } catch (ScriptException | NoSuchMethodException e) {
            e.printStackTrace(); // todo handle in a better way
        }

        return Optional.empty();
    } // invokeFunction.

    protected ScriptEngine getEngine() {

        return this.scriptEngineManager.getEngineByName("nashorn");
    }
}
