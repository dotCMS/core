package com.dotcms.model.asset;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;

public class FileUploadData {

    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    @FormParam("file")
    public InputStream fileInputStream;

    @FormParam("assetPath")
    public String assetPath;

    @PartType(MediaType.APPLICATION_JSON)
    @FormParam("detail")
    public FileUploadDetail detail;

    public InputStream getFile() {
        return fileInputStream;
    }

    public void setFile(InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public FileUploadDetail getDetail() {
        return detail;
    }

    public void setDetail(FileUploadDetail detail) {
        this.detail = detail;
    }

}
