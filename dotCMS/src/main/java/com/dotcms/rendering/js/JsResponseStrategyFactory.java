package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.rendering.JsEngineException;
import com.dotcms.rendering.js.proxy.JsResponse;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.Map;

/**
 * {@link JsResponseStrategy} Factory
 * @author jsanca
 */
public class JsResponseStrategyFactory {

    public static final String RESPONSE = "response";

    private static class SingletonHolder {
        private static final JsResponseStrategyFactory INSTANCE = new JsResponseStrategyFactory();
    }
    /**
     * Get the instance.
     * @return JsResponseStrategyFactory
     */
    public static JsResponseStrategyFactory getInstance() {

        return JsResponseStrategyFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    private final JsResponseStrategy jsResponseStrategy = ((request, response, user, cache, context, result) -> {

        return Response.status(JsResponse.class.cast(result).getResponse().getStatus()).build();
    });

    private final JsResponseStrategy dotJSONStrategy = ((request, response, user, cache, context, result) -> {

        final DotJSON dotJSON = (DotJSON) result;
        if (UtilMethods.isSet(dotJSON.get("errors"))) {
            return Response.status(Response.Status.BAD_REQUEST).entity(dotJSON.get("errors")).build();
        }
        // let's add it to cache
        cache.add(request, user, dotJSON);
        return Response.ok(dotJSON.getMap()).build();
    });

    private final JsResponseStrategy charSequenceStrategy = ((request, response, user, cache, context, result) -> {

        final HttpServletResponse wrapperResponse = null != context.get(RESPONSE) ?
                (HttpServletResponse) context.get(RESPONSE) : response;

        if (!wrapperResponse.isCommitted()) {

            final String contentType = (wrapperResponse != null && wrapperResponse.getContentType() != null) ?
                    wrapperResponse.getContentType() : MediaType.TEXT_PLAIN_TYPE.toString();

            addHeaders(response, wrapperResponse);

            return UtilMethods.isSet(contentType)
                    ? Response.ok(resultToString(result)).type(contentType).build()
                    : Response.ok(resultToString(result)).type(MediaType.TEXT_PLAIN_TYPE).build();
        }

        return Response.serverError().build();
    });

    private static void addHeaders(final HttpServletResponse response, final HttpServletResponse wrapperResponse) {
        if (wrapperResponse != null && wrapperResponse.getHeaderNames() != null) {
            for (final String headerName : wrapperResponse.getHeaderNames()) {
                response.setHeader(headerName, wrapperResponse.getHeader(headerName));
            }
        }
    }

    private final JsResponseStrategy defaultStrategy = ((request, response, user, cache, context, result) -> {

        final HttpServletResponse wrapperResponse = null != context.get(RESPONSE) ?
                (HttpServletResponse) context.get(RESPONSE) : response;

        final StringWriter stringWriter = new StringWriter();
        Try.run(()->DotObjectMapperProvider.getInstance().
                getDefaultObjectMapper().writeValue(stringWriter, result)).getOrElseThrow(JsEngineException::new);

        if (!wrapperResponse.isCommitted()) {

            final String contentType = (wrapperResponse != null && wrapperResponse.getContentType() != null) ?
                    wrapperResponse.getContentType() : MediaType.APPLICATION_JSON;

            addHeaders(response, wrapperResponse);

            return UtilMethods.isSet(contentType)
                    ? Response.ok(stringWriter.toString()).type(contentType).build()
                    : Response.ok(stringWriter.toString()).type(MediaType.APPLICATION_JSON).build();
        }

        return Response.serverError().build();
    });
    private String resultToString(final Object result) {

        if (result instanceof Map) {

            final Map<?,?> map = Map.class.cast(result); // note: we do not know what could it be, so we have to handle as a generic Map.
            if (map.containsKey("output")) {
                return map.get("output").toString();
            }
        }
        return result.toString();
    }

    public JsResponseStrategy get(final Object result) {

        if (result instanceof JsResponse) {

            return jsResponseStrategy;
        }

        // if it is the DotJSON means the user has used the DotJson to populate a final json
        if (result instanceof DotJSON) {

            return dotJSONStrategy;
        }

        // if it is a char sequence means wants to print a template
        if (result instanceof CharSequence) {

            return charSequenceStrategy;
        }

        // if it is a string, means the user has used the DotJson to populate a final json
        return defaultStrategy; // return default
    } // get.
}
