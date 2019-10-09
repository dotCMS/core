package com.dotcms.rest;

import com.dotmarketing.util.Config;

public enum AnonymousAccess {

  NONE,
  READ,
  WRITE,
  ;
  /**
   * resolves a String to an AnonymousAccess
   * defaults to NONE
   * @param accessString
   * @return
   */
  public static AnonymousAccess from(final String accessString) {
    for(AnonymousAccess access : values()) {
      if(access.name().equalsIgnoreCase(accessString)) {
        return access;
      }
    }
    return NONE;
    
  }
  
  public final static String CONTENT_APIS_ALLOW_ANONYMOUS="CONTENT_APIS_ALLOW_ANONYMOUS";
  
  /**
   * this will be used as the default setting for
   * rest endpoints that can respect anonymous permissions
   * e.g. content read/write.
   * Endpoints that are explictly for backend use ignore this value
   * all together
   * @return
   */
  public static AnonymousAccess systemSetting() {
    return from(Config.getStringProperty(CONTENT_APIS_ALLOW_ANONYMOUS,"READ"));
  }
  
  
  
}
