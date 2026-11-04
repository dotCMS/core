package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link WidgetRenderer} always evaluates the widget code; it evaluates the produced text a
 * second time (which runs Velocity written inside the widget field values) only when the
 * feature flag is on (its default) and the widget was last modified by a Scripting Developer.
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

    @AfterClass
    public static void cleanup() throws Exception {
        // Deleting the type also deletes the widgets persisted by the tests.
        ContentTypeDataGen.remove(widgetType);
        // Deleting a user needs a replacement user that owns a user role, which the system user lacks.
        final User admin = TestUserUtils.getAdminUser();
        APILocator.getUserAPI().delete(plainUser, admin, APILocator.systemUser(), false);
        APILocator.getUserAPI().delete(scriptingDeveloper, admin, APILocator.systemUser(), false);
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
        final boolean previous = Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, true);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, flag);
            return render(widget);
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, previous);
        }
    }

    /**
     * Method to test: {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: the flag is left at its default, widget last modified by a Scripting Developer
     * Expected result: the Velocity inside the field value is evaluated, the switch is on by default
     */
    @Test
    public void defaultFlagEvaluatesFieldValueForScriptingDeveloper() {
        assertTrue(Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, true));

        assertEquals("[3]42", render(widgetLastModifiedBy(scriptingDeveloper)));
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
     * Given scenario: flag on, widget persisted as is (the data generator checks it in as the system user)
     * Expected result: the system user does not qualify, the field value comes out as text
     */
    @Test
    public void flagOnWithSystemLastModifierRendersFieldValueAsText() {
        final Contentlet widget = new ContentletDataGen(widgetType.id())
                .setProperty("widgetTitle", "WidgetRendererTest")
                .setProperty("code", FIELD_VELOCITY)
                .nextPersisted();
        assertEquals(UserAPI.SYSTEM_USER_ID, widget.getModUser());

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
    }

    /**
     * Method to test: {@link WidgetRenderer#renderWithDetails(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: no contentlet at all, which is what the widget endpoint hands over when the
     * requested id has no version the caller may read
     * Expected result: empty text and no field-scripting note, instead of a failure
     */
    @Test
    public void nullContentletRendersEmpty() {
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        final HttpServletResponse response = new MockHttpResponse().response();

        final WidgetRenderer.Rendering rendering = WidgetRenderer.getInstance()
                .renderWithDetails(request, response, null);

        assertTrue(rendering.text().isEmpty());
        assertFalse(rendering.fieldScriptingSkipped());
    }

    /**
     * Method to test: {@link WidgetRenderer#renderWithDetails(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag off, a widget whose field value holds Velocity
     * Expected result: the rendering says the field scripting was skipped
     */
    @Test
    public void flagOffReportsSkippedFieldScripting() {
        final WidgetRenderer.Rendering rendering = renderWithDetailsAndFlag(
                widgetLastModifiedBy(scriptingDeveloper), false);

        assertEquals("[3]" + FIELD_VELOCITY, rendering.text().trim());
        assertTrue(rendering.fieldScriptingSkipped());
    }

    /**
     * Method to test: {@link WidgetRenderer#renderWithDetails(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag off, a widget whose field value is plain text
     * Expected result: nothing to report, the output has no Velocity left in it
     */
    @Test
    public void flagOffWithPlainFieldReportsNothing() {
        final Contentlet widget = new ContentletDataGen(widgetType.id())
                .setProperty("widgetTitle", "WidgetRendererTest")
                .setProperty("code", "plain text")
                .nextPersisted();

        final WidgetRenderer.Rendering rendering = renderWithDetailsAndFlag(widget, false);

        assertEquals("[3]plain text", rendering.text().trim());
        assertFalse(rendering.fieldScriptingSkipped());
    }

    /**
     * Method to test: {@link WidgetRenderer#renderWithDetails(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag off, the widget code itself prints a colour and a price, the field values are plain
     * Expected result: nothing to report, {@code $} and {@code #} in the output do not count
     */
    @Test
    public void flagOffWithVelocityLookalikesInWidgetCodeReportsNothing() {
        final ContentType colourType = TestDataUtils.getWidgetLikeContentType(
                "WidgetRendererColourType" + System.currentTimeMillis(), null, () -> "#fff costs $20 [$code]");
        final Contentlet widget = new ContentletDataGen(colourType.id())
                .setProperty("widgetTitle", "WidgetRendererTest")
                .setProperty("code", "plain text")
                .nextPersisted();
        try {
            final WidgetRenderer.Rendering rendering = renderWithDetailsAndFlag(widget, false);

            assertEquals("#fff costs $20 [plain text]", rendering.text().trim());
            assertFalse(rendering.fieldScriptingSkipped());
        } finally {
            ContentTypeDataGen.remove(colourType);
        }
    }

    /**
     * Method to test: {@link WidgetRenderer#renderWithDetails(HttpServletRequest, HttpServletResponse, Contentlet)}
     * Given scenario: flag on, widget last modified by a Scripting Developer
     * Expected result: the field value is evaluated and there is nothing to report
     */
    @Test
    public void flagOnWithScriptingRoleReportsNothing() {
        final WidgetRenderer.Rendering rendering = renderWithDetailsAndFlag(
                widgetLastModifiedBy(scriptingDeveloper), true);

        assertEquals("[3]42", rendering.text().trim());
        assertFalse(rendering.fieldScriptingSkipped());
    }

    private static WidgetRenderer.Rendering renderWithDetailsAndFlag(final Contentlet widget, final boolean flag) {
        final boolean previous = Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, true);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, flag);
            final HttpServletRequest request = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                    .request();
            return WidgetRenderer.getInstance().renderWithDetails(request, new MockHttpResponse().response(), widget);
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, previous);
        }
    }

}
