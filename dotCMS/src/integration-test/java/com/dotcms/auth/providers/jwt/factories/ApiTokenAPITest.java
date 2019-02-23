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


public class ApiTokenAPITest {

    static ApiTokenAPI jwTokenAPI;

    static ApiTokenCache cache;


    static final String USER_ID = "userId1";
    static String REQUESTING_USER_ID;
    static final String REQUESTING_IP = "192.156.2.6";


    @BeforeClass
    public static void prepare() throws Exception {
        
        
        IntegrationTestInitService.getInstance().init();
        jwTokenAPI = APILocator.getApiTokenAPI();
        cache = CacheLocator.getApiTokenCache();
        REQUESTING_USER_ID = APILocator.systemUser().getUserId();
        
        //new DotConnect().setSQL("drop table jwt_token_issued").loadResult();
        //System.out.println(new Task05060CreateTokensIssuedTable().getMySQLScript());
        //new DotConnect().setSQL(new Task05060CreateTokensIssuedTable().getMySQLScript()).loadResult();
        
    }

    
    Date futureDate(Duration d) {
        Instant myDate = Instant.now().plus(d);

        return Date.from(myDate);

        
    }
    
    
    
    /**
     * gets a token with every field set except id and mod_date, 
     * ready to be persisted in the DB
     * @return
     */
    ApiToken getLoadedToken() {
        
        final Date expireDate = futureDate(Duration.ofDays(30));
        final Date issueDate = new Date();
        final Date revokedDate = futureDate(Duration.ofDays(2));
        return ApiToken.builder()
                .withClusterId("withClusterId")
                .withExpires(expireDate)
                .withIssueDate(issueDate)
                .withMetaData("withMetaData")
                .withRequestingIp("withRequestingIp")
                .withRequestingUserId(APILocator.systemUser().getUserId())
                .withRevoked(revokedDate)
                .withUserId("withUserId")
                .build();
        
        
    }
    
    ApiToken getSkinnyToken() {
        return ApiToken
            .builder()
            .withExpires(futureDate(Duration.ofDays(10)))
            .withUserId("testUser")
            .withRequestingUserId(APILocator.systemUser().getUserId())
            .withRequestingIp("127.0.0.1")
            .build();
    }
    
    
    @Test
    public void test_JWT_Issue_Cache() {

        ApiToken testToken = getSkinnyToken();
        
        // save token in db
        ApiToken issued = jwTokenAPI.persistJWTokenIssued(testToken, APILocator.systemUser());

        // not in cache
        assert(!cache.getToken(issued.id).isPresent());
        
        // loads cache, returns from db
        ApiToken issuedFromDb = jwTokenAPI.findJWTokenIssued(issued.id).get();
        
        // in cache
        assert(cache.getToken(issued.id).isPresent());
        
        ApiToken issuedFromCache = cache.getToken(issued.id).get();
        
        // this is the same in memory object as was returned from db
        assert(issuedFromCache == issuedFromDb);
        
        // testing equals method
        assertEquals(issued, issuedFromDb);
        
        // test remove
        cache.removeToken(issued.id);
        
        assert(!cache.getToken(issued.id).isPresent());

    }
    
    
    
    
    
    
    @Test
    public void test_JWT_Issued_Persistance_Works() {

        
        ApiToken fatToken = getLoadedToken();
        ApiToken skinnyToken = getSkinnyToken();
        
        skinnyToken = jwTokenAPI.persistJWTokenIssued(skinnyToken,APILocator.systemUser());

        ApiToken issuedFromDb = jwTokenAPI.findJWTokenIssued(skinnyToken.id).get();

        assertEquals(skinnyToken, issuedFromDb);
        
        
        
        
        
        
        fatToken = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());


        issuedFromDb = jwTokenAPI.findJWTokenIssued(fatToken.id).get();
        
        
        
        
        // testing equals method
        assertEquals(fatToken, issuedFromDb);

    }
    
    
    @Test
    public void test_JWT_Issued_Builder() {
        ApiToken fatToken = getLoadedToken();

        ApiToken issued = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());

        ApiToken testToken=ApiToken.from(fatToken).withId(issued.id).build();
        
        // testing equals method
        assertEquals(issued, testToken);

    }
    
    
    @Test(expected=DotStateException.class)
    public void test_JWT_subnets() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowFromNetwork("123").build();
        assert(!fatToken.isValid());
        ApiToken issued = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());
    }
    

    public void test_JWT_allow_all() {
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withAllowFromNetwork("0.0.0.0/0").build();
        assert(!fatToken.isValid());
        ApiToken issued = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());
    }
    
    @Test
    public void test_JWT_Issue_isValid() {

        
        ApiToken skinnyToken = getSkinnyToken();
        assert(!skinnyToken.isValid());
        skinnyToken = jwTokenAPI.persistJWTokenIssued(skinnyToken,APILocator.systemUser());
        assert(skinnyToken.isValid());
        skinnyToken = jwTokenAPI.findJWTokenIssued(skinnyToken.id).get();
        assert(skinnyToken.isValid());
        
        
        
        ApiToken fatToken = ApiToken.from(getLoadedToken()).withRevoked(new Date()).build();
        // no id, not valid
        assert(!fatToken.isValid());
        fatToken = ApiToken.from(getLoadedToken()).withRevoked(null).build();
        fatToken = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());
        
        // now has id, valid
        assert(fatToken.isValid());
        
        ApiToken issuedFromDb = jwTokenAPI.findJWTokenIssued(fatToken.id).get();
        assert(issuedFromDb.isValid());
        
        //revoking
        jwTokenAPI.revokeToken(fatToken);
        
        //should be invalid
        issuedFromDb = jwTokenAPI.findJWTokenIssued(fatToken.id).get();
        assert(!issuedFromDb.isValid());
        

    }
    
    
    @Test
    public void test_JWT_Issue_isValid_from_ip() {

        
        ApiToken skinnyToken = ApiToken.from(getSkinnyToken()).withAllowFromNetwork("192.168.211.0/24").build();
        
        assert(!skinnyToken.isValid());
        
        skinnyToken = jwTokenAPI.persistJWTokenIssued(skinnyToken,APILocator.systemUser());
        
        assert(!skinnyToken.isValid());

        
        assert(skinnyToken.isValid("192.168.211.54"));
        

    }
    
    
    @Test
    public void test_Can_Create_Token_Issue_for_User_with_Expire_Date() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.MILLISECOND, 0);
        final Date expireDate = cal.getTime();
        ApiToken issue = jwTokenAPI.persistJWTokenIssued(USER_ID, expireDate, REQUESTING_USER_ID, REQUESTING_IP);


        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(USER_ID, issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expires);

    }


}
