package com.dotcms.util;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Xss;

/**
 * This class exposes utility routines related to security aspects of dotCMS
 * that can be used by several pieces of the system.
 * 
 * @author Jorge Urdaneta
 * @version 2.5.4, 3.7
 * @since Feb 24, 2014
 *
 */
public class SecurityUtils {

	/**
	 * 
	 * @param request
	 * @param referer
	 * @return
	 * @throws IllegalArgumentException
	 */
    public static String stripReferer ( HttpServletRequest request, String referer ) throws IllegalArgumentException {

    	if(referer==null) return referer;

        String ref = referer;
        if(Config.getBooleanProperty("DISABLE_EXTERNAL_REFERERS", false) && ref.contains("://")) {

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

                    	// lets check if it is a Virtual Link
                    	List<VirtualLink> virtualLinks = APILocator.getVirtualLinkAPI().getVirtualLinksByURI(referer);

                    	if(!UtilMethods.isSet(virtualLinks) || virtualLinks.isEmpty()) {
                    		ref = "/";
                    	}

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

	/**
	 * This is a useful mechanism for dealing with DoS and similar security
	 * attacks in which it might be adequate to temporarily pause a request for
	 * a specific time before letting it continue. This is particularly useful
	 * to deal with multiple failed login attempts, in which the attacker can be
	 * penalized for it. Different security strategies can be added in order to 
	 * counter-attack these threats.
	 * <p>
	 * By default, the approach consists of taking a seed value and raising it
	 * to the power of 2. The result will represent the seconds that a given
	 * request will have to wait before moving on. For example, during a failed
	 * authentication, the {@code seed} can be the number of failed login
	 * attempts. This way, the more times hackers fail to authenticate, the more
	 * time they will have to wait to try it again.
	 * 
	 * @param seed
	 *            - The number of times that a user has tried to log in and
	 *            failed.
	 * @param delayStrategy
	 *            - The delay strategy used after a failed login.
	 */
	public static void delayRequest(int seed, final String delayStrategy) {
		seed = Math.abs(seed);
		// Default strategy
		if (delayStrategy.equalsIgnoreCase("pow")) {
			if (seed > 0) {
				long sleepTime = (long) Math.pow(seed, 2);
				try {
					TimeUnit.SECONDS.sleep(sleepTime);
				} catch (InterruptedException e) {
					// Sleep was interrupted, just ignore it
				}
			}
		}
	}

}
