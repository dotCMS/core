package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.apps.view.ViewStack.StackContext;
import com.dotcms.security.apps.AppDescriptor;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 * Represents a service integration. Which serves as the top level entry for all the endpoints. The
 * view unfolds itself in the specifics for the associated sites.
 */
@JsonSerialize(using = AppView.AppViewSerializer.class)
public class AppView {

    private final int configurationsCount;

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    private final boolean allowExtraParams;

    @JsonInclude(Include.NON_NULL)
    private final Integer sitesWithWarnings;

    @JsonInclude(Include.NON_NULL)
    private final List<SiteView> sites;

    /**
     * Used to build a site-less integration view
     */
    public AppView(final AppDescriptor appDescriptor, final int configurationsCount,
            final int sitesWithWarnings) {
        this.key = appDescriptor.getKey();
        this.name = appDescriptor.getName();
        this.description = appDescriptor.getDescription();
        this.iconUrl = appDescriptor.getIconUrl();
        this.allowExtraParams = appDescriptor.isAllowExtraParameters();
        this.configurationsCount = configurationsCount;
        this.sitesWithWarnings = sitesWithWarnings == 0 ? null : sitesWithWarnings;
        this.sites = null;
    }

    /**
     * Use to build a more detailed integration view Including site specific config info.
     */
    public AppView(final AppDescriptor appDescriptor, final int configurationsCount,
            final List<SiteView> sites) {
        this.key = appDescriptor.getKey();
        this.name = appDescriptor.getName();
        this.description = appDescriptor.getDescription();
        this.iconUrl = appDescriptor.getIconUrl();
        this.allowExtraParams = appDescriptor.isAllowExtraParameters();
        this.configurationsCount = configurationsCount;
        this.sites = sites;
        this.sitesWithWarnings = null;
    }

    /**
     * number of configuration (Total count)
     */
    public long getConfigurationsCount() {
        return configurationsCount;
    }

    /**
     * Service unique identifier
     */
    public String getKey() {
        return key;
    }

    /**
     * any given name
     */
    public String getName() {
        return name;
    }

    /**
     * Any given description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The url of the avatar used on the UI
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Whether or not extra params are supported
     */
    public boolean isAllowExtraParams() {
        return allowExtraParams;
    }

    /**
     * Number of potential issues per site (warnings)
     */
    public Integer getSitesWithWarnings() {
        return sitesWithWarnings;
    }

    /**
     * All site specific configurations
     */
    public List<SiteView> getSites() {
        return sites;
    }


    public static class AppViewSerializer extends JsonSerializer<AppView> {

        static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper();

        @Override
        public void serialize(final AppView appView, final JsonGenerator jsonGenerator,
                final SerializerProvider serializers) throws IOException {

            final Map<String, Object> map = new HashMap<>();
            map.put("key", appView.key);
            map.put("name", appView.name);
            map.put("description", appView.description);
            map.put("iconUrl", appView.iconUrl);
            map.put("allowExtraParams", appView.allowExtraParams);
            map.put("configurationsCount", appView.configurationsCount);

            if (null != appView.sites) {
                map.put("sites", appView.sites);
            }
            if (null != appView.sitesWithWarnings) {
                map.put("sitesWithWarnings", appView.sitesWithWarnings);
            }

            ViewStack.createStack(map);

            final String json = mapper.writeValueAsString(map);

            final StackContext currentStack = ViewStack.getCurrentStack();

            final String interpolationAppliedJson = applyInterpolation(json, currentStack);

            jsonGenerator.writeRawValue(interpolationAppliedJson);

            ViewStack.dispose();

        }
    }

    static String applyInterpolation(final String inputJson, final StackContext stackContext) {
        try {
            final RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            final StringReader stringReader = new StringReader(inputJson);
            final SimpleNode simpleNode = runtimeServices.parse(stringReader, "app template");

            final Template template = new Template();
            template.setData(simpleNode);
            template.initDocument();

            final VelocityContext velocityContext = new VelocityContext();

            final String appPrefix = "app.";

            stackContext.app.forEach((key, value) -> {
                velocityContext.put(appPrefix + key, value);
            });

            final String secretsPrefix = appPrefix + "secrets.";

            stackContext.secretsBySite.forEach((siteId, secrets) -> {
                for (Map<String, Object> mapSecret : secrets) {
                    final String secretName = (String) mapSecret.get("name");
                    final Object hidden = mapSecret.get("hidden");
                    final Object type = mapSecret.get("type");
                    final Object value = mapSecret.get("value");

                    velocityContext.put(secretsPrefix + secretName, secretName);
                    velocityContext.put(secretsPrefix + secretName + ".name", secretName);
                    velocityContext.put(secretsPrefix + secretName + ".hidden", hidden);
                    velocityContext.put(secretsPrefix + secretName + ".type", type);
                    velocityContext.put(secretsPrefix + secretName + ".value", value);
                }
            });

            final String sitesPrefix = appPrefix + "sites['%s']";

            stackContext.sites.forEach((site, mapSite) -> {
                final String id = (String) mapSite.get("id");
                final String siteName = (String) mapSite.get("name");
                final Object configured = mapSite.get("configured");
                final String sitePrefix = String.format(sitesPrefix, id);
                velocityContext.put(sitePrefix + ".id", id);
                velocityContext.put(sitePrefix + ".name", siteName);
                velocityContext.put(sitePrefix + ".configured", configured);
            });

            final StringWriter stringWriter = new StringWriter();
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();
        } catch (ParseException e) {
            Logger.error(AppView.class, "Error Parsing ", e);
        }
        return inputJson;
    }

}
