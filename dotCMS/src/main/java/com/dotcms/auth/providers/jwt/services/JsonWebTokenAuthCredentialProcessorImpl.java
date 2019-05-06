package com.dotcms.auth.providers.jwt.services;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.ContainerRequest;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Default implementation
 * @author jsanca
 */
public class JsonWebTokenAuthCredentialProcessorImpl implements JsonWebTokenAuthCredentialProcessor {

    private final JsonWebTokenUtils jsonWebTokenUtils;

    private static class SingletonHolder {
        private static final JsonWebTokenAuthCredentialProcessorImpl INSTANCE = new JsonWebTokenAuthCredentialProcessorImpl();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenAuthCredentialProcessorImpl
     */
    public static JsonWebTokenAuthCredentialProcessorImpl getInstance() {

        return JsonWebTokenAuthCredentialProcessorImpl.SingletonHolder.INSTANCE;
    } // getInstance.

    private JsonWebTokenAuthCredentialProcessorImpl() {

        this(JsonWebTokenUtils.getInstance());
    }

    @VisibleForTesting
    protected JsonWebTokenAuthCredentialProcessorImpl(final JsonWebTokenUtils jsonWebTokenUtils) {

        this.jsonWebTokenUtils = jsonWebTokenUtils;
    }


    
    protected User processAuthHeaderFromJWT(final String authorizationHeader,final String ipAddress) {
    

      if (StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.trim().startsWith(BEARER)) {

          final String jsonWebToken = authorizationHeader.substring(BEARER.length());

          if(!UtilMethods.isSet(jsonWebToken)) {
              // "Invalid syntax for username and password"
              throw new SecurityException("Invalid Json Web Token", Response.Status.BAD_REQUEST);
          }

          try {

              return jsonWebTokenUtils.getUser(jsonWebToken.trim(), ipAddress);
          } catch (Exception e) {

              this.jsonWebTokenUtils.handleInvalidTokenExceptions(this.getClass(), e, null, null);
          }


      }
      return null;
      
      
    }
    
    @Override
    public User processAuthHeaderFromJWT(final String authorizationHeader,
                                              final HttpSession session, final String ipAddress) {


        final User user =processAuthHeaderFromJWT(authorizationHeader, ipAddress);
        if(user != null && null != session) {
            session.setAttribute(WebKeys.CMS_USER, user);
            session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        }

        

        return user;
    } // processAuthCredentialsFromJWT.

    @Override
    public User processAuthHeaderFromJWT(final HttpServletRequest request) {

        // Extract authentication credentials
        final String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

        
        User user =  processAuthHeaderFromJWT(authentication, request.getRemoteAddr());
        if(user!=null) {
          request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
          request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        }
        return user;
        
        
        

    } // processAuthCredentialsFromJWT.

} // E:O:F:JsonWebTokenAuthCredentialProcessorImpl.
