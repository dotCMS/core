package com.dotcms.cdi;

import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class Config {
    public void init( @Observes @Initialized( ApplicationScoped.class ) Object init ) {
        Logger.info(this,"CDI Config Bean initialized");
    }

    public void destroy( @Observes @Destroyed( ApplicationScoped.class ) Object init ) {
        Logger.info(this,"CDI Config Bean destroyed");
    }
}
