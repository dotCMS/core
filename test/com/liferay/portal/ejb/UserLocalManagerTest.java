package com.liferay.portal.ejb;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.apache.oro.text.regex.Perl5Compiler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.RegEX;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * Created by nollymar on 6/8/16.
 */
public class UserLocalManagerTest extends TestBase{

    @Test
    public void testValidName() throws DotDataException, SystemException, PortalException, DotSecurityException {
        boolean result = false;

        String firstName;
        String lastName;
        String email;

        email = "user.test@dotcms.com";
        firstName = "TestFirstNameá";
        lastName = "Test Last Name";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        }catch(SystemException | PortalException e){
            throw e;
        }finally {
            userManager.deleteUser(user.getUserId());
        }
    }

    @Test(expected = UserFirstNameException.class)
    public void testInvalidFirstName() throws DotDataException, SystemException, PortalException, DotSecurityException {
        String firstName;
        String lastName;
        String email;

        email = "user.test@dotcms.com";
        firstName = "TestFirstName$";
        lastName = "Test Last Name";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        }catch(SystemException | PortalException e){
            throw e;
        }finally {
            userManager.deleteUser(user.getUserId());
        }
    }

    @Test(expected = UserLastNameException.class)
    public void testInvalidLastName() throws DotDataException, SystemException, PortalException, DotSecurityException {
        String firstName;
        String lastName;
        String email;

        email = "user.test8@dotcms.com";
        firstName = "TestFirstName";
        lastName = "Test Last Name~";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        }catch(SystemException | PortalException e){
            throw e;
        }finally {
            userManager.deleteUser(user.getUserId());
        }
    }
}
