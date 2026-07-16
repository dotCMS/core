package com.dotcms.auth.providers.jwt.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApiTokenAPITest extends IntegrationTestBase {

    static ApiTokenAPI apiTokenAPI;

    static ApiTokenCache cache;

    static User TOKEN_USER = null;

    static String REQUESTING_USER_ID;
    static final String REQUESTING_IP = "192.156.2.6";

    static final String IP_NETWORK = "192.168.211.0/24";
    static final String IN_NETWORK = "192.168.211.55";
    static final String OUT_OF_NETWORK = "10.0.0.4";

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
        apiTokenAPI = APILocator.getApiTokenAPI();
        cache = CacheLocator.getApiTokenCache();
        REQUESTING_USER_ID = APILocator.systemUser().getUserId();
        TOKEN_USER = new UserDataGen().nextPersisted();
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
                .withRequestingUserId(APILocator.systemUser().getUserId()).withRevoked(revokedDate).withUserId(TOKEN_USER.getUserId())
                .build();

    }

    ApiToken getSkinnyToken() {

        return ApiToken.builder().withIssuer(ClusterFactory.getClusterId()).withExpires(futureDate(Duration.ofDays(10)))
                .withUserId(TOKEN_USER.getUserId()).withRequestingUserId(APILocator.systemUser().getUserId()).withRequestingIp("127.0.0.1")
                .build();
    }

    @Test
    public void test_apiToken_Cache() {

        ApiToken testToken = getSkinnyToken();

        // save token in db
        ApiToken issued = apiTokenAPI.persistApiToken(testToken, APILocator.systemUser());

        // not in cache
        assertFalse(cache.getApiToken(issued.id).isPresent());

        // loads cache, returns from db
        ApiToken issuedFromDb = apiTokenAPI.findApiToken(issued.id).get();

        // in cache
        assertTrue(cache.getApiToken(issued.id).isPresent());

        ApiToken issuedFromCache = cache.getApiToken(issued.id).get();

        // this is the same in memory object as was returned from db
        assertTrue(issuedFromCache == issuedFromDb);

        // testing equals method
        assertEquals(issued, issuedFromDb);

        // test remove
        cache.removeApiToken(issued.id);

        assertFalse(cache.getApiToken(issued.id).isPresent());
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

        ApiToken testToken = ApiToken.from(fatToken)
                .withId(issued.id)
                .withIssueDate(issued.issueDate)
                .withModDate(issued.modificationDate)
                .build();

        // testing equals method
        assertEquals(issued, testToken);

    }

    @Test(expected = DotStateException.class)
    public void test_JWT_subnets() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowNetwork("123").build();
        assertFalse(fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
        assertFalse(issued.isValid());

    }

    @Test
    public void test_JWT_claims_json() {

        final String key = "realClaim";
        final String value = "I am a claim, I am json";
        final String json = "{\"" + key + "\":\"" + value + "\"}";

        ApiToken fatToken = ApiToken.from(getLoadedToken()).withClaims("I am not a claim, I should be json").build();
        assert (fatToken.claims.isEmpty());
        fatToken = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
        assert (fatToken.isValid());

        fatToken = ApiToken.from(getLoadedToken()).withClaims(json).build();
        assert (fatToken.claims.size() == 1);
        assert (fatToken.claims.get(key).equals(value));

        fatToken = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
        fatToken = apiTokenAPI.findApiToken(fatToken.id).get();
        assert (fatToken.isValid());
        assert (fatToken.claims.size() == 1);
        assert (fatToken.claims.get(key).equals(value));

        String jwt = apiTokenAPI.getJWT(fatToken, APILocator.systemUser());

        JWToken token = JsonWebTokenFactory.getInstance().getJsonWebTokenService().parseToken(jwt);

        assert (token.getClaims().size() == 1);
        assert (token.getClaims().get(key).equals(value));

    }

    @Test
    public void test_JWT_allow_all_networks() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowNetwork("0.0.0.0/0").build();
        assertFalse(fatToken.isValid());
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

        // let's wait a bit so revoking time gets passed
        DateUtil.sleep(1000);

        // should be invalid
        issuedFromDb = apiTokenAPI.findApiToken(fatToken.id).get();
        assert (!issuedFromDb.isValid());

    }

    @Test
    public void test_ApiToken_isValid_from_ip() {

        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withAllowNetwork("192.168.211.0/24").build();
        assert (!skinnyToken.isValid());

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        assert (!skinnyToken.isValid());

        assert (skinnyToken.isValid("192.168.211.54"));

        assertFalse(skinnyToken.isValid("10.10.20.54"));
    }

    @Test
    public void test_JWT_isValid_from_ip() {

        SubnetUtils utils = new SubnetUtils(IP_NETWORK);
        utils.setInclusiveHostCount(true);
        assertTrue(utils.getInfo().isInRange(IN_NETWORK));

        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withAllowNetwork(IP_NETWORK).build();
        assertTrue(skinnyToken.isInIpRange(IN_NETWORK));

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        assertFalse(apiTokenAPI.fromJwt(jwt).isPresent());
        assertTrue(apiTokenAPI.fromJwt(jwt, IN_NETWORK).isPresent());
        assertFalse(apiTokenAPI.fromJwt(jwt, OUT_OF_NETWORK).isPresent());

    }

    @Test
    public void test_revoke_ApiToken() {
        User user = new UserDataGen().nextPersisted();
        ApiToken skinnyToken = apiTokenAPI.persistApiToken(getSkinnyToken(), APILocator.systemUser());

        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());

        ApiToken savedToken = (ApiToken) apiTokenAPI.fromJwt(jwt, "192.168.211.5").get();

        assertEquals(savedToken, skinnyToken);

        apiTokenAPI.revokeToken(savedToken, APILocator.systemUser());

        // let's wait a bit so revoking time gets passed
        DateUtil.sleep(1000);

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
            fail("jwt has been deleted and should not exist, throwing an exception");
        } catch (NoSuchElementException e) {
            assertTrue("jwt has been deleted and should not exist", true);
        }
        try {
            apiTokenAPI.fromJwt(jwt2).get();
            fail("jwt has been deleted and should not exist, throwing an exception");
        } catch (NoSuchElementException e) {
            assertTrue("jwt has been deleted and should not exist", true);
        }
    }

    @Test
    public void test_expired_ApiToken() throws Exception {
        long inTheFuture = 5000;
        Date future = new Date(System.currentTimeMillis() + inTheFuture);

        ApiToken skinnyToken = ApiToken.from(getSkinnyToken())
                .withUserId(APILocator.systemUser().getUserId()).withExpires(future)
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

        final Company company = new CompanyDataGen()
                .name("TestCompany")
                .shortName("TC")
                .authType("email")
                .autoLogin(true)
                .emailAddress("lol2@dotCMS.com")
                .homeURL("localhost")
                .city("NYC")
                .mx("MX")
                .type("test")
                .phone("5552368")
                .portalURL("/portalURL")
                .nextPersisted();
        assertNotNull(company.getCompanyId());
        final Company retrievedCompany =  CompanyUtil.findByPrimaryKey(company.getCompanyId());
        assertEquals(company.getCompanyId(), retrievedCompany.getCompanyId());
        User user = new UserDataGen().active(true)
                .skinId(UUIDGenerator.generateUuid())
                .companyId(retrievedCompany.getCompanyId())
                .nextPersisted();
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
        ApiToken issue = apiTokenAPI.persistApiToken(TOKEN_USER.getUserId(), expireDate, REQUESTING_USER_ID, REQUESTING_IP);

        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(TOKEN_USER.getUserId(), issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expiresDate);

    }

    @Test
    public void test_Can_Create_Token_Issue_for_User_with_Expired_Date() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1); // todo; ask Will if this is ok
        cal.set(Calendar.MILLISECOND, 0);
        final Date expireDate = cal.getTime();
        ApiToken issue = apiTokenAPI.persistApiToken(TOKEN_USER.getUserId(), expireDate, REQUESTING_USER_ID, REQUESTING_IP);

        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(TOKEN_USER.getUserId(), issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expiresDate);
        assertTrue(issue.isExpired());

    }

    @Test(expected = DotStateException.class)
    public void test_revoke_permissions_fail() {

        User user1 = new UserDataGen().nextPersisted();
        User user2 = new UserDataGen().nextPersisted();

        ApiToken issue = null;

        try {
            issue = apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);
        } catch (Exception e) {
            assertTrue("should not be here: " + e.getMessage(), false);
        }

        // revoke with a user with no perms
        apiTokenAPI.revokeToken(issue, user2);
        assertTrue("should not be here: ", false);

    }

    @Test
    public void test_revoke_permissions_work() {

        User user1 = new UserDataGen().nextPersisted();

        ApiToken issue = apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);

        // revoke with a user with no perms

        assertTrue("should be here: ", apiTokenAPI.revokeToken(issue, user1));

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
        assertTrue("jwt should be here: ", jwt != null);

        jwt = apiTokenAPI.getJWT(apiToken, user2);
        assertTrue("should have errored out already: ", false);

    }

    @Test
    public void test_listing_permissions_work() {

        User user1 = new UserDataGen().nextPersisted();
        User user2 = new UserDataGen().nextPersisted();

        apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), user1.getUserId(), REQUESTING_IP);
        apiTokenAPI.persistApiToken(user1.getUserId(), futureDate(Duration.ofDays(30)), REQUESTING_USER_ID, REQUESTING_IP);

        // you can see your own tokens
        List<ApiToken> tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, user1);
        assertTrue(tokens.size() == 2);

        // others cannot
        tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, user2);
        assertTrue(tokens.isEmpty());

        // system user can see your tokens
        tokens = apiTokenAPI.findApiTokensByUserId(user1.getUserId(), false, APILocator.systemUser());
        assertTrue(tokens.size() == 2);

    }

    /**
     * API level token validation test: With invalid input
     */
    @Test
    public void testNonWellFormedToken(){
        assertFalse(apiTokenAPI.isWellFormedToken("any"));
    }

    /**
     * API level token validation test: With a valid input
     */
    @Test
    public void testWellFormedToken() {
        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withUserId(APILocator.systemUser().getUserId()).build();
        assert (!skinnyToken.isValid());
        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());
        String jwt = apiTokenAPI.getJWT(skinnyToken, APILocator.systemUser());
        assertTrue(apiTokenAPI.isWellFormedToken(jwt));

    }

    /**
     * Test findExpiringTokens method returns tokens expiring within specified days
     */
    @Test
    public void testFindExpiringTokens() throws DotDataException, DotSecurityException {
        User adminUser = APILocator.getUserAPI().getSystemUser();
        User regularUser = new UserDataGen().nextPersisted();
        
        // Create tokens expiring in different timeframes
        ApiToken expiringInFiveDays = ApiToken.builder()
                .withIssuer(ClusterFactory.getClusterId())
                .withExpires(futureDate(Duration.ofDays(5)))
                .withUserId(regularUser.getUserId())
                .withRequestingUserId(adminUser.getUserId())
                .withRequestingIp("127.0.0.1")
                .build();
        
        ApiToken expiringInTenDays = ApiToken.builder()
                .withIssuer(ClusterFactory.getClusterId())
                .withExpires(futureDate(Duration.ofDays(10)))
                .withUserId(regularUser.getUserId())
                .withRequestingUserId(adminUser.getUserId())
                .withRequestingIp("127.0.0.1")
                .build();
        
        ApiToken expiringInTwentyDays = ApiToken.builder()
                .withIssuer(ClusterFactory.getClusterId())
                .withExpires(futureDate(Duration.ofDays(20)))
                .withUserId(regularUser.getUserId())
                .withRequestingUserId(adminUser.getUserId())
                .withRequestingIp("127.0.0.1")
                .build();
        
        // Persist tokens
        expiringInFiveDays = apiTokenAPI.persistApiToken(expiringInFiveDays, adminUser);
        expiringInTenDays = apiTokenAPI.persistApiToken(expiringInTenDays, adminUser);
        expiringInTwentyDays = apiTokenAPI.persistApiToken(expiringInTwentyDays, adminUser);
        
        // Test different lookahead periods
        List<ApiToken> tokensExpiring7Days = apiTokenAPI.findExpiringTokens(7, adminUser);
        List<ApiToken> tokensExpiring15Days = apiTokenAPI.findExpiringTokens(15, adminUser);
        List<ApiToken> tokensExpiring30Days = apiTokenAPI.findExpiringTokens(30, adminUser);

        final ApiToken expiringInFiveDaysFinal = expiringInFiveDays;
        final ApiToken expiringInTenDaysFinal = expiringInTenDays;
        final ApiToken expiringInTwentyDaysFinal = expiringInTwentyDays;

        // Check if the correct tokens are returned
        boolean fiveDayTokenFound7 = tokensExpiring7Days.stream()
                .anyMatch(token -> token.id.equals(expiringInFiveDaysFinal.id));
        boolean tenDayTokenFound7 = tokensExpiring7Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTenDaysFinal.id));
        boolean twentyDayTokenFound7 = tokensExpiring7Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTwentyDaysFinal.id));
        
        boolean fiveDayTokenFound15 = tokensExpiring15Days.stream()
                .anyMatch(token -> token.id.equals(expiringInFiveDaysFinal.id));
        boolean tenDayTokenFound15 = tokensExpiring15Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTenDaysFinal.id));
        boolean twentyDayTokenFound15 = tokensExpiring15Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTwentyDaysFinal.id));
        
        boolean fiveDayTokenFound30 = tokensExpiring30Days.stream()
                .anyMatch(token -> token.id.equals(expiringInFiveDaysFinal.id));
        boolean tenDayTokenFound30 = tokensExpiring30Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTenDaysFinal.id));
        boolean twentyDayTokenFound30 = tokensExpiring30Days.stream()
                .anyMatch(token -> token.id.equals(expiringInTwentyDaysFinal.id));
        
        // Assertions for 7-day lookahead
        assertTrue("Token expiring in 5 days should be found in 7-day lookahead", fiveDayTokenFound7);
        assertFalse("Token expiring in 10 days should NOT be found in 7-day lookahead", tenDayTokenFound7);
        assertFalse("Token expiring in 20 days should NOT be found in 7-day lookahead", twentyDayTokenFound7);
        
        // Assertions for 15-day lookahead
        assertTrue("Token expiring in 5 days should be found in 15-day lookahead", fiveDayTokenFound15);
        assertTrue("Token expiring in 10 days should be found in 15-day lookahead", tenDayTokenFound15);
        assertFalse("Token expiring in 20 days should NOT be found in 15-day lookahead", twentyDayTokenFound15);
        
        // Assertions for 30-day lookahead
        assertTrue("Token expiring in 5 days should be found in 30-day lookahead", fiveDayTokenFound30);
        assertTrue("Token expiring in 10 days should be found in 30-day lookahead", tenDayTokenFound30);
        assertTrue("Token expiring in 20 days should be found in 30-day lookahead", twentyDayTokenFound30);
        
        // Test role-based filtering
        List<ApiToken> regularUserTokens = apiTokenAPI.findExpiringTokens(30, regularUser);
        
        // Regular user should only see their own tokens
        assertTrue("Regular user should see their own tokens", regularUserTokens.size() <= 3);
        
        // Clean up
        try {
            apiTokenAPI.deleteToken(expiringInFiveDays, adminUser);
            apiTokenAPI.deleteToken(expiringInTenDays, adminUser);
            apiTokenAPI.deleteToken(expiringInTwentyDays, adminUser);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

}
