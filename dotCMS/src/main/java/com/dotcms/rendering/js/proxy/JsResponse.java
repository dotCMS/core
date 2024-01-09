package com.dotcms.rendering.js.proxy;

import com.dotcms.rendering.JsEngineException;
import com.dotcms.rendering.js.JsHeaders;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import io.vavr.control.Try;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;


/**
 * Abstraction of the Response for the Javascript engine.
 * @author jsanca
 */
public class JsResponse implements Serializable, JsProxyObject<HttpServletResponse> {

    public static final String APPLICATION_JSON = "application/json";
    private final JsHeaders headers = new JsHeaders();
    private final HttpServletResponse response;

    public JsResponse(final HttpServletResponse response) {
        this.response = response;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    public HttpServletResponse  getWrappedObject() {
        return this.getResponse();
    }

    @HostAccess.Export
    public JsHeaders headers() {
        return headers;
    }

    @HostAccess.Export
    public JsResponse ok() {
        response.setStatus(HttpServletResponse.SC_OK);
        return this;
    }

    @HostAccess.Export
    public JsResponse status(final int code) {
        this.response.setStatus(code);
        return this;
    }

    @HostAccess.Export
    public JsResponse error(final int code) {
        Try.run(()->this.response.sendError(code)).getOrElseThrow(JsEngineException::new);
        return this;
    }

    @HostAccess.Export
    public JsResponse redirect(final String location) {
        Try.run(()->this.response.sendRedirect(location)).getOrElseThrow(JsEngineException::new);
        return this;
    }

    @HostAccess.Export
    public JsResponse json(final Object json) {

        Try.run(()->{

            this.response.setContentType(APPLICATION_JSON);
            DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(this.response.getWriter(), json);
            this.response.getWriter().flush();
        }).getOrElseThrow(JsEngineException::new);

        return this;
    }

    @HostAccess.Export
    public JsResponse json(final String json) {

        Try.run(()->{
            this.response.setContentType(APPLICATION_JSON);
            this.response.getWriter().write(json);
            this.response.getWriter().flush();
        }).getOrElseThrow(JsEngineException::new);

        return this;
    }

    @HostAccess.Export
    public JsResponse text(final String text) {

        Try.run(()->{
            this.response.getWriter().write(text);
            this.response.getWriter().flush();
        }).getOrElseThrow(JsEngineException::new);

        return this;
    }

}
