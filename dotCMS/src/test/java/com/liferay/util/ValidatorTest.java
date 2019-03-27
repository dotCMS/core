package com.liferay.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Oscar Arrieta on 6/19/17.
 */
public class ValidatorTest extends UnitTestBase {

    @Test
    public void testValidEmails(){

        final List<String> validEmails =
                Arrays.asList( "john.doe@dotcms.com", "james.sa'd@test.com",
                        "john+test@dotcms.com", "john+test@test.test.test.test.ag" );

        for ( String validEmail : validEmails ) {
            assertTrue( Validator.isEmailAddress( validEmail ) );
        }
        
    }

    @Test
    public void testInvalidEmails(){

        final List<String> invalidEmails = Arrays.asList( null, "", "abc.example.com", "a@b@c@example.com" );

        for ( String invalidEmail : invalidEmails ) {
            assertFalse( Validator.isEmailAddress( invalidEmail ) );
        }

    }

}
