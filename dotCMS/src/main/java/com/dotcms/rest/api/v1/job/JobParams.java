package com.dotcms.rest.api.v1.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * This class represents the parameters for a job.
 * This bean encapsulates the expected parameters for a job.
 * that would be a file and a json object.
 * The json object is expected to be a simple key value pair.
 */
public class JobParams {

    @FormDataParam("file")
    private InputStream fileInputStream;

    @FormDataParam("file")
    private FormDataContentDisposition contentDisposition;

    @FormDataParam("params")
    private String jsonParams;

    @FormDataParam("params")
    private Map<String, Object> params;

    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    public void setFileInputStream(InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public FormDataContentDisposition getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(FormDataContentDisposition contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public void setJsonParams(String jsonParams) {
        this.jsonParams = jsonParams;
    }

    public Map<String, Object> getParams() throws JsonProcessingException {
        if (null == params && (null != jsonParams && !jsonParams.isEmpty())) {
            params = new ObjectMapper().readValue(jsonParams, Map.class);
        }
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getJsonParams() {
        return jsonParams;
    }

}
