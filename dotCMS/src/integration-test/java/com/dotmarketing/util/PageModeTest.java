package com.dotmarketing.util;

import static org.junit.Assert.*;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.BaseRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class PageModeTest {

    private static User systemUser;
    private static User testUser;
    private static UserAPI userAPI;
    private static User frontEndUser, backEndUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();


        frontEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();

        backEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();


        userAPI = APILocator.getUserAPI();

    }

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
    
    
    
    


}
