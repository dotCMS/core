package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.domain.AIRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;

import javax.ws.rs.HttpMethod;
import java.io.OutputStream;
import java.io.Serializable;

public interface AIClient {

    AIClient NOOP = new AIClient() {
        @Override
        public AIProvider getProvider() {
            return AIProvider.NONE;
        }

        @Override
        public OutputStream sendRequest(final AIRequest<? extends Serializable> request) {
            return throwUnsupported();
        }

        private OutputStream throwUnsupported() {
            throw new UnsupportedOperationException("Noop client does not support sending requests");
        }
    };

    AIProvider getProvider();

    static HttpUriRequest resolveMethod(final String method, final String url) {
        switch(method) {
            case HttpMethod.POST:
                return new HttpPost(url);
            case HttpMethod.PUT:
                return new HttpPut(url);
            case HttpMethod.DELETE:
                return new HttpDelete(url);
            case "patch":
                return new HttpPatch(url);
            case HttpMethod.GET:
            default:
                return new HttpGet(url);
        }
    }

    OutputStream sendRequest(final AIRequest<? extends Serializable> request);

}
