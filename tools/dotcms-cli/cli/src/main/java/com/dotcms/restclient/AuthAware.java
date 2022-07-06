package com.dotcms.restclient;

import com.dotcms.cli.ApplicationContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import java.util.Optional;
import org.jboss.logging.Logger;

public interface AuthAware {

    Logger logger = Logger.getLogger(AuthAware.class);

    default String lookupAuth() {
        try(InstanceHandle<ApplicationContext> handler = Arc.container().instance(ApplicationContext.class)){
            final ApplicationContext context = handler.get();
            final Optional<String> token = context.getToken();
            if(token.isEmpty()){
                logger.error("We dont have a token to pass to the API. Try login again.");
                return null;
            }
            return "Bearer "+token.get();
        }
    }

}
