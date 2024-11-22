package com.dotcms.rest.api.v1.contentImport;

import com.dotcms.repackage.javax.validation.ValidationException;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.annotate.JsonIgnore;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;

/**
 * Bean class that encapsulates the multipart form parameters for content import operations.
 */
public class ContentImportParams extends Validated {

    @NotNull(message = "The file is required.")
    @FormDataParam("file")
    private InputStream fileInputStream;

    @JsonIgnore
    @FormDataParam("file")
    private FormDataContentDisposition contentDisposition;

    @FormDataParam("form")
    private ContentImportForm form;

    @NotNull(message = "The form data is required.")
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

    public void setJsonForm(String jsonForm) {
        this.jsonForm = jsonForm;
    }

    public String getJsonForm() {
        return jsonForm;
    }

    public void setForm(ContentImportForm form) {
        this.form = form;
    }

    /**
     * Gets the parsed form object, lazily parsing the JSON if needed
     * @return The ContentImportForm object
     */
    public ContentImportForm getForm() throws JsonProcessingException {
        if (null == form && (null != jsonForm && !jsonForm.isEmpty())) {
            form = new ObjectMapper().readValue(jsonForm, ContentImportForm.class);
        }
        return form;
    }

    @Override
    public String toString() {
        return "ContentImportParams{" +
                "form=" + getJsonForm() +
                ", hasFile=" + (fileInputStream != null) +
                ", fileName=" + (contentDisposition != null ? contentDisposition.getFileName() : "null") +
                '}';
    }

    @Override
    public void checkValid() {
        super.checkValid();
        if (contentDisposition == null || contentDisposition.getFileName() == null) {
            throw new ValidationException("The file must have a valid file name.");
        }
    }
}