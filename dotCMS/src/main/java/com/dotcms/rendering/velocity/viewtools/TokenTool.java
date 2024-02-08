package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.ApiToken.Builder;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

public class TokenTool implements ViewTool {


    HttpServletRequest request;
    SecretTool secrets = new SecretTool();

    @Override
    public void init(Object initData) {

        if (initData instanceof ViewContext) {
            request = ((ViewContext) initData).getRequest();
        }
        this.secrets.init(initData);
    }


    public String getToken(String secretKey, int expirationSeconds) {
        return generateToken(expirationSeconds, secretKey, null);
    }

    /**
     * Uses the secretKey to pull a JWT from the Velocity Secrets app. It then uses this JWT to generate a new, time
     * limited JWT for use in REST API operations in dotCMS
     *
     * @param expirationSeconds
     * @param secretKeyOrId
     * @return
     */
    public String getIpToken(String secretKeyOrId, int expirationSeconds) {
        String ipAddress = getRequest().getRemoteAddr();

        int slashNetmask = Config.getIntProperty("TOKENTOOL_DEFAULT_NETMASK", 24);

        return generateToken(expirationSeconds, secretKeyOrId, ipAddress + "/" + slashNetmask);
    }

    /**
     * @param expirationSeconds
     * @param secretKey
     * @param allowedNetmask
     * @return
     */
    public String getIpToken(String secretKey, int expirationSeconds, String allowedNetmask) {
        return generateToken(expirationSeconds, secretKey, allowedNetmask);
    }

    /**
     * Uses the secretKey to pull a JWT from the Velocity Secrets app. It then uses this JWT to generate a new, time
     * limited and IP limited JWT for use in REST API operations in dotCMS.  The JWT will only allow request from the
     * subnet of the original requestor
     * @param expirationSeconds
     * @param secretKeyOrId
     * @param subnetToLimit
     * @return
     */
    private String generateToken(int expirationSeconds, String secretKeyOrId, String subnetToLimit) {

        Optional<User> userOpt = resolveUserForToken(secretKeyOrId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User targetUser = userOpt.get();
        if (targetUser.isAdmin() && Config.getBooleanProperty("TOKENTOOL_BLOCK_CMS_ADMINS", true)) {
            SecurityLogger.logInfo(this.getClass(), "Unable to generate a short lived token for admin users.");
            throw new DotRuntimeException("Unable to generate a short lived token for admin users.");
        }
        if (targetUser.isBackendUser() && Config.getBooleanProperty("TOKENTOOL_BLOCK_BACKEND_USERS", true)) {
            SecurityLogger.logInfo(this.getClass(), "Unable to generate a short lived token for back end users.");
            throw new DotRuntimeException("Unable to generate a short lived token for back end users.");
        }





        Builder tokenBuilder = ApiToken.builder()
                .withIssueDate(new Date())
                .withUser(targetUser)
                .withRequestingIp(request.getRemoteAddr())
                .withRequestingUserId(targetUser.getUserId())
                .withExpires(Date.from(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS)));
        if (UtilMethods.isSet(subnetToLimit)) {
            tokenBuilder.withAllowNetwork(subnetToLimit);
        }

        ApiToken token = APILocator.getApiTokenAPI().persistApiToken(tokenBuilder.build(), targetUser);
        return APILocator.getApiTokenAPI().getJWT(token, targetUser);

    }

    /**
     * This checks the system secrets for a token under the key passed in and if it exists, will extract and return the
     * user of that token.  If there is no secret token that corresponds to the secretKey, then the Token is looked up
     * by its ID and the user is returned from that.
     *
     * @param secretKeyOrTokenId
     * @return
     */
    Optional<User> resolveUserForToken(String secretKeyOrTokenId) {
        String jwtToken = (String) this.secrets.getSystemSecret(secretKeyOrTokenId);
        if(UtilMethods.isEmpty(jwtToken)){
            Optional<ApiToken> token = APILocator.getApiTokenAPI().findApiToken(secretKeyOrTokenId);
            return token.map(
                    apiToken -> Try.of(() -> APILocator.getUserAPI().loadUserById(apiToken.userId)).getOrNull());
        }
        return APILocator.getApiTokenAPI().userFromJwt(jwtToken, getRequest().getRemoteAddr());

    }



    HttpServletRequest getRequest() {
        if (this.request != null) {
            return this.request;
        }
        if (UtilMethods.isSet(HttpServletRequestThreadLocal.INSTANCE.getRequest())) {
            return HttpServletRequestThreadLocal.INSTANCE.getRequest();
        }
        return new FakeHttpRequest("localhost", "/").request();
    }


}
