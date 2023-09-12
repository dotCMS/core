package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Assets Request Form is a json representation of a request to get assets
 */
public class FileUploadData {

    @FormDataParam("file")
    private InputStream fileInputStream;

    @FormDataParam("file")
    private FormDataContentDisposition contentDisposition;

    @FormDataParam("assetPath")
    private String assetPath;

    @FormDataParam("detail")
    private String jsonDetail;

    @FormDataParam("detail")
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
