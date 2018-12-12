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
    private Map<String, String> bodyMap;
    private WebResource webResource;

    public HTTPMethodParamsBuilder setRequest(HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public HTTPMethodParamsBuilder setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
        return this;
    }

    public HTTPMethodParamsBuilder setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    public HTTPMethodParamsBuilder setFolderName(String folderName) {
        this.folderName = folderName;
        return this;
    }

    public HTTPMethodParamsBuilder setPathParam(String pathParam) {
        this.pathParam = pathParam;
        return this;
    }

    public HTTPMethodParamsBuilder setBodyMap(Map<String, String> bodyMap) {
        this.bodyMap = bodyMap;
        return this;
    }

    public HTTPMethodParamsBuilder setWebResource(WebResource webResource) {
        this.webResource = webResource;
        return this;
    }

    public VTLResourceIntegrationTest.HTTPMethodParams build() {
        return new VTLResourceIntegrationTest.HTTPMethodParams(request, servletResponse, uriInfo,
                folderName, pathParam, bodyMap, webResource);
    }
}