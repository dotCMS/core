package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.js.fetch.JsFetch;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.js.proxy.JsRequest;
import com.dotcms.rendering.js.proxy.JsResponse;
import com.dotcms.rendering.js.viewtools.CacheJsViewTool;
import com.dotcms.rendering.js.viewtools.CategoriesJsViewTool;
import com.dotcms.rendering.js.viewtools.ContainerJsViewTool;
import com.dotcms.rendering.js.viewtools.ContentJsViewTool;
import com.dotcms.rendering.js.viewtools.LanguageJsViewTool;
import com.dotcms.rendering.js.viewtools.SecretJsViewTool;
import com.dotcms.rendering.js.viewtools.SiteJsViewTool;
import com.dotcms.rendering.js.viewtools.TagJsViewTool;
import com.dotcms.rendering.js.viewtools.TemplateJsViewTool;
import com.dotcms.rendering.js.viewtools.UserJsViewTool;
import com.dotcms.rendering.js.viewtools.WorkflowJsViewTool;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import io.vavr.control.Try;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Js script engine implementation
 * @author jsanca
 */
public class JsEngine implements ScriptEngine {

    private final Map<String, Class> jsRequestViewToolMap = new ConcurrentHashMap<>();
    private final Map<String, JsViewTool> jsAplicationViewToolMap = new ConcurrentHashMap<>();

    {
        try {
            this.addJsViewTool(UserJsViewTool.class);
            this.addJsViewTool(LanguageJsViewTool.class);
            this.addJsViewTool(SecretJsViewTool.class);
            this.addJsViewTool(CategoriesJsViewTool.class);
            this.addJsViewTool(ContentJsViewTool.class);
            this.addJsViewTool(WorkflowJsViewTool.class);
            this.addJsViewTool(SiteJsViewTool.class);
            this.addJsViewTool(TagJsViewTool.class);
            this.addJsViewTool(TemplateJsViewTool.class);
            this.addJsViewTool(ContainerJsViewTool.class);
            this.addJsViewTool(CacheJsViewTool.class);
        }catch (Throwable e) {
            Logger.error(JsEngine.class, "Could not start the js view tools", e);
        }
    }

    private static final String ENGINE_JS = "js";
    private final static JsDotLogger JS_DOT_LOGGER = new JsDotLogger();

    /**
     * Add a JsViewTool to the engine
     * @param jsViewTool
     */
    public <T extends JsViewTool> void addJsViewTool (final Class<T> jsViewTool) {

        if(JsViewTool.class.isAssignableFrom(jsViewTool)) {
            final JsViewTool jsViewToolInstance = ReflectionUtils.newInstance(jsViewTool);
            if (jsViewToolInstance.getScope() == JsViewTool.SCOPE.APPLICATION) {
                this.initApplicationView(jsViewToolInstance);
                this.jsAplicationViewToolMap.put(jsViewToolInstance.getName(), jsViewToolInstance);
            } else {
                this.jsRequestViewToolMap.put(jsViewToolInstance.getName(), jsViewTool);
            }
        }
    }

    private void initApplicationView(final JsViewTool jsViewToolInstance) {

        this.jsAplicationViewToolMap.entrySet().forEach(entry -> {

            final JsViewTool instance = entry.getValue();

            if (instance instanceof JsApplicationContextAware) {

                JsApplicationContextAware.class.cast(instance).setContext(Config.CONTEXT);
            }
        });
    }

    /**
     * Remove a JsViewTool from the engine
     * @param jsViewTool
     */
    public void removeJsViewTool(final Class jsViewTool) {

        this.jsRequestViewToolMap.remove(jsViewTool.getName());
    }

    @Override
    public Object eval(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final Reader scriptReader,
                       final Map<String, Object> contextParams) {

        final DotJSON dotJSON = (DotJSON)contextParams.getOrDefault("dotJSON", new DotJSON());
        try (Context context = Context.newBuilder(ENGINE_JS)
                .out(new ConsumerOutputStream((msg)->Logger.debug(JsEngine.class, msg)))
                .err(new ConsumerOutputStream((msg)->Logger.debug(JsEngine.class, msg)))
                //.allowHostAccess(HostAccess.ALL) // todo: ask if we want all access to the classpath
                //allows access to all Java classes
                //.allowHostClassLookup(className -> true)
                .build()) {

            final Object fileName = contextParams.getOrDefault("dot:jsfilename", "sample.js");
            final Source source   = Source.newBuilder(ENGINE_JS, scriptReader, fileName.toString()).build();
            final Value bindings  = context.getBindings(ENGINE_JS);
            contextParams.entrySet().forEach(entry -> bindings.putMember(entry.getKey(), entry.getValue()));
            this.addTools(request, response, bindings, contextParams);

            final JsRequest jsRequest  = new JsRequest(request, contextParams);
            final JsResponse jsResponse = new JsResponse(response);
            bindings.putMember("dotJSON", dotJSON);
            bindings.putMember("request",  jsRequest);
            bindings.putMember("response", jsResponse);
            bindings.putMember("fetch", new JsFetch(context));
            Value eval   = context.eval(source);
            if (eval.canExecute()) {
                eval = contextParams.containsKey("dot:arguments")?
                        eval.execute(buildArgs(jsRequest, jsResponse, (Object[])contextParams.get("dot:arguments"))):
                        eval.execute(buildArgs(jsRequest, jsResponse, null));
            }

            if (eval.isHostObject()) {
                return eval.asHostObject();
            }

            if (isString(eval)) {
                return eval.as(String.class);
            }

            final Value finalValue = eval;
            final Map resultMap = Try.of(()-> finalValue.as(Map.class)).getOrNull();
            if (Objects.nonNull(resultMap)) {
                return CollectionsUtils.toSerializableMap(resultMap); // we need to do that b.c the context will be close after the return and the resultMap won;t be usable.
            }

            return CollectionsUtils.map("output", eval.asString(), "dotJSON", dotJSON);
        } catch (final IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean isString(final Value eval) {
        return eval.isString();
    }

    private Object[] buildArgs(final JsRequest request,
                               final JsResponse response,
                               final Object[] objects) {

        final Object [] defaultArgsArray = new Object[]{
                JsProxyFactory.createProxy(new JsContext.Builder().request(request).response(response).logger(JS_DOT_LOGGER).build()) };

        return null != objects && objects.length > 0?
                CollectionsUtils.concat(defaultArgsArray, objects): defaultArgsArray;
    }

    private void addTools(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final Value bindings,
                          final Map<String, Object> contextParams) {

        this.jsRequestViewToolMap.entrySet().forEach(entry -> {

                try {
                    final Object instance = entry.getValue().newInstance();
                    if (instance instanceof JsViewTool) {

                        final JsViewTool jsViewTool = (JsViewTool)instance;
                        initJsViewTool(request, response, jsViewTool);
                        bindings.putMember(jsViewTool.getName(), instance);
                    }
                } catch (final InstantiationException | IllegalAccessException e) {

                    Logger.error(this, e.getMessage(), e);
                }
        });

        this.jsAplicationViewToolMap.entrySet().forEach(entry -> {

            final JsViewTool instance = entry.getValue();
            bindings.putMember(instance.getName(), instance);
        });
    }

    private void initJsViewTool(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final JsViewTool instance) {

        if (instance instanceof JsHttpServletResponseAware) {

            JsHttpServletResponseAware.class.cast(instance).setResponse(response);
        }

        if (instance instanceof JsHttpServletRequestAware) {

            JsHttpServletRequestAware.class.cast(instance).setRequest(request);
        }

        if (instance instanceof JsApplicationContextAware) {

            JsApplicationContextAware.class.cast(instance).setContext(Config.CONTEXT);
        }

        if (instance instanceof JsViewContextAware) {

            final ViewContext velocityContext = new ChainedContext(VelocityUtil.getBasicContext(), request,
                    response, Config.CONTEXT);
            JsViewContextAware.class.cast(instance).setViewContext(velocityContext);
        }
    }
}
