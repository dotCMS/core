package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.exception.DotDataException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;

/**
 * Bean class that encapsulates the multipart form parameters for content import operations.
 */
public class ContentImportParams extends Validated {

    @FormDataParam("file")
    private InputStream fileInputStream;

    @FormDataParam("file")
    private FormDataContentDisposition contentDisposition;

    @FormDataParam("form")
    private com.dotcms.rest.api.v1.contentImport.ContentImportForm form;

    @FormDataParam("form")
    private String jsonForm;

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

    public String getJsonForm() {
        return jsonForm;
    }

    public void setForm(com.dotcms.rest.api.v1.contentImport.ContentImportForm form) {
        this.form = form;
    }

    /**
     * Gets the parsed form object, lazily parsing the JSON if needed
     * @return The ContentImportForm object
     */
    public com.dotcms.rest.api.v1.contentImport.ContentImportForm getForm() throws DotDataException, JsonProcessingException {
        if (null == form && (null != jsonForm && !jsonForm.isEmpty())) {
            form = new ObjectMapper().readValue(jsonForm, com.dotcms.rest.api.v1.contentImport.ContentImportForm.class);
        }

        if (form == null) {
            throw new DotDataException("Import form parameters are required");
        }
        return form;
    }

    @Override
    public String toString() {
        return "ContentImportParams{" +
                "form=" + form +
                ", hasFile=" + (fileInputStream != null) +
                ", fileName=" + (contentDisposition != null ? contentDisposition.getFileName() : "null") +
                '}';
    }
}