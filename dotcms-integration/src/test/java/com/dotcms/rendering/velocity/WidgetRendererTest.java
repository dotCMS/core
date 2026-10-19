package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link WidgetRenderer} always evaluates the widget code; it evaluates the produced text a
 * second time (which runs Velocity written inside the widget field values) only when the
 * feature flag is on and the widget was last modified by a Scripting Developer.
 */
public class WidgetRendererTest {

    private static final String FIELD_VELOCITY = "$math.add(20,22)";
    private static final String WIDGET_CODE = "[$math.add(1,2)]$code";

    private static ContentType widgetType;
    private static User plainUser;
    private static User scriptingDeveloper;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        widgetType = TestDataUtils.getWidgetLikeContentType(
                "WidgetRendererTestType" + System.currentTimeMillis(), null, () -> WIDGET_CODE);

        Role scriptingRole = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        if (scriptingRole == null) {
            scriptingRole = new RoleDataGen().key(Role.SCRIPTING_DEVELOPER).name(Role.SCRIPTING_DEVELOPER)
                    .nextPersisted();
        }
        plainUser = new UserDataGen().nextPersisted();
        scriptingDeveloper = new UserDataGen().roles(scriptingRole).nextPersisted();
    }

    private static Contentlet widgetLastModifiedBy(final User user) {
        final Contentlet widget = new ContentletDataGen(widgetType.id())
                .setProperty("widgetTitle", "WidgetRendererTest")
                .setProperty("code", FIELD_VELOCITY)
                .nextPersisted();
        widget.setModUser(user.getUserId());
        return widget;
    }

    private static String render(final Contentlet widget) {
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        final HttpServletResponse response = new MockHttpResponse().response();
        return WidgetRenderer.getInstance().render(request, response, widget).trim();
    }

    private static String renderWithFlag(final Contentlet widget, final boolean flag) {
        final boolean previous = Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, false);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, flag);
            return render(widget);
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, previous);
        }
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag off, widget last modified by a Scripting Developer
     * Expected result: the widget code is evaluated, the field value comes out as text
     */
    @Test
    public void flagOffRendersFieldValueAsText() {
        final String rendered = renderWithFlag(widgetLastModifiedBy(scriptingDeveloper), false);

        assertEquals("[3]" + FIELD_VELOCITY, rendered);
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag on, widget last modified by a user without the Scripting Developer role
     * Expected result: the field value still comes out as text
     */
    @Test
    public void flagOnWithoutScriptingRoleRendersFieldValueAsText() {
        final String rendered = renderWithFlag(widgetLastModifiedBy(plainUser), true);

        assertEquals("[3]" + FIELD_VELOCITY, rendered);
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag on, widget last modified by a Scripting Developer
     * Expected result: the Velocity inside the field value is evaluated
     */
    @Test
    public void flagOnWithScriptingRoleEvaluatesFieldValue() {
        final String rendered = renderWithFlag(widgetLastModifiedBy(scriptingDeveloper), true);

        assertEquals("[3]42", rendered);
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag on, widget with no last modifier recorded
     * Expected result: the field value comes out as text
     */
    @Test
    public void flagOnWithoutLastModifierRendersFieldValueAsText() {
        final Contentlet widget = widgetLastModifiedBy(scriptingDeveloper);
        widget.setModUser(null);

        final String rendered = renderWithFlag(widget, true);

        assertEquals("[3]" + FIELD_VELOCITY, rendered);
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: a contentlet whose type is not a widget
     * Expected result: empty text, as before
     */
    @Test
    public void nonWidgetRendersEmpty() {
        final Contentlet notAWidget = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());

        assertTrue(render(notAWidget).isEmpty());
        assertFalse(widgetType.baseType().name().isEmpty());
    }

}
