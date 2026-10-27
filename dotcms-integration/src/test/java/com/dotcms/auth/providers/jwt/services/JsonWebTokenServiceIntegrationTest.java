package com.dotcms.auth.providers.jwt.services;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import java.lang.reflect.Field;
import java.security.Key;
import java.util.Arrays;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class JsonWebTokenServiceIntegrationTest {

    private static JsonWebTokenService jsonWebTokenService;
    private static String userId;
    final String jwtId = "jwt1";
    private static String clusterId;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //Mocking data
        clusterId = ClusterFactory.getClusterId();

        //Generate the token service
        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Create User
        final User newUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadCMSAdminRole(), newUser);
        assertTrue(APILocator.getUserAPI().isCMSAdmin(newUser));
        userId = newUser.getUserId();
    }

    /**
     * Testing the generateToken JsonWebTokenServiceTest
     */
    @Test
    public void generateTokenTest() throws DotSecurityException, DotDataException {

        final User user = APILocator.getUserAPI().loadUserById(userId);
        //Generate a new token
        final String jwtokenId    = user.getRememberMeToken();
        final UserToken userToken = new UserToken.Builder()
                .id(jwtokenId)
                .subject(userId)
                .issuer(clusterId)
                .expiresDate(DateUtil.daysToMillis(2))
                .claims(ImmutableMap.of())
                .build();
        String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);

        //Parse the generated token
        final JWToken jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtokenId);
        assertEquals(jwtBean.getIssuer(), clusterId);
        final String subject = jwtBean.getSubject();
        assertNotNull(subject);
        assertEquals(subject, userId);
    }

    /**
     * Testing the generateToken JsonWebTokenServiceTest
     */
    @Test(expected = ExpiredJwtException.class)
    public void generateToken_expired_token_Test() throws ParseException, DotSecurityException, DotDataException {

        final User user = APILocator.getUserAPI().loadUserById(userId);
        //Generate a new token
        final UserToken userToken = new UserToken.Builder().id(user.getRememberMeToken())
                .subject(userId)
                .issuer(clusterId)
                .modificationDate(new Date())
                .expiresDate(DateUtil.addDate(new Date(), Calendar.MONTH,-2))
                .claims(ImmutableMap.of())
                .build();

        final String jsonWebToken = jsonWebTokenService.generateUserToken(userToken);

        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);

        //Parse the expired token
        final JWToken jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
    }

    /**
     * Testing the generateToken and parseToken but trying to simulate the use of a token in a
     * different server.
     */
    @Test(expected = IncorrectClaimException.class)
    public void generateToken_incorrect_issuer() throws DotSecurityException, DotDataException {

        final User user = APILocator.getUserAPI().loadUserById(userId);
        //Generate a new token

        final String jsonWebToken = jsonWebTokenService.generateUserToken(
                new UserToken.Builder().id(user.getRememberMeToken()).subject(userId).expiresDate(DateUtil.daysToMillis(2)).build());
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);

        /*
        Change the existing cluster id in order to simulate we are using the token in a
        different server.
         */
        set(jsonWebTokenService, "issuerId", "ANOTHER-CLUSTER-456");

        // Set config to validate the issuer
        Config.setProperty("JWT_CLAIM_ISSUER_VALIDATE", true);

        //Parse the generated token
        jsonWebTokenService.parseToken(jsonWebToken);
    }

    /**
     * Given: a stored API token and the server's signing key
     * When: a JWT is signed for that token ID but with issue and expiry dates that differ from the
     *       stored record (as a forged token would)
     * Then: the JWT is rejected, while the JWT the server issues for the same token is accepted
     */
    @Test
    public void apiToken_jwt_with_claims_not_matching_record_is_rejected() throws Exception {
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(userId,
                DateUtil.addDate(new Date(), Calendar.YEAR, 1), userId, "127.0.0.1");
        try {
            final String issued = jsonWebTokenService.generateApiToken(apiToken);
            assertNotNull(jsonWebTokenService.parseToken(issued));

            final Key key = (Key) get(jsonWebTokenService, "signingKey");
            final SignatureAlgorithm algorithm = Arrays.stream(SignatureAlgorithm.values())
                    .filter(alg -> key.getAlgorithm().equals(alg.getJcaName())).findFirst().get();
            final Date now = new Date();
            final String forged = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setId("forged-" + now.getTime())
                    .setSubject(apiToken.id)
                    .setIssuer(clusterId)
                    .setIssuedAt(now)
                    .setNotBefore(now)
                    .setExpiration(new Date(now.getTime() + 3_600_000L))
                    .signWith(algorithm, key)
                    .compact();
            try {
                jsonWebTokenService.parseToken(forged);
                fail("A JWT whose iat/exp do not match the stored token must be rejected");
            } catch (final IncorrectClaimException expected) {
                // expected
            }
        } finally {
            APILocator.getApiTokenAPI().deleteToken(apiToken, APILocator.systemUser());
        }
    }

    private Object get(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, fieldValue);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @After
    public void cleanUp() {
        set(jsonWebTokenService, "issuerId", clusterId);
    }
}
