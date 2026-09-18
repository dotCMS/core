package com.dotcms.rest;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

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
    final AnonymousAccess setting = from(Config.getStringProperty(CONTENT_APIS_ALLOW_ANONYMOUS, "READ"));
    if (setting == READ || setting == WRITE) {
      Logger.warn(AnonymousAccess.class,
          "SECURITY: CONTENT_APIS_ALLOW_ANONYMOUS=" + setting
              + " — content API is accessible without authentication."
              + " If this instance is deployed behind a CDN/WAF, ensure the origin server"
              + " is network-restricted to trusted proxies only (e.g., CloudFront IP ranges)."
              + " Set CONTENT_APIS_ALLOW_ANONYMOUS=NONE for fully private instances.");
    }
    return setting;
  }
  
  
  
}
