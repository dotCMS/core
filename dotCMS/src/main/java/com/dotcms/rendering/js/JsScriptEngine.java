package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

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

        final DotJSON dotJSON = new DotJSON();
        try (Context context = Context.create()) {

            final Object fileName = contextParams.getOrDefault("dot:jsfilename", "sample.js");
            final Source source   = Source.newBuilder("js", scriptReader, fileName.toString()).build();
            final Value bindings  = context.getBindings("js");
            contextParams.entrySet().forEach(entry -> bindings.putMember(entry.getKey(), entry.getValue()));
            this.addTools(request, response, bindings, contextParams);

            bindings.putMember("dotJSON", dotJSON);
            bindings.putMember("request", request);
            bindings.putMember("response", response);
            final Value eval   = context.eval(source);
            final Value result = eval.execute();
            return CollectionsUtils.map("output", result.asString(), "dotJSON", dotJSON);
        } catch (final IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void addTools(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final Value bindings,
                          final Map<String, Object> contextParams) {


    }
}
