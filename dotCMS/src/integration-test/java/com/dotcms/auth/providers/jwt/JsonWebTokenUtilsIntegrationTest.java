package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JsonWebTokenUtilsIntegrationTest {

    private static String userId;
    private static String clusterId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static Date date;
    private static UserAPI userAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        date = dateFormat.parse("04/10/1981");

        //Mocking data
        clusterId = ClusterFactory.getClusterId();
        userAPI = APILocator.getUserAPI();

        //Create User
        final User newUser = new UserDataGen().skinId(UUIDGenerator.generateUuid()).nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), newUser);
        assertTrue(userAPI.isCMSAdmin(newUser));
        userId = newUser.getUserId();
    }

    /**
     * Testing the generateToken JsonWebTokenUtils.getUser
     */
    @Test
    public void get_user_in_token()
            throws DotSecurityException, DotDataException, ParseException {
        final User user = userAPI.loadUserById(userId);
        user.setModificationDate(date);

        //Generate the token service
        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        final String jwtId = user.getRememberMeToken();
        final UserToken userToken = new UserToken.Builder()
                .id(jwtId).subject(userId)
                .modificationDate(date)
                .expiresDate(DateUtil.daysToMillis(2))
                .build();
        String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);

        //Parse the generated token
        final JWToken jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtId);
        assertEquals(jwtBean.getIssuer(), clusterId);
        final String subject = jwtBean.getSubject();
        assertNotNull(subject);
        assertEquals(subject, userId);

        //Get the user
        JsonWebTokenUtils jsonWebTokenUtils = new JsonWebTokenUtils(jsonWebTokenService);
        User userInToken = jsonWebTokenUtils.getUser(jsonWebToken, "127.0.0.1");
        assertNotNull(userInToken);
        assertEquals(user, userInToken);
    }

    /**
     * Testing the generateToken JsonWebTokenUtils.getUser with a modification after the creation of
     * the token, no user should return as the system notice a modification on the user was made
     * after the creation of the token.
     */
    @Test
    public void get_user_in_token_modified()
            throws DotSecurityException, DotDataException, InterruptedException {

        //Generate the token service
        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        final String jwtId = APILocator.getUserAPI().loadUserById(userId).getRememberMeToken();
        //Generate a new token
        final UserToken userToken = new UserToken.Builder().id(jwtId).subject(userId).modificationDate(date)
                .expiresDate(DateUtil.daysToMillis(2))
                .build();
        String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);

        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);

        //Parse the generated token
        final JWToken jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtId);
        assertEquals(jwtBean.getIssuer(), clusterId);
        final String subject = jwtBean.getSubject();
        assertNotNull(subject);
        assertEquals(subject, userId);

        Thread.sleep(1000);

        userAPI.loadUserById(userId).setSkinId("xxxx");

        //Get the user
        JsonWebTokenUtils jsonWebTokenUtils = new JsonWebTokenUtils(jsonWebTokenService);
        User userInToken = jsonWebTokenUtils.getUser(jsonWebToken, "127.0.0.1");
        assertNull(userInToken);
    }

}
