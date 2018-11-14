package com.dotcms.javascript.app;

import com.dotcms.UnitTestBase;
import com.dotcms.api.vtl.model.DotJSON;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavascriptEngineTest extends UnitTestBase {


    @Test
    public void evaluate_simple_hello_return_double(){

        final StringBuilder builder =
                new StringBuilder("print ('hello dotcms');")
                .append          ("dotJSON.put('pi', 3.14);");

        final JavascriptEngine engine = new JavascriptEngine();
        final Map<String, Object> context = new HashMap<>();
        final DotJSON dotJSON = new DotJSON();

        context.put("dotJSON", dotJSON);
        engine.evaluate(builder, context);

        assertTrue(dotJSON.size() > 0);
        assertTrue(dotJSON.getMap().containsKey("pi"));
        assertEquals(Double.valueOf(3.14d), dotJSON.get("pi"));
    }

    @Test
    public void return_json(){

        final StringBuilder builder =
                new StringBuilder("function json() { return {\"firstName\":\"Jonathan\", \"lastName\":\"Sanchez\"} }");

        final JavascriptEngine engine = new JavascriptEngine();

        final Optional<Object> json = engine.invokeFunction(builder, "json");

        assertTrue(json.isPresent());
        final ScriptObjectMirror mirror = (ScriptObjectMirror)json.get();

        assertEquals("Jonathan", mirror.get("firstName"));
        assertEquals("Sanchez", mirror.get("lastName"));
        System.out.println(mirror.entrySet());
        assertEquals(new LinkedHashSet<>(Arrays.asList("firstName", "lastName")), mirror.keySet());
        assertEquals(Arrays.asList("Jonathan", "Sanchez"), mirror.values());
    }
}
