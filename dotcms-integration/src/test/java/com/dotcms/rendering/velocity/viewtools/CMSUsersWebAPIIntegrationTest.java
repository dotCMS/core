package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A template using {@code $cmsuser} gets the user's profile but not its stored password.
 */
public class CMSUsersWebAPIIntegrationTest {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = new UserDataGen().password("a-very-long-password-" + System.currentTimeMillis()).nextPersisted();
    }

    private static String evaluate(final String template) {
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        final HttpServletResponse response = new MockHttpResponse().response();
        final Context context = VelocityUtil.getWebContext(VelocityUtil.getBasicContext(), request, response);
        final StringWriter writer = new StringWriter();
        VelocityUtil.getEngine().evaluate(context, writer, "CMSUsersWebAPIIntegrationTest", template);
        return writer.toString().trim();
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getUserByEmail(String)} from a template
     * Given scenario: a template prints the password of a user found by email
     * Expected result: nothing is printed, and the stored password never appears in the output
     */
    @Test
    public void templateCannotPrintStoredPassword() throws Exception {
        final String storedPassword = APILocator.getUserAPI()
                .loadUserById(user.getUserId(), APILocator.systemUser(), false).getPassword();
        assertNotNull(storedPassword);

        final String rendered = evaluate(
                "#set($v = $cmsuser.getUserByEmail(\"" + user.getEmailAddress() + "\"))"
                        + "[$!v.getPassword()][$!v.password][$!v.getRememberMeToken()]");

        assertEquals("[][][]", rendered);
        assertFalse(rendered.contains(storedPassword));
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getUserByUserId(String)} from a template
     * Given scenario: a template prints the profile of a user found by id
     * Expected result: the profile data is still available
     */
    @Test
    public void templateStillSeesProfileData() {
        final String rendered = evaluate(
                "#set($v = $cmsuser.getUserByUserId(\"" + user.getUserId() + "\"))"
                        + "$v.emailAddress|$v.firstName|$v.userId");

        assertEquals(user.getEmailAddress() + "|" + user.getFirstName() + "|" + user.getUserId(), rendered);
    }

}
