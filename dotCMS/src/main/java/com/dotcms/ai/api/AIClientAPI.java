package com.dotcms.ai.api;

import com.dotcms.ai.model.request.AIRequest;

import java.io.OutputStream;

public interface AIClientAPI {

    void sendRequest(final AIRequest request, final OutputStream output);

    void sendRequest(final AIRequest request);

}
