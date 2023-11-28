package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.js.viewtools.FetchJsViewTool;
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
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.util.FileUtil;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.JsDynamicObjectUtils;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.object.LayoutImpl;
import com.oracle.truffle.object.LayoutStrategy;
import com.oracle.truffle.object.ShapeImpl;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Js script engine implementation
 * @author jsanca
 */
public class JsEngine implements ScriptEngine {

    private static final String ENGINE_JS = JavaScriptLanguage.ID;
    private final JsFileSystem jsFileSystem = new JsFileSystem();
    private final JsDotLogger jsDotLogger = new JsDotLogger();
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

    private Context buildContext () {

        return Context.newBuilder(ENGINE_JS)
                .allowIO(true)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true")
                .out(new ConsumerOutputStream((msg)->Logger.debug(JsEngine.class, msg)))
                .err(new ConsumerOutputStream((msg)->Logger.debug(JsEngine.class, msg)))
                .fileSystem(jsFileSystem)
                //.allowHostAccess(HostAccess.ALL) // todo: ask if we want all access to the classpath
                //allows access to all Java classes
                //.allowHostClassLookup(className -> true)
                .build();
    }

    @Override
    public Object eval(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final Reader scriptReader,
                       final Map<String, Object> contextParams) {

        final DotJSON dotJSON = (DotJSON)contextParams.getOrDefault("dotJSON", new DotJSON());
        try (Context context = buildContext()) {

            final Object fileName   = contextParams.getOrDefault("dot:jsfilename", "sample.js");
            final Source userSource = Source.newBuilder(ENGINE_JS, scriptReader, fileName.toString()).build();
            final List<Source> dotSources = getDotSources();
            final Value bindings = context.getBindings(ENGINE_JS);
            contextParams.entrySet().forEach(entry -> bindings.putMember(entry.getKey(), entry.getValue()));
            this.addTools(request, response, bindings, contextParams);

            final JsRequest jsRequest   = new JsRequest(request, contextParams);
            final JsResponse jsResponse = new JsResponse(response);
            bindings.putMember("dotJSON", dotJSON);
            bindings.putMember("request",  jsRequest);
            bindings.putMember("response", jsResponse);

            dotSources.stream().forEach(source -> context.eval(source));
            Value eval = context.eval(userSource);
            if (eval.canExecute()) {
                eval = contextParams.containsKey("dot:arguments")?
                        eval.execute(buildArgs(jsRequest, jsResponse, (Object[])contextParams.get("dot:arguments"))):
                        eval.execute(buildArgs(jsRequest, jsResponse, null));
            }

            checkRejected (eval);

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

    private void checkRejected(final Value eval) {

        try {
            final JSPromiseObject promise = eval.as(JSPromiseObject.class);
            if (promise.getPromiseState() == JSPromise.REJECTED) {

                final Object[] stackTraceArray = JsDynamicObjectUtils.getObjectArray(promise);

                throw new DotRuntimeException(Map.of(
                        "message", "Promise rejected",
                        "rootCause", eval.toString(),
                        "stackTrace", Arrays.asList(stackTraceArray)).toString());
            }
        } catch (ClassCastException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private List<Source> getDotSources() throws IOException {

        final List<Source> sources = new ArrayList<>();
        addFunctions(sources);
        addModules(sources);
        return sources;
    }

    private void addModules(final List<Source> sources) throws IOException {

        final String absoluteWebInfPath  = Config.CONTEXT.getRealPath(File.separator + "WEB-INF");
        final String relativeModulesPath = File.separator + "WEB-INF" + File.separator + "javascript" + File.separator + "modules" + File.separator;
        final String absoluteModulesPath = Config.CONTEXT.getRealPath(relativeModulesPath);
        FileUtil.walk(absoluteModulesPath,
                path -> path.getFileName().toString().endsWith(".mjs"), path -> {

                    final String absolutePath = path.toString();
                    Logger.info(this, "Loading: " + absolutePath);
                    final Source source = toModuleSource(absolutePath,
                            StringUtils.remove(absolutePath, absoluteWebInfPath), path.toFile());
                    if (Objects.nonNull(source)) {
                        sources.add(source);
                        Logger.info(this, "Loaded: " + absolutePath);
                    }
                });
    }

    private void addFunctions(final List<Source> sources) throws IOException {
        final String relativeFunctionsPath = File.separator + "WEB-INF" + File.separator + "javascript" + File.separator + "functions" + File.separator;
        final String absoluteFunctionsPath = Config.CONTEXT.getRealPath(relativeFunctionsPath);
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
                    Source.newBuilder(ENGINE_JS, stringReader, absolutePath).build()).getOrElseThrow(e -> new RuntimeException(e));
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
                    Source.newBuilder(ENGINE_JS, stringReader, modulePath)
                            .mimeType("application/javascript+module")
                            .build()).getOrElseThrow(e -> new RuntimeException(e));
        }

        return source;
    }

    private boolean isString(final Value eval) {
        return eval.isString();
    }

    private Object[] buildArgs(final JsRequest request,
                               final JsResponse response,
                               final Object[] objects) {

        final Object [] defaultArgsArray = new Object[]{
                JsProxyFactory.createProxy(new JsContext.Builder().request(request).response(response).logger(jsDotLogger).build()) };

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
