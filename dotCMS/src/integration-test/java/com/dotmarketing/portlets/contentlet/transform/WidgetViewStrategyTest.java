package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class WidgetViewStrategyTest extends BaseWorkflowIntegrationTest {

    static Host site;
    static ContentType contentType;
    static Contentlet widgetWithJson;
    static Contentlet widgetWithoutJson;
    static Contentlet widgetSessionKiller;

    final static String VELOCITY_MESSAGE = "Hello World";

    final static String DONT_RENDER_THIS_CODE = "Message from velocity $date";
    final static String YES_RENDER_THIS_CODE = "My value: #set($message=\""+ VELOCITY_MESSAGE + "\") $dotJSON.put(\"message\", $message)";

    final static String DESTROY_USER_SESSION = "kill session ${session.invalidate()}";



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

        APILocator.getContentTypeFieldAPI().save(FieldBuilder.builder(codeField).values("$code").sortOrder(100).build(), APILocator.systemUser());




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

    @Test
    public void test_widget_returns_NO_json() throws DotDataException, DotSecurityException {


        Map<String, Object> map = new DotTransformerBuilder().defaultOptions().content(widgetWithoutJson).build().toMaps().get(0);
        Map<String,Object> widgetCode = (Map<String, Object>)map.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
        assertNotNull(widgetCode);
        assertTrue(widgetCode.isEmpty());

    }

    @Test
    public void test_widget_returns_WITH_json() throws DotDataException, DotSecurityException {


        Map<String, Object> map = new DotTransformerBuilder().defaultOptions().content(widgetWithJson).build().toMaps().get(0);
        Map<String,Object> widgetCode = (Map<String, Object>)map.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
        assertNotNull(widgetCode);
        assertTrue(!widgetCode.isEmpty());


        assertEquals(VELOCITY_MESSAGE,widgetCode.get("message"));


    }

    @Test
    public void test_widget_does_not_modify_real_request_session() throws DotDataException, DotSecurityException {

        HttpServletRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new FakeHttpRequest("localhost", "/").request()
                )
        );

        request.getSession().setAttribute("testing", Boolean.TRUE);
        String sessionId = request.getSession().getId();

        try {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);


            Map<String, Object> map = new DotTransformerBuilder().defaultOptions().content(widgetSessionKiller).build().toMaps().get(0);
            Map<String, Object> widgetCode = (Map<String, Object>) map.get(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR);
            assertNotNull(widgetCode);
            assertTrue(widgetCode.isEmpty());

            assertEquals(sessionId, request.getSession().getId());
            assertTrue((Boolean) request.getSession().getAttribute("testing"));


        }finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        }

    }





}
