package com.dotcms.rest;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.tag.model.Tag;

import java.util.function.Function;

public class TagTransform implements RestTransform<Tag, RestTag>{

    public TagTransform() {
    }

    @Override
    public RestTag appToRest(Tag app) {
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
        return new RestTag.Builder()
            .key(app.getTagName())
            .label(app.getTagName())
            .build();
    };
}

