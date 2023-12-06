package com.dotcms.api.client.push.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest.Builder;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
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
    public void add(File localFile, ContentType localContentType,
            Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = new Builder().of(localContentType).build();
        contentTypeAPI.createContentTypes(List.of(saveRequest));
    }

    @ActivateRequestContext
    @Override
    public void edit(File localFile, ContentType localContentType, ContentType serverContentType,
            Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = new Builder().of(localContentType).build();
        contentTypeAPI.updateContentTypes(localContentType.variable(), saveRequest);
    }

    @ActivateRequestContext
    @Override
    public void remove(ContentType serverContentType, Map<String, Object> customOptions) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        contentTypeAPI.delete(serverContentType.variable());
    }

}