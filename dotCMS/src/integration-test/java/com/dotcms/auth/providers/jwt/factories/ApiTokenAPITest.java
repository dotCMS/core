package com.dotcms.auth.providers.jwt.factories;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;


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

        String[] sqls = apiTokenSql.CREATE_TOKEN_TABLE_SCRIPT.split(";");
        for (String sql : sqls) {
            new DotConnect().setSQL(sql).loadResult();
        }


    }


    Date futureDate(Duration d) {
        Instant myDate = Instant.now().plus(d);

        return Date.from(myDate);


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
        return ApiToken.builder().withClusterId("withClusterId").withExpires(expireDate).withIssueDate(issueDate)
                .withClaims("{'test':'test'}").withRequestingIp("withRequestingIp")
                .withRequestingUserId(APILocator.systemUser().getUserId()).withRevoked(revokedDate).withUserId("withUserId").build();


    }

    ApiToken getSkinnyToken() {
        return ApiToken.builder().withExpires(futureDate(Duration.ofDays(10))).withUserId("testUser")
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
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowFromNetwork("123").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
    }

    @Test(expected = DotStateException.class)
    public void test_JWT_claims_json() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withClaims("I am not a claim, I should be json").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
    }

    public void test_JWT_allow_all() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowFromNetwork("0.0.0.0/0").build();
        assert (!fatToken.isValid());
        ApiToken issued = apiTokenAPI.persistApiToken(fatToken, APILocator.systemUser());
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
        apiTokenAPI.revokeToken(fatToken);

        // should be invalid
        issuedFromDb = apiTokenAPI.findApiToken(fatToken.id).get();
        assert (!issuedFromDb.isValid());


    }


    @Test
    public void test_ApiToken_isValid_from_ip() {


        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withAllowFromNetwork("192.168.211.0/24").build();

        assert (!skinnyToken.isValid());

        skinnyToken = apiTokenAPI.persistApiToken(skinnyToken, APILocator.systemUser());

        assert (!skinnyToken.isValid());


        assert (skinnyToken.isValid("192.168.211.54"));


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
        assertEquals(expireDate, issue.expires);

    }


}
