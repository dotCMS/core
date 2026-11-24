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

  public static final String SCRIPTING_APIS_ALLOW_ANONYMOUS = "SCRIPTING_APIS_ALLOW_ANONYMOUS";

  /**
   * Whether anonymous requests may call the file-based scripting endpoints, {@code /api/vtl/{folder}}
   * and {@code /api/js/{folder}}, even when {@link #systemSetting()} is {@code NONE}. These endpoints
   * run code an administrator published, so this lets a site keep the built-in content APIs closed
   * to anonymous callers while still serving its own public endpoints. When {@link #systemSetting()}
   * already allows anonymous reads, the scripting endpoints are reachable whatever this flag says.
   * The script runs as the anonymous user either way. The {@code /dynamic} endpoints are not covered and follow
   * {@link #systemSetting()}.
   *
   * @return {@code true} when {@code SCRIPTING_APIS_ALLOW_ANONYMOUS} is set to true; defaults to {@code false}
   */
  public static boolean scriptingEndpointsAllowAnonymous() {
    return Config.getBooleanProperty(SCRIPTING_APIS_ALLOW_ANONYMOUS, false);
  }
  
  
  
}
