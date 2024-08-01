package com.dotcms.ai.api;

import com.dotcms.ai.model.request.AIRequest;

import java.io.OutputStream;
import java.io.Serializable;

public interface AIClientAPI {

    <T extends Serializable> void sendRequest(final AIRequest<T> request, final OutputStream output);

    <T extends Serializable> void sendRequest(final AIRequest<T> request);

}
