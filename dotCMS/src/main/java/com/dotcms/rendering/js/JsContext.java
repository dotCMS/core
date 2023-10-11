package com.dotcms.rendering.js;

import com.dotcms.rendering.js.proxy.JsRequest;
import com.dotcms.rendering.js.proxy.JsResponse;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates the context of the Javascript execution.
 */
public class JsContext implements Serializable {

    private final JsRequest request;
    private final JsResponse response;

    private final JsDotLogger logger;

    public JsContext(final Builder builder) {
        this.request = builder.request;
        this.response = builder.response;
        this.logger = builder.logger;
    }

    @HostAccess.Export
    public JsRequest getRequest() {
        return request;
    }

    @HostAccess.Export
    public JsResponse getResponse() {
        return response;
    }

    @HostAccess.Export
    public JsDotLogger getLogger() {
        return logger;
    }

    public static final class Builder {
        private JsRequest request; // not present on create
        private JsResponse response;
        private  JsDotLogger logger;
        public Builder request(final JsRequest request) {
            this.request = request;
            return this;
        }

        public Builder response(final JsResponse response) {
            this.response = response;
            return this;
        }

        public Builder logger(final JsDotLogger logger) {
            this.logger = logger;
            return this;
        }

        public JsContext build() {
            return new JsContext(this);
        }
    }


}
