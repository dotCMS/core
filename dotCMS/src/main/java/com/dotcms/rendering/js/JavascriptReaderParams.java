package com.dotcms.rendering.js;

import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Encapsulates the parameters needed to read a javascript file from the file system
 * @author jsanca
 */
public class JavascriptReaderParams {
    private final HTTPMethod httpMethod;
    private final HttpServletRequest request;
    private final String folderName;
    private final User user;
    private final Map<Object, Object> bodyMap;
    private final PageMode pageMode;

    JavascriptReaderParams(final HTTPMethod httpMethod, final HttpServletRequest request, final String folderName,
                           final User user, final Map<Object, Object> bodyMap, final PageMode pageMode) {
        this.httpMethod = httpMethod;
        this.request = request;
        this.folderName = folderName;
        this.user = user;
        this.bodyMap = bodyMap;
        this.pageMode = pageMode;
    }

    HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getFolderName() {
        return folderName;
    }

    public User getUser() {
        return user;
    }

    Map<Object, Object> getBodyMap() {
        return bodyMap;
    }

    public PageMode getPageMode() {
        return pageMode;
    }

    public static class JavascriptReaderParamsBuilder {
        private HTTPMethod httpMethod;
        private HttpServletRequest request;
        private String folderName;
        private User user;
        private Map<Object, Object> bodyMap;
        private PageMode pageMode;

        public JavascriptReaderParamsBuilder setPageMode(final PageMode pageMode) {
            this.pageMode = pageMode;
            return this;
        }

        public JavascriptReaderParamsBuilder setHttpMethod(final HTTPMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public JavascriptReaderParamsBuilder setRequest(final HttpServletRequest request) {
            this.request = request;
            return this;
        }

        public JavascriptReaderParamsBuilder setFolderName(final String folderName) {
            this.folderName = folderName;
            return this;
        }

        public JavascriptReaderParamsBuilder setUser(final User user) {
            this.user = user;
            return this;
        }

        public JavascriptReaderParamsBuilder setBodyMap(final Map<Object, Object> bodyMap) {
            this.bodyMap = bodyMap;
            return this;
        }

        public JavascriptReaderParams build() {
            return new JavascriptReaderParams(httpMethod, request, folderName, user, bodyMap, pageMode);
        }
    }
}
