package com.dotcms.auth.providers.jwt.factories;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssue;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;


public class JWTokenAPITest {

    static JWTokenAPI jwTokenAPI;

    static JWTokenCache cache;


    static final String USER_ID = "userId1";
    static final String REQUESTING_USER_ID = "requestingUser";
    static final String REQUESTING_IP = "192.0156.2.6";


    @BeforeClass
    public static void prepare() throws Exception {
        
        
        IntegrationTestInitService.getInstance().init();
        jwTokenAPI = APILocator.getJWTTokenAPI();
        cache = CacheLocator.getJWTokenCache();
    }

    
    Date futureDate(Duration d) {
        Instant myDate = Instant.now().plus(d);

        return Date.from(myDate.truncatedTo(ChronoUnit.SECONDS));

        
    }
    
    
    
    /**
     * gets a token with every field set except id and mod_date, 
     * ready to be persisted in the DB
     * @return
     */
    JWTokenIssue getLoadedToken() {
        
        final Date expireDate = futureDate(Duration.ofDays(30));
        final Date issueDate = new Date();
        final Date revokedDate = futureDate(Duration.ofDays(2));
        return JWTokenIssue.builder()
                .withAllowFromNetwork("withAllowFromNetwork")
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
    
    
    
    @Test
    public void test_JWT_Issue_Cache() {

        JWTokenIssue testToken = JWTokenIssue.builder().withUserId("userId").withExpires(futureDate(Duration.ofDays(30))).build();
        
        // save token in db
        JWTokenIssue issued = jwTokenAPI.persistJWTokenIssue(testToken, APILocator.systemUser());

        // not in cache
        assert(!cache.getToken(issued.id).isPresent());
        
        // loads cache, returns from db
        JWTokenIssue issuedFromDb = jwTokenAPI.findJWTokenIssue(issued.id).get();
        
        // in cache
        assert(cache.getToken(issued.id).isPresent());
        
        JWTokenIssue issuedFromCache = cache.getToken(issued.id).get();
        
        // this is the same in memory object as was returned from db
        assert(issuedFromCache == issuedFromDb);
        
        // testing equals method
        assertEquals(issued, issuedFromDb);
        
        // test remove
        cache.removeToken(issued.id);
        
        assert(!cache.getToken(issued.id).isPresent());

    }
    
    
    
    
    
    
    @Test
    public void test_JWT_Issue_Persistance_Works() {

        JWTokenIssue fatToken = getLoadedToken();
        
        
        JWTokenIssue issued = jwTokenAPI.persistJWTokenIssue(fatToken,APILocator.systemUser());


        JWTokenIssue issuedFromDb = jwTokenAPI.findJWTokenIssue(issued.id).get();
        
        
        
        
        // testing equals method
        assertEquals(issued, issuedFromDb);

    }
    
    
    @Test
    public void test_JWT_Issue_Builder() {
        JWTokenIssue fatToken = getLoadedToken();

        JWTokenIssue issued = jwTokenAPI.persistJWTokenIssue(fatToken,APILocator.systemUser());

        JWTokenIssue testToken=JWTokenIssue.from(fatToken).withId(issued.id).build();
        
        // testing equals method
        assertEquals(issued, testToken);

    }
    
    
    @Test
    public void test_JWT_Issue_isValid() {

        
        JWTokenIssue fatToken = JWTokenIssue.from(getLoadedToken()).withRevoked(new Date()).build();
        // no id, not valid
        assert(!fatToken.isValid());
        fatToken = JWTokenIssue.from(getLoadedToken()).withRevoked(null).build();
        fatToken = jwTokenAPI.persistJWTokenIssue(fatToken,APILocator.systemUser());
        
        // now has id, valid
        assert(fatToken.isValid());
        
        JWTokenIssue issuedFromDb = jwTokenAPI.findJWTokenIssue(fatToken.id).get();
        assert(issuedFromDb.isValid());
        
        //revoking
        jwTokenAPI.revokeToken(fatToken);
        
        //should be invalid
        issuedFromDb = jwTokenAPI.findJWTokenIssue(fatToken.id).get();
        assert(!issuedFromDb.isValid());
        


    }
    
    
    
    
    @Test
    public void test_Can_Create_Token_Issue_for_User_with_Expire_Date() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.MILLISECOND, 0);
        final Date expireDate = cal.getTime();
        JWTokenIssue issue = jwTokenAPI.persistJWTokenIssue(USER_ID, expireDate, REQUESTING_USER_ID, REQUESTING_IP);


        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(USER_ID, issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expires);

    }


}
