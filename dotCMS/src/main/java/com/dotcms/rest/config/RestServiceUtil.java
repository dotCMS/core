package com.dotcms.rest.config;

import com.dotcms.rest.servlet.ReloadableServletContainer;

public class RestServiceUtil {

  public static synchronized void addResource(Class clazz) {

    new DotRestApplication().getClasses();
    if (DotRestApplication.REST_CLASSES.contains(clazz)) {
      return;
    }

    DotRestApplication.REST_CLASSES.add(clazz);

    reloadRest();
  }

  public static synchronized void removeResource(Class clazz) {
    new DotRestApplication().getClasses();
    if (DotRestApplication.REST_CLASSES.contains(clazz)) {
      DotRestApplication.REST_CLASSES.remove(clazz);
      reloadRest();
    }
  }

  public static synchronized void reloadRest() {
    ReloadableServletContainer.reload(new DotRestApplication());
  }
}
