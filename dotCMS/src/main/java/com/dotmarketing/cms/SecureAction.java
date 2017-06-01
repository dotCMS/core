package com.dotmarketing.cms;

import com.dotcms.repackage.org.apache.struts.actions.DispatchAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;

@Deprecated
public class SecureAction extends DispatchAction {

  
  public SecureAction(){
    String key = "allow.legacy." + this.getClass().getCanonicalName().toLowerCase();
    if(!Config.getBooleanProperty(key, false)){
      SecurityLogger.logInfo(this.getClass(), "Legacy Front End Account Management Disallowed.");
      Logger.warn(this.getClass(), "Legacy Front End Account Management Disallowed.");
      Logger.warn(this.getClass(), "------------------------------------");
      Logger.warn(this.getClass(), "To enable, add '" + key + "=true' to your dotmarketing-config.properties");
      Logger.warn(this.getClass(), "------------------------------------");
      throw new SecurityException("Front End Account Management Disallowed");
    }
  }
  
  
  
  
  
  
  
}
