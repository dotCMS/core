package com.dotcms.rendering.js;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.mock.request.BaseRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletResponse;
import org.graalvm.polyglot.PolyglotException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
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

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Eval a script that sum two numbers
     * ExpectedResult: The result of the sum
     *
     */
    @Ignore
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

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Concat two strings
     * ExpectedResult: The results of the concat
     *
     */
    @Ignore
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

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Eval an AND between true and false booleans
     * ExpectedResult: False boolean
     *
     */
    @Ignore
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

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Create a Json Object
     * ExpectedResult: The Json Object is created as a Map with the right values
     *
     */
    @Ignore
    @Test
    public void testMapEval() throws Exception {

        final String script = "var a = 'value1'; var b = 2; var c = {value1:a, value2:b}; c;";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertTrue(result instanceof Map);
        final Map map = (Map) result;
        Assert.assertEquals( 2, map.get("value2"));
        Assert.assertEquals( "value1", map.get("value1"));
    }

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Populates values into the dotJSON (which is implicit on the context)
     * ExpectedResult: The dotJSON is being returned with the right values
     *
     */
    @Ignore
    @Test
    public void testDotJSONEval() throws Exception {

        final String script = "var a = 'value1'; var b = 2; dotJSON.put('value1', a); dotJSON.put('value2', b); dotJSON;";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertTrue(result instanceof DotJSON);
        final DotJSON map = (DotJSON) result;
        Assert.assertEquals( 2, map.get("value2"));
        Assert.assertEquals( "value1", map.get("value1"));
    }

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: The script throws an exception
     * ExpectedResult: An exception is expected
     *
     */
    @Ignore
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

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Creates a function and call it with a hardcoded parameters
     * ExpectedResult: The square of 2 should be returned (4)
     *
     */
    @Ignore
    @Test()
    public void testFunctionEval() throws Exception {

        /*
        function square(x) {
                return x * x;
        }

         square(2);
         */
        final String script = "function square(x) {\n" +
                "        return x * x;\n" +
                "}\n" +
                "\n" +
                "square(2);";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams = Map.of();

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals(4, result);
    }

    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Creates a function and call it with a parameter pass by argument to the function
     * ExpectedResult: The square of 4 should be returned (16)
     *
     */
    @Ignore
    @Test()
    public void testFunctionWithArgumentsEval() throws Exception {

        /*
        (function square(context, x) {
                return x * x;
        })
         */
        final String script = "(function square(context, x) {\n" +
                "        return x * x;\n" +
                "})";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams =  new HashMap<>(Map.of("dot:arguments", new Object[]{4}));

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals(16, result);
    }

    // test class
    /**
     * Method to test: {@link JsEngine#eval(HttpServletRequest, HttpServletResponse, Reader, Map)}
     * Given Scenario: Creates a class and call it with a parameter pass by argument to the function
     * ExpectedResult: The square of 4 should be returned (16)
     *
     */
    @Ignore
    @Test()
    public void testClassWithArgumentsEval() throws Exception {

        /*
        class MyMath {
            static square(x) {
                return x * x;
            }
        }

        (function mycall(context, x) {
                return MyMath.square(x);
        })
         */
        final String script = "class MyMath {\n" +
                "            static square(x) {\n" +
                "                return x * x;\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        (function mycall(context, x) {\n" +
                "                return MyMath.square(x);\n" +
                "        })";
        final HttpServletRequest request = new MockAttributeRequest(new BaseRequest().request());
        final HttpServletResponse response = new FakeHttpServletResponse();
        final Reader scriptReader = new StringReader(script);
        final Map<String, Object> contextParams =  new HashMap<>(Map.of("dot:arguments", new Object[]{4}));

        final Object result = this.jsEngine.eval(request, response, scriptReader,  contextParams);

        Assert.assertEquals(16, result);
    }
}
