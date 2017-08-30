package com.dotmarketing.util;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;
import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

import javax.servlet.http.Cookie;

public class DotCookie extends Cookie {

    private static final long serialVersionUID = -8548021814083668839L;
    private static final String ALWAYS = "always";
    private static final String HTTPS = "https";

    private boolean httpOnly=false;
    private boolean secure=false;
    
    
    
    public DotCookie(String arg0, String arg1) {
        super(arg0, arg1);
    }

    @Override
    public boolean getSecure() {
        if (ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))
                        || HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, HTTPS))) {

            return true;
        }
        else{
            return secure;
        }
            

        
    }

    @Override
    public boolean isHttpOnly() {
        return Config.getBooleanProperty(COOKIES_HTTP_ONLY, httpOnly);
    }

    @Override
    public void setHttpOnly(boolean isHttpOnly) {
        this.httpOnly = isHttpOnly;
    }

    @Override
    public void setSecure(boolean flag) {
        this.secure = flag;
    }



}
