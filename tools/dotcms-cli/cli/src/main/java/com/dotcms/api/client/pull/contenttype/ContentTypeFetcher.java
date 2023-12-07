package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.contenttype.model.type.ContentType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

@Dependent
public class ContentTypeFetcher implements ContentFetcher<ContentType>, Serializable {

    private static final long serialVersionUID = 8846079246656891055L;

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<ContentType> fetch(Map<String, Object> customOptions) {

        // Fetching the all the existing content types
        List<ContentType> allContentTypes = new ArrayList<>();

        final ContentTypeIterator contentTypeIterator = new ContentTypeIterator(
                clientFactory, 100
        );
        while (contentTypeIterator.hasNext()) {
            List<ContentType> contentTypes = contentTypeIterator.next();
            allContentTypes.addAll(contentTypes);
        }

        // Create a ForkJoinPool to process the content types in parallel
        // We need this extra logic because the site API returns when calling all content types an
        // object that is not equal to the one returned when calling by id or by var name, it is a
        // reduced, so we need to call the API for each content type to get the full object.
        var forkJoinPool = ForkJoinPool.commonPool();
        var task = new HttpRequestTask(allContentTypes, this);
        return forkJoinPool.invoke(task);
    }

    @ActivateRequestContext
    @Override
    public ContentType fetchByKey(String contentTypeIdOrVarName, Map<String, Object> customOptions)
            throws NotFoundException {

        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final var responseEntityView = contentTypeAPI.getContentType(
                contentTypeIdOrVarName, null, null
        );
        return responseEntityView.entity();
    }

}