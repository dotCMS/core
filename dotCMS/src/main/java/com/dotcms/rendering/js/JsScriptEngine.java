package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.engine.ScriptEngine;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.Invocable;
import java.io.IOException;
import org.graalvm.polyglot.Source;

/**
 * Js script engine implementation
 * @author jsanca
 */
public class JsScriptEngine implements ScriptEngine {
    @Override
    public Object eval(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final Reader scriptReader,
                       final Map<String, Object> contextParams) {
        // todo: this should be implemented by graal js

        final DotJSON dotJSON = new DotJSON();
        try (Context context = Context.create()) {

            final Source source = Source.newBuilder("js", scriptReader,
                            "sample.js") // todo: we do not have this name
                    .build();
            final Value bindings = context.getBindings("js");
            // todo: put here the view tools and everything else
            for (final Map.Entry<String, Object> entry : contextParams.entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }
            bindings.putMember("dotJSON", dotJSON);
            final Value eval = context.eval(source).build();
            final Value result = eval.execute();
            return CollectionsUtils.map("output", result.asString(), "dotJSON", dotJSON);
        }
    }
}
