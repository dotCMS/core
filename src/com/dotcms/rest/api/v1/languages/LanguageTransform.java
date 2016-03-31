package com.dotcms.rest.api.v1.languages;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.util.function.Function;

/**
 * @author Geoff M. Granum
 */
public class LanguageTransform implements RestTransform<Language, RestLanguage>{

    public LanguageTransform() {
    }

    @Override
    public RestLanguage appToRest(Language app) {
        return toRest.apply(app);
    }

    @Override
    public Language applyRestToApp(RestLanguage rest, Language app) {
        return app;
    }

    @Override
    public Function<Language, RestLanguage> appToRestFn() {
        return toRest;
    }

    private final Function<Language, RestLanguage> toRest = (app) ->  new RestLanguage.Builder()
            .key(app.getLanguageCode())
            .name(app.getLanguage())
            .build();
}

