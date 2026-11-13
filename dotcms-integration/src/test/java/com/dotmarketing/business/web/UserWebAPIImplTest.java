package com.dotmarketing.business.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class UserWebAPIImplTest {


    private static User frontEndUser, backEndUser;


    private static UserWebAPIImpl userWebAPI;

    /** Users created by the tests below, removed once after the class. */
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Removes the users the tests created.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(UserDataGen::remove);
    }


    private HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/api/testing-web-resorce").request()).request();
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

    /**
     * Method to test: {@link UserWebAPIImpl#getLoggedInUser(HttpServletRequest)},
     * {@link UserWebAPIImpl#isLoggedToBackend(HttpServletRequest)} and
     * {@link UserWebAPIImpl#getUser(HttpServletRequest)}
     * Given Scenario: a back-end user is logged in, so the session holds the user id, and is then
     * made inactive through {@link UserAPI#save}.
     * ExpectedResult: before the change the session answers the user, who is logged in to the back
     * end; from the next request on it answers no user, is not logged in, counts as anonymous, and
     * the user id is gone from the session.
     */
    @Test
    public void test_userMadeInactiveAfterLogin_isNoLongerLoggedIn() throws Exception {
        final User user = newBackEndUser();
        createdUsers.add(user);
        final HttpSession session = sessionOf(user);
        final HttpServletRequest before = requestOnSession(session);
        assertEquals(user, userWebAPI.getLoggedInUser(before));
        assertTrue(userWebAPI.isLoggedToBackend(before));

        final User toSave = (User) APILocator.getUserAPI().loadUserById(user.getUserId()).clone();
        toSave.setActive(false);
        APILocator.getUserAPI().save(toSave, APILocator.systemUser(), false);

        final HttpServletRequest after = requestOnSession(session);
        assertNull("a user made inactive is not logged in any more", userWebAPI.getLoggedInUser(after));
        assertFalse(userWebAPI.isLoggedToBackend(after));
        assertEquals(userWebAPI.getAnonymousUserNoThrow(), userWebAPI.getUser(after));
        assertNull("the session no longer names the user", session.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Method to test: {@link UserWebAPIImpl#getLoggedInUser(HttpServletRequest)},
     * {@link UserWebAPIImpl#isLoggedToBackend(HttpServletRequest)} and
     * {@link UserWebAPIImpl#getUser(HttpServletRequest)}
     * Given Scenario: a back-end user is logged in and is then deleted through
     * {@link UserAPI#delete}.
     * ExpectedResult: from the next request on the session answers no user, is not logged in,
     * counts as anonymous, and the user id is gone from the session.
     */
    @Test
    public void test_userDeletedAfterLogin_isNoLongerLoggedIn() throws Exception {
        final User user = newBackEndUser();
        final HttpSession session = sessionOf(user);
        final HttpServletRequest before = requestOnSession(session);
        assertEquals(user, userWebAPI.getLoggedInUser(before));
        assertTrue(userWebAPI.isLoggedToBackend(before));

        APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(user.getUserId()),
                APILocator.systemUser(), APILocator.systemUser(), false);

        final HttpServletRequest after = requestOnSession(session);
        assertNull("a deleted user is not logged in any more", userWebAPI.getLoggedInUser(after));
        assertFalse(userWebAPI.isLoggedToBackend(after));
        assertEquals(userWebAPI.getAnonymousUserNoThrow(), userWebAPI.getUser(after));
        assertNull("the session no longer names the user", session.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Method to test: {@link UserWebAPIImpl#getLoggedInUser(HttpServletRequest)} and
     * {@link UserWebAPIImpl#getUser(HttpServletRequest)}
     * Given Scenario: a login filter identifies the user by setting the user id on the request
     * (no session user), first while the user is active and again after the user is made inactive.
     * ExpectedResult: the active user is the logged-in user; once inactive, the request has no
     * logged-in user and counts as anonymous.
     */
    @Test
    public void test_userIdOnRequest_isResolvedOnlyWhileTheUserIsActive() throws Exception {
        final User user = newBackEndUser();
        createdUsers.add(user);
        final HttpServletRequest whileActive = anonymousRequest();
        whileActive.setAttribute(WebKeys.USER_ID, user.getUserId());
        assertEquals(user, userWebAPI.getLoggedInUser(whileActive));

        final User toSave = (User) APILocator.getUserAPI().loadUserById(user.getUserId()).clone();
        toSave.setActive(false);
        APILocator.getUserAPI().save(toSave, APILocator.systemUser(), false);

        final HttpServletRequest onceInactive = anonymousRequest();
        onceInactive.setAttribute(WebKeys.USER_ID, user.getUserId());
        assertNull("an inactive user named on the request is not logged in",
                userWebAPI.getLoggedInUser(onceInactive));
        assertEquals(userWebAPI.getAnonymousUserNoThrow(), userWebAPI.getUser(onceInactive));
    }

    private static User newBackEndUser() throws Exception {
        return new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    }

    /**
     * A session as the login leaves it: it holds the user id and no copy of the user.
     */
    private static HttpSession sessionOf(final User user) {
        final HttpSession session = new MockSession(UUIDGenerator.generateUuid());
        session.setAttribute(WebKeys.USER_ID, user.getUserId());
        return session;
    }

    /**
     * A new request on the given session, as each browser request after a login is. Each request
     * is a new object because the user resolved for a request is kept on that request.
     */
    private static HttpServletRequest requestOnSession(final HttpSession session) {
        final MockSessionRequest sessionRequest = new MockSessionRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/testing-web-resorce").request());
        sessionRequest.setSession(session);
        return new MockAttributeRequest(sessionRequest).request();
    }

}
