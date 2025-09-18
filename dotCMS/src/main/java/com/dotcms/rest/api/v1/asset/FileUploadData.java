package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.InputStream;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * File upload form data for multipart/form-data requests
 */
@Schema(description = "File upload form data with asset path and optional details")
public class FileUploadData {

    @FormDataParam("file")
    @Schema(description = "File input stream to upload", required = true)
    private InputStream fileInputStream;

    @FormDataParam("file")
    @Schema(description = "File content disposition information", hidden = true)
    private FormDataContentDisposition contentDisposition;

    @FormDataParam("assetPath")
    @Schema(
        description = "Full path where the asset should be stored including site and folder structure",
        example = "//demo.dotcms.com/application/assets/my-file.pdf",
        required = true
    )
    private String assetPath;

    @FormDataParam("detail")
    @Schema(
        description = "JSON string containing file metadata and additional details",
        example = "{\"title\":\"My Document\",\"description\":\"Important document\",\"tags\":[\"document\",\"important\"]}",
        required = false
    )
    private String jsonDetail;

    @FormDataParam("detail")
    @Schema(description = "Parsed file upload detail object", hidden = true)
    private FileUploadDetail detail;

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

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public String getJsonDetail() {
        return jsonDetail;
    }

    public FileUploadDetail getDetail() throws JsonProcessingException {
        if(null == detail){
            detail =  new ObjectMapper().readValue(getJsonDetail(), FileUploadDetail.class);
        }
        return detail;
    }

    public void setDetail(FileUploadDetail detail) {
        this.detail = detail;
    }
}
