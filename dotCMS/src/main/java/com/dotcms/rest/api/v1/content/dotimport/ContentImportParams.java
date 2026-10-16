package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import net.minidev.json.annotate.JsonIgnore;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Bean class that encapsulates the multipart form parameters for content import operations.
 */
public class ContentImportParams extends Validated {

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

    /**
     * A CSV the caller already uploaded, such as the one a preview stored, sent instead of the
     * file so it isn't uploaded again (#37896).
     */
    @FormDataParam("tempFileId")
    private String tempFileId;

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

    /**
     * The id of an already uploaded CSV to import instead of a new file.
     *
     * @return the temp file id, or {@code null} when a file is sent
     */
    public String getTempFileId() {
        return tempFileId;
    }

    /**
     * Sets the id of an already uploaded CSV to import instead of a new file.
     *
     * @param tempFileId the temp file id
     */
    public void setTempFileId(final String tempFileId) {
        this.tempFileId = tempFileId;
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
                ", tempFileId=" + tempFileId +
                ", fileName=" + (contentDisposition != null ? contentDisposition.getFileName() : "null") +
                '}';
    }

    @Override
    public void checkValid() {
        super.checkValid();

        if (jsonForm == null || jsonForm.isEmpty()) {
            throw new ValidationException("The form data is required.");
        }

        final boolean hasTempFile = tempFileId != null && !tempFileId.isBlank();
        if (fileInputStream != null && hasTempFile) {
            throw new ValidationException("Send either the file or a tempFileId, not both.");
        }
        if (hasTempFile) {
            // The temp file's existence and ownership are checked when it is resolved.
            return;
        }
        if (fileInputStream == null) {
            throw new ValidationException("The file is required.");
        }
        if (contentDisposition == null || contentDisposition.getFileName() == null) {
            throw new ValidationException("The file must have a valid file name.");
        }
        if (!isCsvFile(contentDisposition.getFileName())) {
            throw new ValidationException("The file must be a CSV file.");
        }
    }

    private boolean isCsvFile(final String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }
}