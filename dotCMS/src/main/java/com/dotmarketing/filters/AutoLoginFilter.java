package com.dotmarketing.filters;

import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorAware;
import com.dotcms.filters.interceptor.jwt.JsonWebTokenInterceptor;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This filter determines whether it is necessary to perform the common
 * authentication process (i.e, sending the user to the Login page) or if the
 * user can be authenticated through other mechanisms, such as Cookie
 * Authentication, JWT, etc.
 * 
 * @author root
 * @version 2.x, 3.7
 * @since Mar 22, 2012
 *
 */
@SuppressWarnings("serial")
public class AutoLoginFilter implements Filter, WebInterceptorAware {

	private boolean alreadyStarted = false;

    private final List<WebInterceptor> interceptors =
            new CopyOnWriteArrayList<>();

    public static final String CAS_FILTER_USER = "edu.yale.its.tp.cas.client.filter.user";

    @Override
    public void destroy() {

        if (!this.interceptors.isEmpty()) {

            this.interceptors.forEach(interceptor -> interceptor.destroy());
        }
    }

    @Override
    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain) throws IOException,
            ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
		HttpSession session = request.getSession(false);
        boolean shouldContinue = true;
		
		boolean useCasFilter = Config.getBooleanProperty("FRONTEND_CAS_FILTER_ON", false);
		
        if (useCasFilter){
        	String userID = (String)session.getAttribute(CAS_FILTER_USER);
        	Logger.debug(AutoLoginFilter.class, "Doing CasAutoLogin Filter for: " + userID);
            if(UtilMethods.isSet(userID)){                
            	LoginFactory.doCookieLogin(PublicEncryptionFactory.encryptString(userID), request, response);      	
            }
        }
        else{ // todo: should we remove this functionality???
	        String encryptedId = UtilMethods.getCookieValue(request.getCookies(), WebKeys.CMS_USER_ID_COOKIE);
	 
	        if (((session != null && session.getAttribute(WebKeys.CMS_USER) == null) || session == null)&& 
	        		UtilMethods.isSet(encryptedId)) {
	            Logger.debug(AutoLoginFilter.class, "Doing AutoLogin for " + encryptedId);
	            LoginFactory.doCookieLogin(encryptedId, request, response);
	        } else {

                //List<WebInterceptor> interceptors = APILocator.getPorongaIntercetorFacadeAPI().getInterceptors(AutoLoginFilter.class);
                if (!this.interceptors.isEmpty()) {

                    for (WebInterceptor webInterceptor : this.interceptors) {

                        shouldContinue &= webInterceptor.intercept(request, response);

                        if (!shouldContinue) {
                            // if just one interceptor failed; we stopped the loop and do not continue the chain call
                            return;
                        }
                    }
                }
            }
	    }

        chain.doFilter(req, response);
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {

    	this.interceptors.add(new JsonWebTokenInterceptor());

        if (!this.interceptors.isEmpty()) {

            this.interceptors.forEach(interceptor -> interceptor.init());
        }

        this.alreadyStarted = true;
    }

    @Override
    public void add(final WebInterceptor webInterceptor) {

        if (null != webInterceptor) {

            if (this.alreadyStarted) {

                webInterceptor.init();
            }

            this.interceptors.add(webInterceptor);
        }
    }

}
