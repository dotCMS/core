package com.dotcms.rendering.js;

import static com.dotmarketing.util.VelocityUtil.getBasicContext;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.JsEngineException;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.js.JsContext.Builder;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.js.proxy.JsRequest;
import com.dotcms.rendering.js.proxy.JsResponse;
import com.dotcms.rendering.js.viewtools.CacheJsViewTool;
import com.dotcms.rendering.js.viewtools.CategoriesJsViewTool;
import com.dotcms.rendering.js.viewtools.ContainerJsViewTool;
import com.dotcms.rendering.js.viewtools.ContentJsViewTool;
import com.dotcms.rendering.js.viewtools.FetchJsViewTool;
import com.dotcms.rendering.js.viewtools.LanguageJsViewTool;
import com.dotcms.rendering.js.viewtools.SecretJsViewTool;
import com.dotcms.rendering.js.viewtools.SiteJsViewTool;
import com.dotcms.rendering.js.viewtools.TagJsViewTool;
import com.dotcms.rendering.js.viewtools.TemplateJsViewTool;
import com.dotcms.rendering.js.viewtools.UserJsViewTool;
import com.dotcms.rendering.js.viewtools.WorkflowJsViewTool;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Js script engine implementation
 * @author jsanca
 */
public class JsEngine implements ScriptEngine {


    public static final String DOT_JSON = "dotJSON";
    public static final String WEB_INF = "WEB-INF";
    private static final String JS_ENGINE = "js";
    private final JsFileSystem jsFileSystem = new JsFileSystem();
    private final JsDotLogger jsDotLogger = new JsDotLogger();
    private final Map<String, Class<? extends JsViewTool>> jsRequestViewToolMap = new ConcurrentHashMap<>();
    private final Map<String, JsViewTool> jsAplicationViewToolMap = new ConcurrentHashMap<>();

    private final Lazy<Boolean> allowAllHostAccess = Lazy.of(
            () -> Config.getBooleanProperty("ALLOW_ALL_HOST_ACCESS_JSENGINE", false));

    public JsEngine () {
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
            this.addJsViewTool(FetchJsViewTool.class);
        } catch (Throwable e) {
            Logger.error(JsEngine.class, "Could not start the js view tools", e);
        }
    }


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

        if (jsViewToolInstance instanceof JsApplicationContextAware) {

            JsApplicationContextAware.class.cast(jsViewToolInstance).setContext(Config.CONTEXT);
        }
    }

    /**
     * Remove a JsViewTool from the engine
     * @param jsViewTool
     */
    public <T extends JsViewTool> void removeJsViewTool(final Class<T> jsViewTool) {

        this.jsRequestViewToolMap.remove(jsViewTool.getName());
    }

    private Context buildContext () {

        final Context.Builder builder =
                Context.newBuilder(JS_ENGINE)
                .allowIO(true)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true")
                .out(new ConsumerOutputStream(msg->Logger.debug(JsEngine.class, msg)))
                .err(new ConsumerOutputStream(msg->Logger.debug(JsEngine.class, msg)))
                .fileSystem(jsFileSystem);

                final boolean allowAllHostAccess = this.allowAllHostAccess.get();
                if (allowAllHostAccess) {
                    builder.allowHostAccess(HostAccess.ALL);
                }
                //allows access to all Java classes
                //.allowHostClassLookup(className -> true)
        return builder.build();
    }

    @Override
    public Object eval(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final Reader scriptReader,
                       final Map<String, Object> contextParams) {

        final DotJSON dotJSON = (DotJSON)contextParams.computeIfAbsent(DOT_JSON, k -> new DotJSON());
        try (Context context = buildContext()) {

            final Object fileName   = contextParams.getOrDefault("dot:jsfilename", "sample.js");
            final Source userSource = Source.newBuilder(JS_ENGINE, scriptReader, fileName.toString()).build();
            final List<Source> dotSources = getDotSources();
            final Value bindings = context.getBindings(JS_ENGINE);
            contextParams.entrySet().forEach(entry -> bindings.putMember(entry.getKey(), entry.getValue()));
            this.addTools(request, response, bindings);

            final JsRequest jsRequest   = new JsRequest(request, contextParams);
            final JsResponse jsResponse = new JsResponse(response);
            bindings.putMember(DOT_JSON, dotJSON);
            bindings.putMember("request",  jsRequest);
            bindings.putMember("response", jsResponse);

            dotSources.stream().forEach(context::eval);
            Value eval = context.eval(userSource);
            if (eval.canExecute()) {
                eval = contextParams.containsKey("dot:arguments")?
                        eval.execute(buildArgs(jsRequest, jsResponse, (Object[])contextParams.get("dot:arguments"))):
                        eval.execute(buildArgs(jsRequest, jsResponse, null));
            }

            checkRejected (eval);

            return asValue(eval, dotJSON);
        } catch (final IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new JsEngineException(e);
        }
    }

    private Object asValue (final Value eval, final DotJSON dotJSON) {

        if (eval.isHostObject()) {
            return eval.asHostObject();
        }

        if (eval.isString()) {
            return eval.as(String.class);
        }

        if (eval.isNumber()) {
            return eval.as(Number.class);
        }

        if (eval.isBoolean()) {
            return eval.as(Boolean.class);
        }

        final Value finalValue = eval;
        // note: we can not parametrized this Map, b/c literally we do not know what it is, could be anything coming from the JS
        final Map resultMap = Try.of(()-> finalValue.as(Map.class)).getOrNull();
        if (Objects.nonNull(resultMap)) {
            return CollectionsUtils.toSerializableMap(resultMap); // we need to do that b.c the context will be close after the return and the resultMap won;t be usable.
        }

        return new HashMap<>(Map.of("output", eval.asString(), DOT_JSON, dotJSON));
    }

    private void checkRejected(final Value eval) {


    }

    private String stackTraceToString (final Object[] stackTraceArray) {

        StringBuilder sb = new StringBuilder();
        for (final Object stackTrace : stackTraceArray) {
            sb.append(stackTrace + "\n");
        }

        return sb.toString();
    }




    private List<Source> getDotSources() throws IOException {

        List<Source> sources = (List<Source>)APILocator.getSystemAPI().getSystemCache().get("jsdotsources");
        if (Objects.isNull(sources)) {

            sources = new ArrayList<>();
            addFunctions(sources);
            addModules(sources);
            APILocator.getSystemAPI().getSystemCache().put("jsdotsources", sources);
        }

        return sources;
    }

    private void addModules(final List<Source> sources) throws IOException {

        if (Objects.isNull(Config.CONTEXT)) {
            Logger.warn(this, "Context is null, can't load modules");
            return;
        }
        final String absoluteWebInfPath  = Config.CONTEXT.getRealPath(File.separator + WEB_INF);
        final String relativeModulesPath = File.separator + WEB_INF + File.separator + "javascript" + File.separator + "modules" + File.separator;
        final String absoluteModulesPath = Config.CONTEXT.getRealPath(relativeModulesPath);
        final File modulesPath = new File(absoluteModulesPath);
        if (modulesPath.exists() && modulesPath.canRead()) {
            FileUtil.walk(absoluteModulesPath,
                    path -> path.getFileName().toString().endsWith(".mjs"), path -> {

                        final String absolutePath = path.toString();
                        Logger.debug(this, "Loading: " + absolutePath);
                        final Source source = toModuleSource(absolutePath,
                                StringUtils.remove(absolutePath, absoluteWebInfPath), path.toFile());
                        if (Objects.nonNull(source)) {
                            sources.add(source);
                            Logger.debug(this, "Loaded: " + absolutePath);
                        }
                    });
        } else {

            Logger.warn(this, "The modules path: " + absoluteModulesPath + " does not exist or is not readable");
        }
    }

    private void addFunctions(final List<Source> sources) throws IOException {
        final String relativeFunctionsPath = File.separator + WEB_INF + File.separator + "javascript" + File.separator + "functions" + File.separator;
        if (Objects.isNull(Config.CONTEXT)) {
            Logger.warn(this, "Context is null, can't load functions");
            return;
        }
        final String absoluteFunctionsPath = Config.CONTEXT.getRealPath(relativeFunctionsPath);
        final File functionsPath = new File(absoluteFunctionsPath);
        if (functionsPath.exists() && functionsPath.canRead()) { 

            FileUtil.walk(absoluteFunctionsPath,
                    path -> path.getFileName().toString().endsWith(".js"), path -> {

                        final String absolutePath = path.toString();
                        Logger.info(this, "Loading: " + absolutePath);
                        final Source source = toSource(absolutePath, path.toFile());
                        if (Objects.nonNull(source)) {
                            sources.add(source);
                            Logger.info(this, "Loaded: " + absolutePath);
                        }
                    });
        } else {
            Logger.warn(this, "The functions path: " + absoluteFunctionsPath + " does not exist or is not readable");
        }
    }

    /**
     * Convert a file to a String
     * @param absolutePath key for looking for on the cache, this is usually the absolute path of the file
     * @param file File to read
     * @return String non null if ok
     */
    public static String toString (final String absolutePath, final File file) {

        final JsCache cache = CacheLocator.getJavascriptCache();
        Object sourceContent = cache.get(absolutePath);
        if (Objects.isNull(sourceContent)) {

            sourceContent = Try.of(()->FileUtil.read(file)).getOrNull();

            if (Objects.nonNull(sourceContent)) {

                cache.put(absolutePath, sourceContent);
            } else {

                Logger.warn(JsEngine.class, "Could not read the file: " + absolutePath);
            }
        }

        return null != sourceContent? sourceContent.toString(): null;
    }

    /**
     * Convert a file to a Source
     * @param absolutePath key for looking for on the cache, this is usually the absolute path of the file
     * @param file File to read
     * @return Source non null if ok
     */
    public static Source toSource (final String absolutePath, final File file) {

        Source source = null;
        final String sourceContent = toString(absolutePath, file);

        if (Objects.nonNull(sourceContent)) {

            final StringReader stringReader  = new StringReader(sourceContent);
            source = Try.of(() ->
                            Source.newBuilder(JS_ENGINE, stringReader, absolutePath).build())
                    .getOrElseThrow(JsEngineException::new);
        }

        return source;
    }

    /**
     * Convert a file to a Source
     * @param absolutePath key for looking for on the cache, this is usually the absolute path of the file
     * @param file File to read
     * @return Source non null if ok
     */
    public static Source toModuleSource (final String absolutePath, final String modulePath, final File file) {

        Source source = null;
        final String sourceContent = toString(absolutePath, file);

        if (Objects.nonNull(sourceContent)) {

            final StringReader stringReader  = new StringReader(sourceContent);
            source = Try.of(() ->
                    Source.newBuilder(JS_ENGINE, stringReader, modulePath)
                            .mimeType("application/javascript+module")
                            .build()).getOrElseThrow(JsEngineException::new);
        }

        return source;
    }

    private Object[] buildArgs(final JsRequest request,
                               final JsResponse response,
                               final Object[] objects) {

        final Object [] defaultArgsArray = new Object[]{
                JsProxyFactory.createProxy(
                        new Builder().request(request).response(response).logger(jsDotLogger).build())};

        return null != objects && objects.length > 0?
                CollectionsUtils.concat(defaultArgsArray, objects): defaultArgsArray;
    }

    private void addTools(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final Value bindings) {

        this.jsRequestViewToolMap.entrySet().forEach(entry -> {

                try {
                    final Object instance = entry.getValue().getDeclaredConstructor().newInstance();
                    if (instance instanceof JsViewTool) {

                        final JsViewTool jsViewTool = (JsViewTool)instance;
                        initJsViewTool(request, response, jsViewTool);
                        bindings.putMember(jsViewTool.getName(), instance);
                    }
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException |
                               InvocationTargetException e) {

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

            final ViewContext velocityContext = new ChainedContext(
                    getBasicContext(), null, request, response);
            JsViewContextAware.class.cast(instance).setViewContext(velocityContext);
        }
    }
}
