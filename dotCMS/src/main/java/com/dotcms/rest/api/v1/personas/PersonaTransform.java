package com.dotcms.rest.api.v1.personas;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.portlets.personas.model.Persona;
import java.util.function.Function;

/**
 * @author Geoff M. Granum
 */
public class PersonaTransform implements RestTransform<Persona, RestPersona>{

    public PersonaTransform() {
    }

    @Override
    public RestPersona appToRest(Persona app) {
        return toRest.apply(app);
    }

    @Override
    public Persona applyRestToApp(RestPersona rest, Persona app) {
        return app;
    }

    @Override
    public Function<Persona, RestPersona> appToRestFn() {
        return toRest;
    }

    private final Function<Persona, RestPersona> toRest = (app) -> {
        return new RestPersona.Builder()
            .key(app.getIdentifier())
            .description(app.getDescription())
            .name(app.getName())
            .build();
    };
}

