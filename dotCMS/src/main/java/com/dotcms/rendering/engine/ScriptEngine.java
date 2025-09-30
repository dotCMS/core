package com.dotcms.rendering.engine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

/**
 * Encapsulates the signature for a script engine.
 * @author jsanca
 */
public interface ScriptEngine {


    /**
     * Evaluate a script and returns the Object as part of the result (depending on the implementation)
     *
     * @param request       {@link HttpServletRequest}
     * @param response      {@link HttpServletResponse}
     * @param inputStream   {@link InputStream}
     * @param contextParams {@link Map}
     * @return Object
     */
    default Object eval (final HttpServletRequest request, final HttpServletResponse response,
                 final InputStream inputStream, final Map<String, Object> contextParams) {

        return this.eval(request, response, new InputStreamReader(inputStream), contextParams);
    }

    /**
     * Evaluate a script and returns the Object as part of the result (depending on the implementation)
     *
     * @param request       {@link HttpServletRequest}
     * @param response      {@link HttpServletResponse}
     * @param scriptReader  {@link Reader}
     * @param contextParams {@link Map}
     * @return Object
     */
    Object eval (final HttpServletRequest request, final HttpServletResponse response,
                final Reader scriptReader, final Map<String, Object> contextParams);


    /**
     * Execute a single function (does not include any context rather than the bindings)
     * @param functionName
     * @param script
     * @param bindings
     * @return Object
     */
    Object executeFunction(String functionName, String script, Map<String, Object> bindings, final Object... args);



} // E:O:F:ScriptEngine.
