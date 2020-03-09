package com.dotmarketing.business.web;

import static org.junit.Assert.assertTrue;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class UserWebAPIImplTest {


    private static User frontEndUser, backEndUser;


    private static UserWebAPIImpl userWebAPI;


    private HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(new MockHttpRequest("localhost", "/api/testing-web-resorce").request()).request();
    }

    private HttpServletRequest frontEndRequest() {
        final HttpServletRequest request = anonymousRequest();
        assertTrue("frontEndUser has frontEnd role", frontEndUser.isFrontendUser());
        request.setAttribute(WebKeys.USER, frontEndUser);
        return request;
    }

    private HttpServletRequest backEndRequest() {
        final HttpServletRequest request = anonymousRequest();
        assertTrue("backEndUser has backend role", backEndUser.isBackendUser());
        request.setAttribute(WebKeys.USER, backEndUser);
        return request;
    }


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();


        frontEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();

        backEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();


        userWebAPI = new UserWebAPIImpl();

    }



    /**
     * Validates that only front end users can be returned from the call to
     * userWebAPI.getLoggedInFrontendUser.
     */
    @Test
    public void test_getLoggedInFrontendUser() {

        assert (userWebAPI.getLoggedInFrontendUser(frontEndRequest()).equals(frontEndUser));
        assert (userWebAPI.getLoggedInFrontendUser(backEndRequest()) == null);

        assert (userWebAPI.getLoggedInFrontendUser(anonymousRequest()) == null);

    }

    /**
     * validates that getLoggedInUser returns any user that is logged into the dotCMS front end or back
     * end
     * 
     */
    @Test
    public void test_getLoggedInUser() {

        assert (userWebAPI.getLoggedInUser(frontEndRequest()).equals(frontEndUser));
        assert (userWebAPI.getLoggedInUser(backEndRequest()).equals(backEndUser));


        assert (userWebAPI.getLoggedInUser(anonymousRequest()) == null);

    }

    /**
     * validates that getUser returns any user that is logged into the dotCMS front end or back end OR
     * returns CMS_ANON if not set
     * 
     */
    @Test
    public void test_getUser() {

        assert (userWebAPI.getUser(backEndRequest()).equals(backEndUser));
        assert (userWebAPI.getUser(frontEndRequest()).equals(frontEndUser));

        assert (userWebAPI.getUser(anonymousRequest()).equals(userWebAPI.getAnonymousUserNoThrow()));

    }

    /**
     * validates that isLoggedToBackend true if front end or back end OR returns CMS_ANON if not set
     * 
     */
    @Test
    public void test_isLoggedToBackend() {

        assert (userWebAPI.isLoggedToBackend(backEndRequest()) == true);
        assert (userWebAPI.isLoggedToBackend(frontEndRequest()) == false);

        assert (userWebAPI.isLoggedToBackend(anonymousRequest()) == false);

    }


    /**
     * validates that getUser returns any user that is logged into the dotCMS front end or back end OR
     * returns CMS_ANON if not set
     * 
     */
    @Test
    public void test_isLoggedToFrontend() {

        assert (userWebAPI.isLoggedToFrontend(backEndRequest()) == false);
        assert (userWebAPI.isLoggedToFrontend(frontEndRequest()) == true);

        assert (userWebAPI.isLoggedToFrontend(anonymousRequest()) == false);

    }



}
