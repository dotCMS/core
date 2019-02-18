package com.dotcms.auth.providers.jwt.factories;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssued;
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
    JWTokenIssued getLoadedToken() {
        
        final Date expireDate = futureDate(Duration.ofDays(30));
        final Date issueDate = new Date();
        final Date revokedDate = futureDate(Duration.ofDays(2));
        return JWTokenIssued.builder()
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

        JWTokenIssued testToken = JWTokenIssued.builder().withUserId("userId").withExpires(futureDate(Duration.ofDays(30))).build();
        
        // save token in db
        JWTokenIssued issued = jwTokenAPI.persistJWTokenIssued(testToken, APILocator.systemUser());

        // not in cache
        assert(!cache.getToken(issued.id).isPresent());
        
        // loads cache, returns from db
        JWTokenIssued issuedFromDb = jwTokenAPI.findJWTokenIssued(issued.id).get();
        
        // in cache
        assert(cache.getToken(issued.id).isPresent());
        
        JWTokenIssued issuedFromCache = cache.getToken(issued.id).get();
        
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

        JWTokenIssued fatToken = getLoadedToken();
        
        
        JWTokenIssued issued = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());


        JWTokenIssued issuedFromDb = jwTokenAPI.findJWTokenIssued(issued.id).get();
        
        
        
        
        // testing equals method
        assertEquals(issued, issuedFromDb);

    }
    
    
    @Test
    public void test_JWT_Issue_Builder() {
        JWTokenIssued fatToken = getLoadedToken();

        JWTokenIssued issued = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());

        JWTokenIssued testToken=JWTokenIssued.from(fatToken).withId(issued.id).build();
        
        // testing equals method
        assertEquals(issued, testToken);

    }
    
    
    @Test
    public void test_JWT_Issue_isValid() {

        
        JWTokenIssued fatToken = JWTokenIssued.from(getLoadedToken()).withRevoked(new Date()).build();
        // no id, not valid
        assert(!fatToken.isValid());
        fatToken = JWTokenIssued.from(getLoadedToken()).withRevoked(null).build();
        fatToken = jwTokenAPI.persistJWTokenIssued(fatToken,APILocator.systemUser());
        
        // now has id, valid
        assert(fatToken.isValid());
        
        JWTokenIssued issuedFromDb = jwTokenAPI.findJWTokenIssued(fatToken.id).get();
        assert(issuedFromDb.isValid());
        
        //revoking
        jwTokenAPI.revokeToken(fatToken);
        
        //should be invalid
        issuedFromDb = jwTokenAPI.findJWTokenIssued(fatToken.id).get();
        assert(!issuedFromDb.isValid());
        


    }
    
    
    
    
    @Test
    public void test_Can_Create_Token_Issue_for_User_with_Expire_Date() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.MILLISECOND, 0);
        final Date expireDate = cal.getTime();
        JWTokenIssued issue = jwTokenAPI.persistJWTokenIssued(USER_ID, expireDate, REQUESTING_USER_ID, REQUESTING_IP);


        assertEquals(REQUESTING_IP, issue.requestingIp);
        assertEquals(USER_ID, issue.userId);
        assertEquals(REQUESTING_USER_ID, issue.requestingUserId);
        assertEquals(expireDate, issue.expires);

    }


}
