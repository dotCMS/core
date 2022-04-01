package com.dotmarketing.util;

import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import org.junit.Test;

public class UserUtilTest {

    /**
     * Simply test we're getting back chars that match our request
     */
    @Test
    public void Test_Generate_Password_Chars_Are_Valid(){
        String p = "[" + UserUtils.ACCEPTABLE_PASSWORD_CHARS + "]";
        final Pattern pattern = Pattern.compile(p);
        //Lets do this a few times just to be sure
        for(int i=0; i<=10; i++ ) {
            final String password = UserUtils.generateSecurePassword();
            assertTrue(pattern.matcher(password).find());
        }
    }

}
