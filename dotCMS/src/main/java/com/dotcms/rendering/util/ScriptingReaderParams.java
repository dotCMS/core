package com.dotcms.rendering.util;

import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Encapsulates the parameters needed to read a script file from the file system
 * @author jsanca
 */
public class ScriptingReaderParams {
    private final HTTPMethod httpMethod;
    private final HttpServletRequest request;
    private final String folderName;
    private final User user;
    private final Map<String, Object> bodyMap;
    private final PageMode pageMode;

    ScriptingReaderParams(final HTTPMethod httpMethod, final HttpServletRequest request, final String folderName,
                          final User user, final Map<String, Object> bodyMap, final PageMode pageMode) {
        this.httpMethod = httpMethod;
        this.request = request;
        this.folderName = folderName;
        this.user = user;
        this.bodyMap = bodyMap;
        this.pageMode = pageMode;
    }

    public HTTPMethod getHttpMethod() {
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

    public Map<String, Object> getBodyMap() {
        return bodyMap;
    }

    public PageMode getPageMode() {
        return pageMode;
    }

    public static class ScriptingReaderParamsBuilder {
        private HTTPMethod httpMethod;
        private HttpServletRequest request;
        private String folderName;
        private User user;
        private Map<String, Object> bodyMap;
        private PageMode pageMode;

        public ScriptingReaderParamsBuilder setPageMode(final PageMode pageMode) {
            this.pageMode = pageMode;
            return this;
        }

        public ScriptingReaderParamsBuilder setHttpMethod(final HTTPMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public ScriptingReaderParamsBuilder setRequest(final HttpServletRequest request) {
            this.request = request;
            return this;
        }

        public ScriptingReaderParamsBuilder setFolderName(final String folderName) {
            this.folderName = folderName;
            return this;
        }

        public ScriptingReaderParamsBuilder setUser(final User user) {
            this.user = user;
            return this;
        }

        public ScriptingReaderParamsBuilder setBodyMap(final Map<String, Object> bodyMap) {
            this.bodyMap = bodyMap;
            return this;
        }

        public ScriptingReaderParams build() {
            return new ScriptingReaderParams(httpMethod, request, folderName, user, bodyMap, pageMode);
        }
    }
}
