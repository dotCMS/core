package com.dotcms.ai.domain;

import com.dotmarketing.exception.DotRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;

/**
 * Represents the data of a response from an AI service.
 *
 * <p>
 * This class encapsulates the details of an AI response, including the response content, error message,
 * status, and any exceptions that may have occurred. It provides methods to retrieve and set these details,
 * as well as a method to check if the response was successful.
 * </p>
 *
 * @author vico
 */
public class AIResponseData {

    private String response;
    private String error;
    private ModelStatus status;
    private DotRuntimeException exception;
    private OutputStream output;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public ModelStatus getStatus() {
        return status;
    }

    public void setStatus(ModelStatus status) {
        this.status = status;
    }

    public DotRuntimeException getException() {
        return exception;
    }

    public void setException(DotRuntimeException exception) {
        this.exception = exception;
    }

    public OutputStream getOutput() {
        return output;
    }

    public void setOutput(OutputStream output) {
        this.output = output;
    }

    public boolean isSuccess() {
        return StringUtils.isBlank(error);
    }

    @Override
    public String toString() {
        return "AIResponseData{" +
                "response='" + response + '\'' +
                ", error='" + error + '\'' +
                ", status=" + status +
                '}';
    }
}
