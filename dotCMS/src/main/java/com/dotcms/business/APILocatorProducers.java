package com.dotcms.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is useful to include classes are not into the CDI container but
 * wants to be available to be injected.
 * Most of the {@link com.dotmarketing.business.APILocator} classes will be eventually here.
 * @author jsanca
 */
@ApplicationScoped
public class APILocatorProducers {

   @Produces
   public HostAPI getHostAPI() {
      return APILocator.getHostAPI();
   }
}
