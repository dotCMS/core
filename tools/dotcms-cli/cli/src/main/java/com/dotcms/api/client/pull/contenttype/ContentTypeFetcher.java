package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.contenttype.model.type.ContentType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.context.ManagedExecutor;

@Dependent
public class ContentTypeFetcher implements ContentFetcher<ContentType>, Serializable {

    private static final long serialVersionUID = 8846079246656891055L;

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    ManagedExecutor executor;

    @ActivateRequestContext
    @Override
    public List<ContentType> fetch(boolean failFast, Map<String, Object> customOptions) {

        // Fetching the all the existing content types
        List<ContentType> allContentTypes = new ArrayList<>();

        final ContentTypeIterator contentTypeIterator = new ContentTypeIterator(
                clientFactory, 100
        );
        while (contentTypeIterator.hasNext()) {
            List<ContentType> contentTypes = contentTypeIterator.next();
            allContentTypes.addAll(contentTypes);
        }

        // Create an HttpRequestTask to process the content types in parallel
        // We need this extra logic because the content type API returns when calling all content
        // types an object that is not equal to the one returned when calling by id or by var name,
        // it is a reduced, so we need to call the API for each content type to get the full object.
        var task = new HttpRequestTask(this, executor);
        task.setTaskParams(allContentTypes);

        return task.compute().join();
    }

    @ActivateRequestContext
    @Override
    public ContentType fetchByKey(String contentTypeIdOrVarName, boolean failFast,
            Map<String, Object> customOptions)
            throws NotFoundException {

        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final var responseEntityView = contentTypeAPI.getContentType(
                contentTypeIdOrVarName, null, null
        );
        return responseEntityView.entity();
    }

}