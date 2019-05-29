package com.liferay.portal.ejb;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.model.User;

/**
 * Tests for user management operations (create, validate, delete)
 *
 * @author Nollymar Longa
 */
public class UserLocalManagerTest {

	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

    @Test
    public void testValidName() throws DotDataException, SystemException, PortalException, DotSecurityException {
        boolean result = false;

        String firstName;
        String lastName;
        String email;

        email = "user.test@dotcms.com";
        firstName = "TestFirstName á~";
        lastName = "繁體中文";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        } catch (SystemException | PortalException e) {
            throw e;
        } finally {
            userManager.deleteUser(user.getUserId());
        }
    }

    @Test(expected = UserFirstNameException.class)
    public void testInvalidFirstName() throws DotDataException, SystemException, PortalException, DotSecurityException {
        String firstName;
        String lastName;
        String email;

        email = "user.test@dotcms.com";
        firstName = "Test>FirstName$";
        lastName = "Test Last Name";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        } catch (SystemException | PortalException e) {
            throw e;
        } finally {
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
        lastName = "Test Last< Name";

        User user = APILocator.getUserAPI().createUser(null, email);

        UserLocalManager userManager = new UserLocalManagerImpl();
        try {
            userManager.validate(user.getUserId(), firstName, lastName, email, null);
        } catch (SystemException | PortalException e) {
            throw e;
        } finally {
            userManager.deleteUser(user.getUserId());
        }
    }
    
    @Test
    public void test_user_id_is_UUID() throws DotDataException, SystemException, PortalException, DotSecurityException {

        String email;

        email = "user" + System.currentTimeMillis() + "@dotcms.com";


        User user = APILocator.getUserAPI().createUser(null, email);
        String userId = user.getUserId();

        
        assertTrue(userId.startsWith("user-"));
        String uuidPart = userId.replaceAll("user-", "");
        assertTrue(UUIDUtil.isUUID(uuidPart));

    }
    
    
    
    
}
