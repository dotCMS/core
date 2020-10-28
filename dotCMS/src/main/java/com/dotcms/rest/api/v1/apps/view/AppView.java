package com.dotcms.rest.api.v1.apps.view;

import static com.dotcms.rest.api.v1.apps.view.ViewUtil.interpolateValues;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.security.apps.AppDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            ViewUtil.newStackContext(appView);

            final String json = mapper.writeValueAsString(map);

            final String interpolationAppliedJson = interpolateValues(json);

            jsonGenerator.writeRawValue(interpolationAppliedJson);

            ViewUtil.disposeStackContext();

        }
    }

}
