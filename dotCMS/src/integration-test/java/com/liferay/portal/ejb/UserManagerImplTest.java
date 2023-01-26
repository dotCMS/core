package com.liferay.portal.ejb;

import static org.junit.Assert.assertTrue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.auth.Authenticator;

public class UserManagerImplTest {

    static String userId;
    
    
    final static String COMPANY_ID="dotcms.org";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        userId = "user" + UUIDGenerator.shorty();
        
        new UserDataGen()
                        .active(true)
                        .emailAddress(userId + "@dotcms.com")
                        .id(userId)
                        .firstName("UserManagerImplTest")
                        .lastName("User")
                        .password(PasswordFactoryProxy.generateHash(userId))
                        .nextPersisted();
                        
        
        
    }

    
    
    @Test
    public void test_good_login_works_fast() throws Exception {
        
        
        UserManagerImpl userManager = new UserManagerImpl();
        long nowsers = System.currentTimeMillis();
        int result = userManager.authenticateByEmailAddress(COMPANY_ID, userId + "@dotcms.com", userId);
        
        
        // assert we have had less than a 2 second delay
        assertTrue(System.currentTimeMillis()-nowsers < 1000);
        Assert.assertEquals(Authenticator.SUCCESS , result);
        

    }
    
    
    @Test
    public void test_bad_login_password_delay() throws Exception {
        
        
        UserManagerImpl userManager = new UserManagerImpl();
        long nowsers = System.currentTimeMillis();
        int result = userManager.authenticateByEmailAddress(COMPANY_ID, userId + "@dotcms.com", "bad");
        
        
        // assert we have had a 2 second delay
        assertTrue(System.currentTimeMillis()-nowsers >= 2000);
        Assert.assertEquals(Authenticator.FAILURE , result);
        

    }

    
    @Test
    public void test_bad_login_user_delay() throws Exception {
        
        
        UserManagerImpl userManager = new UserManagerImpl();
        long nowsers = System.currentTimeMillis();
        int result = userManager.authenticateByEmailAddress(COMPANY_ID, userId + "@XXXXXX.com", "bad");
        
        
        // assert we have had a 2 second delay
        assertTrue(System.currentTimeMillis()-nowsers >= 2000);
        Assert.assertEquals(Authenticator.DNE , result);
        

    }
    
    
}
