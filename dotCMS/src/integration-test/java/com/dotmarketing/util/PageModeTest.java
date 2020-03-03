package com.dotmarketing.util;

import static org.junit.Assert.*;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.BaseRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
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

        assert (PageMode.get(request) == PageMode.LIVE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);

        assert (PageMode.get(request) == PageMode.EDIT_MODE);

    }

    /**
     * getting and setting a PAGE MODE does not create a session
     */
    @Test
    public void test_page_mode_does_not_create_session() {

        HttpServletRequest request = backEndRequest();

        assert (PageMode.get(request) == PageMode.LIVE);

        PageMode.setPageMode(request, PageMode.EDIT_MODE);


        assert (PageMode.get(request) == PageMode.EDIT_MODE);

        assert (request.getSession(false) == null);


    }



}
