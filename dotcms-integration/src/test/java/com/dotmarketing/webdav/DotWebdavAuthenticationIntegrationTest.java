package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.time.Instant;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

/** Verifies DAV uses real persisted API-token validation, not just a plausible header. */
public class DotWebdavAuthenticationIntegrationTest {

    /** Initialize the real user, token and company services. */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** A username/JWT pair requires matching ownership, an allowed client IP and no revocation. */
    @Test
    public void jwtTokenRequiresTheNamedOwnerAllowedClientIpAndUnrevokedApiToken() throws Exception {
        final String previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final User owner = new UserDataGen().nextPersisted();
        User other = null;
        ApiToken token = null;
        final ApiTokenAPI tokens = APILocator.getApiTokenAPI();
        try {
            other = new UserDataGen().nextPersisted();
            Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
            final DotCMSMockRequest request = new DotCMSMockRequest();
            request.setRemoteAddr("192.0.2.10");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            token = tokens.persistApiToken(ApiToken.builder().withUserId(owner.getUserId())
                    .withExpires(Date.from(Instant.now().plusSeconds(3600)))
                    .withRequestingIp("192.0.2.10").withAllowNetwork("192.0.2.0/24")
                    .build(), APILocator.systemUser());
            final String jwt = tokens.getJWT(token, APILocator.systemUser());
            final String login = davLogin(owner);
            final DotWebdavHelper helper = new DotWebdavHelper();

            assertEquals(owner.getUserId(), helper.authorizePrincipal(login, jwt).getUserId());
            final String wrongLogin = davLogin(other);
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(wrongLogin, jwt));

            request.setRemoteAddr("198.51.100.10");
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(login, jwt));
            request.setRemoteAddr("192.0.2.10");
            assertEquals(owner.getUserId(), helper.authorizePrincipal(login, jwt).getUserId());

            tokens.revokeToken(token, APILocator.systemUser());
            // isRevoked requires the revocation timestamp to be strictly before the current time.
            DateUtil.sleep(1000);
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(login, jwt));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
            Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
            try {
                if (token != null) {
                    tokens.deleteToken(token, APILocator.systemUser());
                }
            } finally {
                try {
                    if (other != null) {
                        UserDataGen.remove(other, false);
                    }
                } finally {
                    UserDataGen.remove(owner, false);
                }
            }
        }
    }

    /** Follow the company's configured email-or-user-id login convention. */
    private static String davLogin(final User user) {
        return Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }
}
