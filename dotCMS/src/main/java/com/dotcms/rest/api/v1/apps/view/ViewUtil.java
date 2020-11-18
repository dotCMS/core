package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

public class ViewUtil {

    private static final ThreadLocal<StackContext> threadLocal = new ThreadLocal<>();

    public static void newStackContext(final AppView appView) {
        threadLocal.set(new StackContext(appView));
    }

    private static StackContext currentStackContext() {
        return threadLocal.get();
    }

    public static void currentSite(final String siteId) {
        final StackContext currentStack = currentStackContext();
        if(null == currentStack){
            throw new IllegalStateException("Create stack hasn't been called yet.");
        }
        currentStack.currentSite = siteId;
    }

    public static void pushSecret(final Map<String, Object> map) {
        final StackContext context = currentStackContext();
        if(null == context){
            throw new IllegalStateException("Create stack hasn't been called yet.");
        }
        if (null == context.currentSite) {
            throw new IllegalStateException(
                    "Site must be pushed prior to pushing a secret.");
        }
        context.secretsBySite.computeIfAbsent(context.currentSite, k -> new ArrayList<>()).add(map);
    }

    public static void disposeStackContext() {
        threadLocal.remove();
    }

    static class StackContext {

        final Map<String, List<Map<String,Object>>> secretsBySite = new HashMap<>();

        final AppView appView;

        String currentSite;

        StackContext(final AppView appView) {
           this.appView = appView;
        }

    }

    public static class SecretHolder {

        private final String name;
        private final String label;
        private final String hint;
        private final Object value;

        SecretHolder(final String name, final String label, final String hint, Object value) {
            this.name = name;
            this.value = value;
            this.label = label;
            this.hint = hint;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getHint() {
            return hint;
        }

        @Override
        public String toString() {
            return "SecretHolder{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", label='" + label + '\'' +
                    ", hint='" + hint + '\'' +
                    '}';
        }
    }


    static String interpolateValues(final String inputJson) {
        final StackContext context = ViewUtil.currentStackContext();
        if (null == context) {
            throw new IllegalStateException(
                    "This method can only be called once a context has been previously created.");
        }
        try {
            final RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            final StringReader stringReader = new StringReader(inputJson);
            final SimpleNode simpleNode = runtimeServices.parse(stringReader, "app template");

            final Template template = new Template();
            template.setData(simpleNode);
            template.initDocument();

            final VelocityContext velocityContext = new VelocityContext();

            velocityContext.put("app", context.appView);

            final List<SiteView> sites = context.appView.getSites();
            if (null != sites) {
                final Map<String, SiteView> sitesById = sites.stream()
                        .collect(Collectors.toMap(SiteView::getId, Function.identity()));

                if (null != context.currentSite) {

                    final SiteView siteView = sitesById.get(context.currentSite);

                    velocityContext.put("siteId", context.currentSite);
                    velocityContext.put("siteName", siteView.getName());
                    velocityContext.put("configured", siteView.isConfigured());

                    final List<Map<String, Object>> currentSiteSecrets = context.secretsBySite
                            .get(context.currentSite);
                    if (null != currentSiteSecrets) {
                        for (final Map<String, Object> siteSecret : currentSiteSecrets) {
                            final String name = (String) siteSecret.get("name");
                            final String hint = (String) siteSecret.get("hint");
                            final String label = (String) siteSecret.get("label");
                            final Object value = siteSecret.get("value");
                            velocityContext.put(name, new SecretHolder(name, label, hint, value));
                        }
                    }
                }
            }

            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if(null != request){
                final User user = PortalUtil.getUser(request);
                velocityContext.put("user", user);
            }

            final StringWriter stringWriter = new StringWriter();
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            Logger.error(AppView.class, "Error interpreting velocity ", e);
        }
        return inputJson;
    }


}
