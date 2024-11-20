package com.dotmarketing.portlets.contentlet.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class WidgetViewStrategyTest extends IntegrationTestBase {

    static final String VELOCITY_MESSAGE = "Hello World";
    static final String DONT_RENDER_THIS_CODE = "Message from velocity $date";
    static final String YES_RENDER_THIS_CODE = "My value: #set($message=\"" + VELOCITY_MESSAGE
            + "\") $dotJSON.put(\"message\", $message)";
    static final String DESTROY_USER_SESSION = "kill session ${session.invalidate()}";
    static Host site;
    static Contentlet widgetWithJson;
    static Contentlet widgetWithoutJson;
    static Contentlet widgetSessionKiller;

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(
                new FieldDataGen()
                        .name("Code")
                        .velocityVarName("code")
                        .required(true)
                        .sortOrder(10)
                        .next()
        );

        ContentType simpleWidgetContentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.WIDGET)
                .name("SimpleWidget" + System.currentTimeMillis())
                .velocityVarName("SimpleWidget" + System.currentTimeMillis())
                .fields(fields)
                .nextPersisted();

        Field codeField = simpleWidgetContentType.fieldMap().get("widgetCode");

        APILocator.getContentTypeFieldAPI()
                .save(FieldBuilder.builder(codeField).values("$code").sortOrder(100).build(),
                        APILocator.systemUser());

        site = new SiteDataGen().nextPersisted();

        Contentlet con = new Contentlet();
        con.setContentType(simpleWidgetContentType);
        con.setStringProperty("code", DONT_RENDER_THIS_CODE);
        con.setStringProperty("widgetTitle", "NoRender");
        widgetWithoutJson = ContentletDataGen.checkin(con);
        ContentletDataGen.publish(widgetWithoutJson);

        con = new Contentlet();
        con.setContentType(simpleWidgetContentType);
        con.setStringProperty("code", YES_RENDER_THIS_CODE);
        con.setStringProperty("widgetTitle", "Yes Render");
        widgetWithJson = ContentletDataGen.checkin(con);

        ContentletDataGen.publish(widgetWithJson);

        con = new Contentlet();
        con.setContentType(simpleWidgetContentType);
        con.setStringProperty("code", DESTROY_USER_SESSION);
        con.setStringProperty("widgetTitle", "Die Session");
        widgetSessionKiller = ContentletDataGen.checkin(con);
        ContentletDataGen.publish(widgetSessionKiller);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);

    }

    /**
     * Validates that a widget without JSON code does not produce a JSON response.
     *
     * <p><b>Given Scenario:</b> A widget is created with content that does not include
     * JSON-producing code.</p>
     * <p><b>Expected Result:</b> The widget's JSON field is present but empty.</p>
     *
     * @throws DotDataException     if there is a data-related error.
     * @throws DotSecurityException if there is a security-related error.
     */
    @Test
    public void test_widget_returns_NO_json() throws DotDataException, DotSecurityException {
        Map<String, Object> map = new DotTransformerBuilder().defaultOptions()
                .content(widgetWithoutJson).build().toMaps().get(0);
        Map<String, Object> widgetCode = (Map<String, Object>) map.get(
                WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
        assertNotNull(widgetCode);
        assertTrue(widgetCode.isEmpty());
    }

    /**
     * Validates that a widget with JSON code produces the correct JSON response.
     *
     * <p><b>Given Scenario:</b> A widget is created with content that includes JSON-producing
     * code.</p>
     * <p><b>Expected Result:</b> The widget's JSON field contains the expected key-value pair.</p>
     *
     * @throws DotDataException     if there is a data-related error.
     * @throws DotSecurityException if there is a security-related error.
     */
    @Test
    public void test_widget_returns_WITH_json() throws DotDataException, DotSecurityException {

        Map<String, Object> map = new DotTransformerBuilder().defaultOptions()
                .content(widgetWithJson).build().toMaps().get(0);
        Map<String, Object> widgetCode = (Map<String, Object>) map.get(
                WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
        assertNotNull(widgetCode);
        assertFalse(widgetCode.isEmpty());
        assertEquals(VELOCITY_MESSAGE, widgetCode.get("message"));

    }

    /**
     * Validates that a widget with session-modifying code does not affect the actual HTTP session.
     *
     * <p><b>Given Scenario:</b> A widget is created with content intended to invalidate the
     * session.</p>
     * <p><b>Expected Result:</b> The session remains unchanged, preserving its attributes and
     * ID.</p>
     */
    @Test
    public void test_widget_does_not_modify_real_request_session() {

        //This should pass as none mocked request
        //Cause of we pass a mock the impersonator will not mock a mock
        final HttpServletRequest request = new HttpServletRequestWrapper(new MockSessionRequest(
                new MockHeaderRequest(new FakeHttpRequest("localhost", "/").request())).request());

        request.getSession().setAttribute("testing", Boolean.TRUE);
        String sessionId = request.getSession().getId();

        try {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            Map<String, Object> map = new DotTransformerBuilder().defaultOptions()
                    .content(widgetSessionKiller).build().toMaps().get(0);
            Map<String, Object> widgetCode = (Map<String, Object>) map.get(
                    WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
            assertNotNull(widgetCode);
            assertTrue(widgetCode.isEmpty());

            assertEquals(sessionId, request.getSession().getId());
            assertTrue((Boolean) request.getSession().getAttribute("testing"));


        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        }

    }

}