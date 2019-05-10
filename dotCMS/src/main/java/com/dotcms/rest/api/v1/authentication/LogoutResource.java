package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * Implements the logout endpoint, 200 if logout is successfully, 500 otherwise
 *
 * @author jsanca
 */
@Path("/v1/logout")
public class LogoutResource implements Serializable {

    private final LoginServiceAPI loginService;
    private final WebResource webResource;

    private final Log log = LogFactory.getLog(LogoutResource.class);

    @SuppressWarnings("unused")
    public LogoutResource() {
        this(APILocator.getLoginServiceAPI(),
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected LogoutResource(final LoginServiceAPI loginService,
                             final WebResource webResource) {

        this.loginService = loginService;
        this.webResource = webResource;
    }


    // todo: add the https annotation
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response logout(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response) {

        Response res = null;
        String   url = null;

        try {

            this.log.debug("Doing the logout");
            User user = PortalUtil.getUser(request);
            
            this.loginService.doActionLogout(request, response);
            
            if(null != user){
            	SecurityLogger.logInfo(this.getClass(), "User " + user.getFullName() + " (" + user.getUserId() + ") has logged out from IP: " + request.getRemoteAddr());
            }

            url = Config.getStringProperty("logout.url", StringPool.BLANK);

            res = UtilMethods.isSet(url)?
                    Response.ok(new ResponseEntityView("Logout successfully"))
                    .header("url", Config.getStringProperty("logout.url", StringPool.BLANK))
                    .build(): // 200
                    Response.ok(new ResponseEntityView("Logout successfully"))
                    .build();

        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);

        } catch (Exception e) {

            this.log.error("Error doing the logout", e);
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    } // logout.
} // E:O:F:LogoutResource.
