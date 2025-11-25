package com.dotcms.rendering.js;

import com.dotcms.rendering.js.proxy.JsRequest;
import com.dotcms.rendering.js.proxy.JsResponse;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the context of the Javascript execution.
 * @author jsanca
 */
public class JsContext extends HashMap<String, Object> implements Serializable {

    private final JsRequest request;
    private final JsResponse response;
    private final JsDotLogger logger;

    public JsContext(final Builder builder) {
        this.request = builder.request;
        this.response = builder.response;
        this.logger = builder.logger;
        builder.members.forEach(tuple -> this.put(tuple._1, tuple._2));
        this.put("request", this.request);
        this.put("response", this.response);
        this.put("dotlogger", this.logger);
    }

    public JsRequest getRequest() {
        return request;
    }

    public JsResponse getResponse() {
        return response;
    }

    public JsDotLogger getLogger() {
        return logger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JsContext jsContext = (JsContext) o;
        return Objects.equals(request, jsContext.request) && Objects.equals(response, jsContext.response) && Objects.equals(logger, jsContext.logger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), request, response, logger);
    }

    public static final class Builder {
        private JsRequest request; // not present on create
        private JsResponse response;
        private  JsDotLogger logger;

        private List<Tuple2<String, Object>> members = new ArrayList<>();
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

        public Builder put (final String memberName, final Object memberValue) {

            this.members.add(Tuple.of(memberName, memberValue));
            return this;
        }

        public JsContext build() {
            return new JsContext(this);
        }
    }


}
