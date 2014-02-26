package com.dotcms.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Xss;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;

public class SecurityUtils {

    public static String stripReferer ( HttpServletRequest request, String referer ) throws IllegalArgumentException {

        String ref = referer;
        if(Config.getBooleanProperty("DISABLE_EXTERNAL_REFERERS",true) && ref.contains("://")) {

            try {

                //Search for the system user
                User systemUser = APILocator.getUserAPI().getSystemUser();

                /*
                 Now we need to identify if this referer url is a external or internal URL, externals URLs
                 could lead to security threats.
                  */
                URL url = new URL( referer );
                String refererHost = url.getHost();

                String serverName = request.getServerName();

                //Verify if we want to move inside the same app
                if ( !refererHost.equals( serverName ) ) {

                    //Trying to find the host in our list of host
                    Host foundHost = APILocator.getHostAPI().findByName( refererHost, systemUser, false );
                    if ( !UtilMethods.isSet( foundHost ) ) {
                        foundHost = APILocator.getHostAPI().findByAlias( refererHost, systemUser, false );
                    }

                    //If the host was not found it means it is a external url
                    if ( !UtilMethods.isSet( foundHost ) ) {
                        ref = "/";
                    }
                }

            } catch ( Exception e ) {
                throw new IllegalArgumentException( "Error validating URL " + referer, e );
            }
        }

        ref = Xss.strip(ref);

        if(ref.contains("%0d") || ref.contains("%0a"))
            ref = "/";

        return ref;
    }
}
