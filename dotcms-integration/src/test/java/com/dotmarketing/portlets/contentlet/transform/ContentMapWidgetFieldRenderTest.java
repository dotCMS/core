package com.dotmarketing.portlets.contentlet.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The content map the REST endpoints return with {@code render=true} goes through
 * {@code RenderFieldStrategy}, which turns every renderable field value into the text its Velocity
 * produces. For a widget that must be rendered as text, those values have to come back untouched,
 * the same way the widget endpoint returns them.
 */
public class ContentMapWidgetFieldRenderTest {

    private static final String FIELD_VELOCITY = "$math.add(20,22)";
    private static final String FIELD_VELOCITY_RESULT = "42";
    private static final String WIDGET_CODE = "[$math.add(1,2)]$code";

    private static ContentType widgetType;
    private static ContentType plainType;
    private static User plainUser;
    private static User scriptingDeveloper;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        widgetType = TestDataUtils.getWidgetLikeContentType(
                "ContentMapWidgetFieldRenderType" + System.currentTimeMillis(), null, () -> WIDGET_CODE);

        plainType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.CONTENT)
                .name("ContentMapPlainType" + System.currentTimeMillis())
                .fields(List.of(new FieldDataGen().name("Code").velocityVarName("code").next()))
                .nextPersisted();

        Role scriptingRole = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        if (scriptingRole == null) {
            scriptingRole = new RoleDataGen().key(Role.SCRIPTING_DEVELOPER).name(Role.SCRIPTING_DEVELOPER)
                    .nextPersisted();
        }
        plainUser = new UserDataGen().nextPersisted();
        scriptingDeveloper = new UserDataGen().roles(scriptingRole).nextPersisted();
    }

    @Before
    public void bindRequest() {
        // The field values are evaluated against the request's Velocity context, which is what
        // carries the view tools; without it $math and friends are not defined.
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        final HttpServletResponse response = new MockHttpResponse().response();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);
    }

    @After
    public void unbindRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(null);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Deleting the types also deletes the contentlets the tests persisted.
        ContentTypeDataGen.remove(widgetType);
        ContentTypeDataGen.remove(plainType);
        // Deleting a user needs a replacement user that owns a user role, which the system user lacks.
        final User admin = TestUserUtils.getAdminUser();
        APILocator.getUserAPI().delete(plainUser, admin, APILocator.systemUser(), false);
        APILocator.getUserAPI().delete(scriptingDeveloper, admin, APILocator.systemUser(), false);
    }

    private static Contentlet widgetLastModifiedBy(final User user) {
        final Contentlet persisted = new ContentletDataGen(widgetType.id())
                .setProperty("widgetTitle", "ContentMapWidgetFieldRenderTest")
                .setProperty("code", FIELD_VELOCITY)
                .nextPersisted();
        // A copy, so the last modifier of the instance the cache may hold is left alone
        final Contentlet widget = new Contentlet(persisted.getMap());
        widget.setModUser(user.getUserId());
        return widget;
    }

    private static Contentlet widgetCheckedInBySystem() {
        return new ContentletDataGen(widgetType.id())
                .setProperty("widgetTitle", "ContentMapWidgetFieldRenderTest")
                .setProperty("code", FIELD_VELOCITY)
                .nextPersisted();
    }

    private static Map<String, Object> renderedMap(final Contentlet contentlet) {
        return ContentletUtil.getContentPrintableMap(APILocator.systemUser(), contentlet, false, true);
    }

    private static Map<String, Object> renderedMapWithFlag(final Contentlet contentlet, final boolean flag) {
        final boolean previous = Config.getBooleanProperty(
                FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, true);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, flag);
            return renderedMap(contentlet);
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER, previous);
        }
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: a widget last modified by a user without the Scripting Developer role
     * Expected result: the field value comes back as the text the author typed
     */
    @Test
    public void widgetWithoutScriptingRoleKeepsFieldValueAsText() {
        final Map<String, Object> map = renderedMap(widgetLastModifiedBy(plainUser));

        assertEquals(FIELD_VELOCITY, map.get("code"));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: a widget last modified by a Scripting Developer
     * Expected result: the field value is evaluated, as it always has been
     */
    @Test
    public void widgetWithScriptingRoleRendersFieldValue() {
        final Map<String, Object> map = renderedMap(widgetLastModifiedBy(scriptingDeveloper));

        assertEquals(FIELD_VELOCITY_RESULT, map.get("code"));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: the feature flag is off and the widget was last modified by a Scripting Developer
     * Expected result: the flag wins, the field value comes back as text
     */
    @Test
    public void flagOffKeepsFieldValueAsTextForScriptingDeveloper() {
        final Map<String, Object> map = renderedMapWithFlag(widgetLastModifiedBy(scriptingDeveloper), false);

        assertEquals(FIELD_VELOCITY, map.get("code"));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: the widget's field value holds Velocity and its last modifier lacks the role
     * Expected result: no {@code widgetCodeJSON} either, because producing it evaluates the field values
     */
    @Test
    public void widgetWithoutScriptingRoleProducesEmptyWidgetCodeJson() {
        final Map<String, Object> withoutRole = renderedMap(widgetLastModifiedBy(plainUser));
        // The key stays so the response shape does not change, but nothing was collected
        assertTrue(withoutRole.containsKey(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR));
        assertEquals(Collections.emptyMap(), withoutRole.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR));

        final Map<String, Object> withRole = renderedMap(widgetLastModifiedBy(scriptingDeveloper));
        assertTrue(withRole.containsKey(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: a contentlet whose type is not a widget, holding Velocity in a text field
     * Expected result: it is rendered as before; the rule only applies to widgets
     */
    @Test
    public void nonWidgetContentKeepsRenderingItsFields() {
        final Contentlet content = new ContentletDataGen(plainType.id())
                .setProperty("code", FIELD_VELOCITY)
                .nextPersisted();
        content.setModUser(plainUser.getUserId());

        final Map<String, Object> map = renderedMap(content);

        assertNotNull(map.get("code"));
        assertEquals(FIELD_VELOCITY_RESULT, map.get("code"));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: the widget type's own fields, which only a content-type editor can write
     * Expected result: the widget code field is left alone too, so nothing in the map was evaluated
     */
    @Test
    public void widgetWithoutScriptingRoleKeepsTypeLevelFieldsAsText() {
        final Map<String, Object> map = renderedMap(widgetLastModifiedBy(plainUser));
        final Field widgetCode = widgetType.fieldMap().get(WidgetContentType.WIDGET_CODE_FIELD_VAR);

        assertNotNull(widgetCode);
        // WIDGET_CODE starts with an expression, so an evaluated value would differ from the stored one
        assertEquals(WIDGET_CODE, map.get(WidgetContentType.WIDGET_CODE_FIELD_VAR));
        // Evaluated, the expression collapses and $code stays literal: the context holds the
        // contentlet, not its field variables
        assertEquals("[3]$code",
                renderedMap(widgetLastModifiedBy(scriptingDeveloper))
                        .get(WidgetContentType.WIDGET_CODE_FIELD_VAR));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: a widget whose recorded last modifier is the system user, as import, push
     * publishing and startup content leave it
     * Expected result: the field value comes back as text; the system user does not qualify
     */
    @Test
    public void widgetLastModifiedBySystemKeepsFieldValueAsText() {
        final Contentlet widget = widgetCheckedInBySystem();
        assertEquals(UserAPI.SYSTEM_USER_ID, widget.getModUser());

        final Map<String, Object> map = renderedMap(widget);

        assertEquals(FIELD_VELOCITY, map.get("code"));
        assertEquals(Collections.emptyMap(), map.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR));
    }

    /**
     * Method to test: {@link ContentletUtil#getContentPrintableMap(User, Contentlet, boolean, boolean)}
     * Given scenario: the feature flag is off for a widget last modified by a Scripting Developer
     * Expected result: no $dotJSON is collected either, the flag covers everything derived from
     * the field values
     */
    @Test
    public void flagOffProducesEmptyWidgetCodeJson() {
        final Map<String, Object> map = renderedMapWithFlag(widgetLastModifiedBy(scriptingDeveloper), false);

        assertEquals(Collections.emptyMap(), map.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, String, Contentlet, String)}
     * Given scenario: the helper the GraphQL field fetchers call directly, for a widget whose last
     * modifier lacks the Scripting Developer role
     * Expected result: the rule travels with the helper, so those callers get the value as text too
     */
    @Test
    public void renderFieldValueHelperAppliesTheRule() {
        final Contentlet gated = widgetLastModifiedBy(plainUser);
        final Contentlet allowed = widgetLastModifiedBy(scriptingDeveloper);

        assertEquals(FIELD_VELOCITY, RenderFieldStrategy.renderFieldValue(
                HttpServletRequestThreadLocal.INSTANCE.getRequest(),
                HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                FIELD_VELOCITY, gated, "code"));
        assertEquals(FIELD_VELOCITY_RESULT, RenderFieldStrategy.renderFieldValue(
                HttpServletRequestThreadLocal.INSTANCE.getRequest(),
                HttpServletResponseThreadLocal.INSTANCE.getResponse(),
                FIELD_VELOCITY, allowed, "code"));
    }
}
