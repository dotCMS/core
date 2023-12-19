package com.dotcms.rendering.js;

import com.dotcms.IntegrationTestBase;
import com.dotcms.mock.request.BaseRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletResponse;
import org.graalvm.polyglot.PolyglotException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

/**
 * Test for the JSEngine
 * @author jsanca
 */
public class JsEngineTest extends IntegrationTestBase {

    private JsEngine jsEngine = null;

    @Before
    public void setUp() throws Exception {

        this.jsEngine = new JsEngine();
    }

    @After
    public void tearDown() throws Exception {

        this.jsEngine = null;
    }

    @Test
    public void testNumberEval() throws Exception {

        final String script = "var a = 1; var b = 2; var c = a + b; c;";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals(3, result);
    }

    @Test
    public void testStringEval() throws Exception {

        final String script = "var a = 'this is'; var b = ' a string'; var c = a + b; c;";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals( "this is a string", result);
    }

    @Test
    public void testBooleanEval() throws Exception {

        final String script = "var a = true; var b = false; var c = a && b; c;";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals( false, result);
    }

    @Test(expected = PolyglotException.class)
    public void testEvalWithException() throws Exception {

        final String script = "var a = 1; var b = 2; var c = a + b; c; throw new Error('test');";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals(3, result);
    }


}
