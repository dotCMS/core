package com.dotcms.business;

import com.dotcms.cube.CubeJSClientFactory;
import com.dotmarketing.business.FactoryLocator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is useful to include classes are not into the CDI container but
 * wants to be available to be injected.
 * Most of the {@link FactoryLocator} classes will be eventually here.
 * @author jsanca
 */
@ApplicationScoped
public class FactoryLocatorProducers {


    @Produces
    public CubeJSClientFactory getCubeJSClientFactory() {
        return FactoryLocator.getCubeJSClientFactory();
    }
}
