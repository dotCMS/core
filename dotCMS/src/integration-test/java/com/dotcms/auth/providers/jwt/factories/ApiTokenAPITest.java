package com.dotcms.auth.providers.jwt.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.liferay.portal.model.User;


public class ApiTokenAPITest {

    static ApiTokenAPI apiTokenAPI;

    static ApiTokenCache cache;


    static final String USER_ID = "userId1";
    static String REQUESTING_USER_ID;
    static final String REQUESTING_IP = "192.156.2.6";


    @BeforeClass
    public static void prepare() throws Exception {


        IntegrationTestInitService.getInstance().init();
        apiTokenAPI = APILocator.getApiTokenAPI();
        cache = CacheLocator.getApiTokenCache();
        REQUESTING_USER_ID = APILocator.systemUser().getUserId();

        // new DotConnect().setSQL("drop table jwt_token_issued").loadResult();
        // System.out.println(new Task05060CreateTokensIssuedTable().getMySQLScript());
        // new DotConnect().setSQL(new Task05060CreateTokensIssuedTable().getMySQLScript()).loadResult();


        ApiTokenSQL apiTokenSql = ApiTokenSQL.getInstance();
        new DotConnect().setSQL(apiTokenSql.DROP_TOKEN_TABLE).loadResult();

        String[] sqls = apiTokenSql.CREATE_TOKEN_TABLE_SCRIPT().split(";");
        for (String sql : sqls) {
            new DotConnect().setSQL(sql).loadResult();
        }


    }


    Date futureDate(Duration d) {

        return Date.from(Instant.now().plus(d));


    }


    /**
     * gets a token with every field set except id and mod_date, ready to be persisted in the DB
     * 
     * @return
     */
    ApiToken getLoadedToken() {

        final Date expireDate = futureDate(Duration.ofDays(30));
        final Date issueDate = new Date();
        final Date revokedDate = futureDate(Duration.ofDays(2));
        return ApiToken.builder().withIssuer(ClusterFactory.getClusterId()).withExpires(expireDate).withIssueDate(issueDate)
                .withClaims("{'test':'test'}").withRequestingIp("withRequestingIp")
                .withRequestingUserId(APILocator.systemUser().getUserId()).withRevoked(revokedDate).withUserId("withUserId").build();


    }

    ApiToken getSkinnyToken() {


        User user = new UserDataGen().nextPersisted();

        return ApiToken.builder().withIssuer(ClusterFactory.getClusterId()).withExpires(futureDate(Duration.ofDays(10))).withUserId(user.getUserId())
                .withRequestingUserId(APILocator.systemUser().getUserId()).withRequestingIp("127.0.0.1").build();
    }


    @Test
    public void test_apiToken_Cache() {

        ApiToken testToken = getSkinnyToken();

        // save token in db
        ApiToken issued = apiTokenAPI.persistApiToken(testToken, APILocator.systemUser());

        // not in cache
        assert (!cache.getApiToken(issued.id).isPresent());

        // loads cache, returns from db
        ApiToken issuedFromDb = apiTokenAPI.findApiToken(issued.id).get();

        // in cache
        assert (cache.getApiToken(issued.id).isPresent());

        ApiToken issuedFromCache = cache.getApiToken(issued.id).get();

        // this is the same in memory object as was returned from db
        assert (issuedFromCache == issuedFromDb);

        // testing equals method
        assertEquals(issued, issuedFromDb);

        // test remove
        cache.removeApiToken(issued.id);

        assert (!cache.getApiToken(issued.id).isPresent());

    }


    @Test
    public void test_ApiToken_Persistance_Works() {


        ApiToken fatToken = getLoadedToken();
        ApiToken skinnyToken = getSkinnyToken();

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        ApiToken issuedFromDb = apiTokenAPI.findApiToken(skinnyToken.id).get();

        assertEquals(skinnyToken, issuedFromDb);


        fatToken = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());


        issuedFromDb = apiTokenAPI.findApiToken(fatToken.id).get();


        // testing equals method
        assertEquals(fatToken, issuedFromDb);

    }


    @Test
    public void test_ApiToken_Builder() {
        ApiToken fatToken = getLoadedToken();

        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());

        ApiToken testToken = ApiToken.from(fatToken).withId(issued.id).build();

        // testing equals method
        assertEquals(issued, testToken);

    }


    @Test(expected = DotStateException.class)
    public void test_JWT_subnets() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowNetwork("123").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());


    }


    @Test(expected = DotStateException.class)
    public void test_JWT_claims_json() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withClaims("I am not a claim, I should be json").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
    }

    public void test_JWT_allow_all_networks() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowNetwork("0.0.0.0/0").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
        assert (issued.isValid("192.112.123.123"));
    }

    @Test
    public void test_ApiToken_isValid() {


        ApiToken skinnyToken = getSkinnyToken();
        assert (!skinnyToken.isValid());
        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());
        assert (skinnyToken.isValid());
        skinnyToken = apiTokenAPI.findApiToken(skinnyToken.id).get();
        assert (skinnyToken.isValid());


        ApiToken fatToken = ApiToken.from(getLoadedToken()).withRevoked(new Date()).build();
        // no id, not valid
        assert (!fatToken.isValid());
        fatToken = ApiToken.from(getLoadedToken()).withRevoked(null).build();
        fatToken = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());

        // now has id, valid
        assert (fatToken.isValid());

        ApiToken issuedFromDb = apiTokenAPI.findApiToken(fatToken.id).get();
        assert (issuedFromDb.isValid());

        // revoking
        apiTokenAPI.revokeToken(fatToken, APILocator.systemUser());

        // should be invalid
        issuedFromDb = apiTokenAPI.findApiToken(fatToken.id).get();
        assert (!issuedFromDb.isValid());


    }


    public void test_ApiToken_isValid_from_ip() {


        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withAllowNetwork("192.168.211.0/24").build();
        assert (!skinnyToken.isValid());

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        assert (!skinnyToken.isValid());


        assert (skinnyToken.isValid("192.168.211.54"));


    }

    @Test
    public void test_revoke_ApiToken() {
        User user = new UserDataGen().nextPersisted();
        ApiToken skinnyToken = apiTokenAPI.persistApiToken(getSkinnyToken(), APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        ApiToken savedToken = (ApiToken) apiTokenAPI.fromJwt(jwt, "192.168.211.5").get();

        assertEquals(savedToken, skinnyToken);

        apiTokenAPI.revokeToken(savedToken, APILocator.systemUser());

        assertFalse("Optional will return empty b/c token is revoked", apiTokenAPI.fromJwt(jwt).isPresent());

    }

    @Test
    public void test_each_issued_token_should_have_different_ids() {
        ApiToken skinnyToken = apiTokenAPI.persistApiToken(getSkinnyToken(), APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        String jwt2 = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        assertNotEquals(jwt, jwt2);
    }


    @Test
    public void test_404_Cache_in_ApiToken_API() {
        assertFalse("Optional<APIToken>  should be empty", apiTokenAPI.findApiToken("apiBadToken").isPresent());
        assertFalse("Optional<APIToken>  should be empty, even with 404 cached", apiTokenAPI.findApiToken("apiBadToken").isPresent());


    }


    @Test
    public void test_delete_ApiToken() {


        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withUserId(APILocator.systemUser().getUserId()).build();
        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        String jwt2 = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        assertNotEquals(jwt, jwt2);

        ApiToken workingToken = (ApiToken) apiTokenAPI.fromJwt(jwt).get();

        apiTokenAPI.deleteToken(workingToken, APILocator.systemUser());

        try {
            apiTokenAPI.fromJwt(jwt).get();
            assertTrue("jwt has been deleted and should not exist, throwing an exception", false);
        } catch (NoSuchElementException e) {
            assertTrue("jwt has been deleted and should not exist", true);
        }
        try {
            apiTokenAPI.fromJwt(jwt2).get();
            assertTrue("jwt has been deleted and should not exist, throwing an exception", false);
        } catch (NoSuchElementException e) {
            assertTrue("jwt has been deleted and should not exist", true);
        }
    }

    @Test
    public void test_expired_ApiToken() throws Exception {
        long inTheFuture = 5000;
        Date future = new Date(System.currentTimeMillis() + inTheFuture);


        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withUserId(APILocator.systemUser().getUserId()).withExpires(future)

                .build();
        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        String jwt2 = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        assertNotEquals(jwt, jwt2);

        Thread.sleep(inTheFuture);


        assertFalse("Optional will return empty b/c token is expired", apiTokenAPI.fromJwt(jwt).isPresent());


    }


    @Test
    public void test_user_must_be_active_to_validate_ApiToken() throws Exception {


        User user = new UserDataGen().nextPersisted();
        assertTrue(user.isActive());


        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withUserId(user.getUserId()).build();

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        user.setActive(false);
        APILocator.getUserAPI().save(user, APILocator.systemUser(), true);


        assertFalse("API token no longer valid, returns empty", apiTokenAPI.fromJwt(jwt).isPresent());


    }


    @Test
    public void test_Valid_ApiToken_jwt() {

        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withUserId(APILocator.systemUser().getUserId()).build();

        assert (!skinnyToken.isValid());

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        ApiToken savedToken = (ApiToken) apiTokenAPI.fromJwt(jwt).get();

        assertEquals(savedToken, skinnyToken);

    }

    @Test
    public void test_Can_Create_Token_Issue_for_User_with_Expire_Date() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.MILLISECOND, 0);
        final Date expireDate = cal.getTime();
        ApiToken issue = apiTokenAPI.persistApiToken(USER_ID, expireDate, REQUESTING_USER_ID, REQUESTING_IP);


        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(USER_ID, issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expiresDate);

    }
    
    
    @Test(expected = DotStateException.class)
    public void test_revoke_permissions_fail() {

        User user1 = new UserDataGen().nextPersisted();
        User user2 = new UserDataGen().nextPersisted();

        ApiToken issue = null;
        
        try{
            issue = apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);
        }catch(Exception e) {
            assertTrue("should not be here: " + e.getMessage(), false);
        }
        
        // revoke with a user with no perms
        apiTokenAPI.revokeToken(issue, user2);
        assertTrue("should not be here: " , false);

    }

    
    @Test
    public void test_revoke_permissions_work() {

        User user1 = new UserDataGen().nextPersisted();


        ApiToken  issue = apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);

        
        // revoke with a user with no perms

        assertTrue("should be here: " , apiTokenAPI.revokeToken(issue, user1));

    }
    

    @Test(expected = DotStateException.class)
    public void test_get_JWT_permissions_work() {

        User user1 = new UserDataGen().nextPersisted();
        User user2 = new UserDataGen().nextPersisted();

        ApiToken apiToken = null;

        try {
            apiToken = apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);
        } catch (Exception e) {
            assertTrue("should not be here: " + e.getMessage(), false);
        }

        String jwt = apiTokenAPI.getJWT(apiToken, user1);
        assertTrue("jwt should be here: " , jwt!=null);
        
        jwt = apiTokenAPI.getJWT(apiToken, user2);
        assertTrue("should have errored out alreay: " , false);

    }

    
    @Test
    public void test_listing_permissions_work() {

        User user1 = new UserDataGen().nextPersisted();
        User user2 = new UserDataGen().nextPersisted();

        apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);
        apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), REQUESTING_USER_ID, REQUESTING_IP);
        
        // you can see your own tokens
        List<ApiToken> tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, user1);
        assertTrue(tokens.size()==2);
        
        // others cannot
        tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, user2);
        assertTrue(tokens.isEmpty());
        
        // system user can see your tokens
        tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, APILocator.systemUser());
        assertTrue(tokens.size()==2);
        
        

    }
    
    
}
