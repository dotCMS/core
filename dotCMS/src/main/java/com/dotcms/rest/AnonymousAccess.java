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
   * Returns the configured anonymous access level for content APIs, defaulting to NONE.
   * Backend-only endpoints do not use this setting.
   *
   * @return the configured {@link AnonymousAccess}, or {@link #NONE} when absent or invalid
   */
  public static AnonymousAccess systemSetting() {
    return from(Config.getStringProperty(CONTENT_APIS_ALLOW_ANONYMOUS, "NONE"));
  }
  
  
  
}
