package com.dotcms.api.client.push.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.StringUtils;

@Dependent
public class ContentTypePushHandler implements PushHandler<ContentType> {

    @Inject
    protected RestClientFactory clientFactory;

    @Override
    public Class<ContentType> type() {
        return ContentType.class;
    }

    @Override
    public String title() {
        return "ContentTypes";
    }

    @Override
    public String fileName(final ContentType contentType) {
        return NamingUtils.contentTypeFileName(contentType);
    }

    @Override
    public String contentSimpleDisplay(ContentType contentType) {

        final var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return String.format(
                "varName: [%s] id: [%s] host: [%s] modDate:[%s] desc: [%s]",
                contentType.variable(),
                contentType.id(),
                contentType.host(),
                contentType.modDate() != null ? simpleDateFormat.format(contentType.modDate())
                        : "n/a",
                StringUtils.isNotEmpty(contentType.description()) ? StringUtils.abbreviate(
                        contentType.description(), 50) : "n/a"
        );
    }

    @ActivateRequestContext
    @Override
    public ContentType add(File localFile, ContentType localContentType,
            Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(localContentType).build();
        final var response = contentTypeAPI.createContentTypes(List.of(saveRequest));

        return response.entity().stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No ContentType was created"));
    }

    @ActivateRequestContext
    @Override
    public ContentType edit(File localFile, ContentType localContentType,
            ContentType serverContentType,
            Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(localContentType).build();
        final var response = contentTypeAPI.updateContentType(localContentType.variable(),
                saveRequest);

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public void remove(ContentType serverContentType, Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        contentTypeAPI.delete(serverContentType.variable());
    }

}