package com.dotcms.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * This class is useful to include classes are not into the CDI container but
 * wants to be available to be injected.
 * Most of the {@link com.dotmarketing.business.APILocator} classes will be eventually here.
 * @author jsanca
 */
@ApplicationScoped
public class APILocatorProducers {

   @Named("HostAPI")
   @Produces
   public HostAPI getHostAPI() {
      return APILocator.getHostAPI();
   }

   @Produces
   public PermissionAPI getPermissionAPI() {
      return APILocator.getPermissionAPI();
   }

   @Produces
   public FolderAPI getFolderAPI() {
      return APILocator.getFolderAPI();
   }

   @Produces
   public UserAPI getUserAPI() {
      return APILocator.getUserAPI();
   }
}
