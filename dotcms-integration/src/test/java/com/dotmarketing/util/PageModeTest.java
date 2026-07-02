package com.dotmarketing.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rendering.velocity.servlet.VelocityServlet;
import com.google.common.collect.ImmutableMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class PageModeTest {

    private static User frontEndUser, backEndUser, frontBackEndUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();


        frontEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();

        backEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        frontBackEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadFrontEndUserRole(), APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
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

    private HttpServletRequest frontBackEndRequest() {
        final HttpServletRequest request = anonymousRequest();
        assertTrue("backEndUser has backend role", frontBackEndUser.isBackendUser());
        request.setAttribute(WebKeys.USER, frontBackEndUser);
        return request;
    }

    /**
     * Method to test: Test for {@link VelocityServlet#processPageMode(User, HttpServletRequest)}
     * Given Scenario: Be user logged in but the referer is empty
     * ExpectedResult: Page mode should be PREVIEW
     *
     */
    @Test
    public void test_be_page_mode_should_be_PREVIEW() {

        final HttpSession session        = new MockSession(UUIDGenerator.uuid());
        final MockSessionRequest requestSession = new MockSessionRequest(backEndRequest());
        final MockAttributeRequest request = new MockAttributeRequest(requestSession);
        session.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION, PageMode.NAVIGATE_EDIT_MODE);
        request.setAttribute(WebKeys.USER, backEndUser);
        requestSession.setSession(session);
        LoginMode.set(request, LoginMode.BE);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final PageMode pageMode = VelocityServlet.processPageMode(backEndUser, request);
        Assert.assertEquals(LoginMode.BE, LoginMode.get(request));
        Assert.assertEquals(PageMode.PREVIEW_MODE, pageMode);
    }

    /**
     * Method to test: Test for {@link VelocityServlet#processPageMode(User, HttpServletRequest)}
     * Given Scenario: Be user logged in but the referer is from dotAdmin
     * ExpectedResult: Page mode should be NAVIGATE_EDIT_MODE
     *
     */
    @Test
    public void backendUserDotAdminReferer() {

        final HttpSession session        = new MockSession(UUIDGenerator.uuid());

        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        httpServletRequest.setAttribute(WebKeys.USER, backEndUser);
        when(httpServletRequest.getHeader("referer")).thenReturn("/dotAdmin/test");

        final MockSessionRequest requestSession = new MockSessionRequest(httpServletRequest);
        final MockAttributeRequest request = new MockAttributeRequest(requestSession);
        session.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION, PageMode.NAVIGATE_EDIT_MODE);
        request.setAttribute(WebKeys.USER, backEndUser);
        requestSession.setSession(session);
        LoginMode.set(request, LoginMode.BE);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final PageMode pageMode = VelocityServlet.processPageMode(backEndUser, request);
        Assert.assertEquals(LoginMode.BE, LoginMode.get(request));
        Assert.assertEquals(PageMode.NAVIGATE_EDIT_MODE, pageMode);
    }

    /**
     * Method to test: Test for {@link VelocityServlet#processPageMode(User, HttpServletRequest)}
     * Given Scenario: Be user (fe_be) logged in as BE should be PREVIEW
     * ExpectedResult: Page mode should be LIVE
     *
     */
    @Test
    public void test_befe_logged_in_be_page_mode_should_be_PREVIEW() {

        final HttpSession session        = new MockSession(UUIDGenerator.uuid());
        final MockSessionRequest requestSession = new MockSessionRequest(frontBackEndRequest());
        final MockAttributeRequest request = new MockAttributeRequest(requestSession);
        session.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION, PageMode.NAVIGATE_EDIT_MODE);
        request.setAttribute(WebKeys.USER, frontBackEndUser);
        requestSession.setSession(session);
        LoginMode.set(request, LoginMode.BE);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final PageMode pageMode = VelocityServlet.processPageMode(frontBackEndUser, request);
        Assert.assertEquals(LoginMode.BE, LoginMode.get(request));
        Assert.assertEquals(PageMode.PREVIEW_MODE, pageMode);
    }

    /**
     * Method to test: Test for {@link VelocityServlet#processPageMode(User, HttpServletRequest)}
     * Given Scenario: Fe user logged in should be live
     * ExpectedResult: Page mode should be live
     *
     */
    @Test
    public void test_fe_page_mode_should_be_LIVE() {

        final HttpSession session        = new MockSession(UUIDGenerator.uuid());
        final MockSessionRequest requestSession = new MockSessionRequest(frontEndRequest());
        final MockAttributeRequest request = new MockAttributeRequest(requestSession);
        session.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION, PageMode.NAVIGATE_EDIT_MODE);
        request.setAttribute(WebKeys.USER, frontEndUser);
        requestSession.setSession(session);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        LoginMode.set(request, LoginMode.FE);
        final PageMode pageMode = VelocityServlet.processPageMode(frontEndUser, frontEndRequest());
        Assert.assertEquals(LoginMode.FE, LoginMode.get(request));
        Assert.assertEquals(PageMode.LIVE, pageMode);
    }

    /**
     * Method to test: Test for {@link VelocityServlet#processPageMode(User, HttpServletRequest)}
     * Given Scenario: Fe user (fe_be) logged in fe should be LIVE
     * ExpectedResult: Page mode should be live
     *
     */
    @Test
    public void test_befe_logged_in_fe_page_mode_should_be_LIVE() {

        final HttpSession session        = new MockSession(UUIDGenerator.uuid());
        final MockSessionRequest requestSession = new MockSessionRequest(frontBackEndRequest());
        final MockAttributeRequest request = new MockAttributeRequest(requestSession);
        session.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION, PageMode.NAVIGATE_EDIT_MODE);
        request.setAttribute(WebKeys.USER, frontBackEndUser);
        requestSession.setSession(session);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        LoginMode.set(request, LoginMode.FE);
        final PageMode pageMode = VelocityServlet.processPageMode(frontBackEndUser, frontEndRequest());
        Assert.assertEquals(LoginMode.FE, LoginMode.get(request));
        Assert.assertEquals(PageMode.LIVE, pageMode);
    }

    /**
     * Front end users can never have any page mode other than LIVE, even when you try to set it
     * explictily
     */

    @Test
    public void test_front_end_user_is_only_LIVE() {

        HttpServletRequest request = frontEndRequest();

        assert (PageMode.get(request) == PageMode.LIVE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);

        assert (PageMode.get(request) == PageMode.LIVE);

    }



    /**
     * Back end users can have their page mode set to EDIT MODE
     */
    @Test
    public void test_back_end_user_can_be_set_to_EDIT() {

        HttpServletRequest request = backEndRequest();

        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);

        assert (PageMode.get(request) == PageMode.EDIT_MODE);

    }

    /**
     * getting and setting a PAGE MODE does not create a session
     */
    @Test
    public void test_page_mode_does_not_create_session() {

        HttpServletRequest request = backEndRequest();

        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);


        assert (PageMode.get(request) == PageMode.EDIT_MODE);

        assert (request.getSession(false) == null);


    }

    /**
     * Back end users can have their page mode set to EDIT MODE
     */
    @Test
    public void test_back_end_user_can_be_set_to_LIVE() {

        HttpServletRequest request = backEndRequest();

        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);

        PageMode.setPageMode(request, PageMode.LIVE);

        assert (PageMode.get(request) == PageMode.LIVE);

    }


    /**
     * Testing order of reading PAGE_MODE for a backend user, It should be like this:
     * 1. request parameter
     * 2. request attribute
     * 3. session
     * 4. default = PREVIEW_MODE
     * we should respect the request parameter,
     * then the request attribute, then the session, and finally PREVIEW_MODE if we don't have anything
     */
    @Test
    public void test_order_of_reading_pagemode() {

        // PREVIEW_MODE if there is no mode and back end user
        HttpServletRequest request = new MockSessionRequest(backEndRequest()).request();
        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);

        // session is last fallback
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_SESSION,
                PageMode.ADMIN_MODE);
        assert (PageMode.get(request) == PageMode.ADMIN_MODE);

        // request.attribute overrides session
        request.setAttribute(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER, PageMode.EDIT_MODE);
        assert (PageMode.get(request) == PageMode.EDIT_MODE);

        // passed in parameter overrides everything
        HttpServletRequest parameterRequest =
                new MockParameterRequest(request, ImmutableMap.of("mode", "LIVE")).request();
        assert (PageMode.get(parameterRequest) == PageMode.LIVE);


    }

    /**
     * Method to test: {@link PageMode#getWithReferer(HttpServletRequest)}
     * Given Scenario: A backend user requests a sub-resource (no {@code ?mode=} of its own) whose
     * {@code Referer} page was viewed in {@code LIVE} mode.
     * Expected Result: The mode declared on the Referer ({@code LIVE}) is honored instead of the
     * backend user's default {@code PREVIEW_MODE}.
     */
    @Test
    public void getWithReferer_backend_user_honors_referer_mode() {
        final HttpServletRequest request = new MockHeaderRequest(backEndRequest(),
                "Referer", "http://localhost/my-page?mode=LIVE").request();
        Assert.assertEquals(PageMode.LIVE, PageMode.getWithReferer(request));
    }

    /**
     * Method to test: {@link PageMode#getWithReferer(HttpServletRequest)}
     * Given Scenario: An anonymous request carries a {@code Referer} that declares {@code EDIT_MODE}.
     * Expected Result: The referer is ignored and the request stays on the forced-LIVE path; a spoofed
     * Referer cannot escalate an anonymous request out of LIVE.
     */
    @Test
    public void getWithReferer_anonymous_ignores_referer() {
        final HttpServletRequest request = new MockHeaderRequest(anonymousRequest(),
                "Referer", "http://localhost/my-page?mode=EDIT_MODE").request();
        Assert.assertEquals(PageMode.LIVE, PageMode.getWithReferer(request));
    }

    /**
     * Method to test: {@link PageMode#getWithReferer(HttpServletRequest)}
     * Given Scenario: The request itself declares an explicit {@code ?mode=} while its {@code Referer}
     * declares a different mode.
     * Expected Result: The request's own {@code ?mode=} wins; the Referer is never consulted when the
     * request already declares a mode.
     */
    @Test
    public void getWithReferer_request_mode_wins_over_referer() {
        final HttpServletRequest withReferer = new MockHeaderRequest(backEndRequest(),
                "Referer", "http://localhost/my-page?mode=LIVE").request();
        final HttpServletRequest request = new MockParameterRequest(withReferer,
                ImmutableMap.of("mode", "EDIT_MODE")).request();
        Assert.assertEquals(PageMode.EDIT_MODE, PageMode.getWithReferer(request));
    }

    /**
     * Method to test: {@link PageMode#getWithReferer(HttpServletRequest)}
     * Given Scenario: A backend user requests a sub-resource with no {@code ?mode=} and a {@code Referer}
     * that declares no {@code mode}.
     * Expected Result: Falls back to the backend user's default {@code PREVIEW_MODE}.
     */
    @Test
    public void getWithReferer_backend_user_no_referer_mode_falls_back() {
        final HttpServletRequest request = new MockHeaderRequest(backEndRequest(),
                "Referer", "http://localhost/my-page").request();
        Assert.assertEquals(PageMode.PREVIEW_MODE, PageMode.getWithReferer(request));
    }

    /**
     * Method to test: {@link PageMode#getWithReferer(HttpServletRequest)}
     * Given Scenario: A front-end-only user carries a {@code Referer} declaring {@code EDIT_MODE}.
     * Expected Result: The referer is ignored (the backend-user gate is not satisfied) and the user
     * stays on LIVE.
     */
    @Test
    public void getWithReferer_frontend_user_ignores_referer() {
        final HttpServletRequest request = new MockHeaderRequest(frontEndRequest(),
                "Referer", "http://localhost/my-page?mode=EDIT_MODE").request();
        Assert.assertEquals(PageMode.LIVE, PageMode.getWithReferer(request));
    }

    /**
     * Given Scenario: We're simply testing that setting a page mode works as expected despite the fact that it is not set in the session
     * Expected Result: The page mode should be set to the mode we set it to, and we should be able to get it back consistently
     */
    @Test
    public void Simple_Set_Page_Mode() {
        HttpServletRequest request = backEndRequest();
        PageMode.setPageMode(request, PageMode.EDIT_MODE, false);
        assert (PageMode.get(request) == PageMode.EDIT_MODE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);
        assert (PageMode.get(request) == PageMode.EDIT_MODE);

        PageMode.setPageMode(request, PageMode.LIVE, false);
        assert (PageMode.get(request) == PageMode.LIVE);

        PageMode.setPageMode(request, PageMode.LIVE);
        assert (PageMode.get(request) == PageMode.LIVE);

        PageMode.setPageMode(request, PageMode.PREVIEW_MODE, false);
        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);

        PageMode.setPageMode(request, PageMode.PREVIEW_MODE);
        assert (PageMode.get(request) == PageMode.PREVIEW_MODE);
    }


}
