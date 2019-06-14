package com.dotcms.rest;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.model.Tag;

import com.dotmarketing.util.Logger;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.function.Function;

public class TagTransform implements RestTransform<Tag, RestTag>{

    public TagTransform() {
    }

    @Override
    public RestTag appToRest(Tag app) {
        Logger.info(this, "TAG: " + app.toString());
        return toRest.apply(app);
    }

    @Override
    public Tag applyRestToApp(RestTag rest, Tag app) {
        return app;
    }

    @Override
    public Function<Tag, RestTag> appToRestFn() {
        return toRest;
    }

    private final Function<Tag, RestTag> toRest = (app) -> {
        final Host host = Sneaky.sneak(() -> APILocator.getHostAPI().find(app.getHostId(),
                APILocator.systemUser(), false));
Logger.info(this,"HOST INFO: " + host.toString());
        return new RestTag.Builder()
            .key(app.getTagName())
            .label(app.getTagName())
            .siteId(app.getHostId())
                .siteName(host.getHostname())
                .persona(app.isPersona())
                .build();
    };
}

