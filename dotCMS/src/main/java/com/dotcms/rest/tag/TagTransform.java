package com.dotcms.rest.tag;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.model.Tag;

import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.function.Function;

/**
 * Utility transformer
 */
public class TagTransform implements RestTransform<Tag, RestTag>{

    public TagTransform() {
    }

    /**
     * Application Tag model to Rest representation
     * @param app
     * @return
     */
    @Override
    public RestTag appToRest(Tag app) {
        return toRest.apply(app);
    }

    /**
     * Apply rest to app
     * @param rest
     * @param app
     * @return
     */
    @Override
    public Tag applyRestToApp(RestTag rest, Tag app) {
        return app;
    }

    /**
     * Transform Function getter
     * @return
     */
    @Override
    public Function<Tag, RestTag> appToRestFn() {
        return toRest;
    }

    /**
     * Transformation function
     */
    private final Function<Tag, RestTag> toRest = (app) -> {
        final Host host = Sneaky.sneak(() -> APILocator.getHostAPI().find(app.getHostId(),
                APILocator.systemUser(), false));
        return new RestTag.Builder()
            .key(app.getTagName())
            .label(app.getTagName())
            .siteId(app.getHostId())
                .siteName(host.getHostname())
                .persona(app.isPersona())
                .id(app.getTagId())
                .build();
    };
}

