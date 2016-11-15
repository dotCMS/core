package com.dotcms.api.system.event;

import com.dotcms.api.system.event.verifier.GlobalVerifier;
import com.dotcms.api.system.event.verifier.PermissionVerifier;
import com.dotcms.api.system.event.verifier.RoleVerifier;
import com.dotcms.api.system.event.verifier.UserVerifier;
import com.dotcms.config.DotInitializer;

/**
 * Initializer class that allow us to register {@link PayloadVerifier} associated with a Payload {@link Visibility}
 * using the {@link PayloadVerifierFactory}.
 * This class is executed by the {@link com.dotcms.config.DotInitializationService} on the application initialization
 *
 * @author Jonathan Gamba
 *         11/15/16
 */
public class PayloadVerifierFactoryInitializer implements DotInitializer {

    @Override
    public void init() {

        //Get the instance of our factory
        final PayloadVerifierFactory factory = PayloadVerifierFactory.getInstance();

        //And register each of ours PayloadVerifier
        factory.register(Visibility.USER, new UserVerifier());
        factory.register(Visibility.ROLE, new RoleVerifier());
        factory.register(Visibility.PERMISSION, new PermissionVerifier());
        factory.register(Visibility.GLOBAL, new GlobalVerifier());
    }

}