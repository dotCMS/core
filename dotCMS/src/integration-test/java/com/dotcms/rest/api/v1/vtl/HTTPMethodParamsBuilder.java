package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.rest.WebResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class HTTPMethodParamsBuilder {
    private HttpServletRequest request;
    private HttpServletResponse servletResponse;
    private UriInfo uriInfo;
    private String folderName;
    private String pathParam;
    private Map<String, Object> bodyMap;
    private WebResource webResource;
    private String bodyMapString;

    public HTTPMethodParamsBuilder setRequest(final HttpServletRequest request) {
        this.request = request;
        return this;
    }

    HTTPMethodParamsBuilder setServletResponse(final HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
        return this;
    }

    HTTPMethodParamsBuilder setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    HTTPMethodParamsBuilder setFolderName(final String folderName) {
        this.folderName = folderName;
        return this;
    }

    HTTPMethodParamsBuilder setPathParam(final String pathParam) {
        this.pathParam = pathParam;
        return this;
    }

    HTTPMethodParamsBuilder setBodyMap(final Map<String, Object> bodyMap) {
        this.bodyMap = bodyMap;
        return this;
    }

    HTTPMethodParamsBuilder setWebResource(final WebResource webResource) {
        this.webResource = webResource;
        return this;
    }

    HTTPMethodParamsBuilder setBodyMapString(final String bodyMapString) {
        this.bodyMapString = bodyMapString;
        return this;
    }

    public VTLResourceIntegrationTest.HTTPMethodParams build() {
        return new VTLResourceIntegrationTest.HTTPMethodParams(request, servletResponse, uriInfo,
                folderName, pathParam, bodyMap, webResource, bodyMapString);
    }
}