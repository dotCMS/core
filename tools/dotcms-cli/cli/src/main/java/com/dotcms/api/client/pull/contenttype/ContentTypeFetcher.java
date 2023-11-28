package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final int pageSize = 100;
        int page = 1;

        // Create a list to store all the retrieved content types
        List<ContentType> allContentTypes = new ArrayList<>();

        while (true) {

            // Retrieve a page of content types
            ResponseEntityView<List<ContentType>> contentTypesResponse = contentTypeAPI.getContentTypes(
                    null,
                    page,
                    pageSize,
                    "variable",
                    null,
                    null,
                    null
            );

            // Check if the response contains content types
            if (contentTypesResponse.entity() != null && !contentTypesResponse.entity().isEmpty()) {

                // Add the content types from the current page to the list
                allContentTypes.addAll(contentTypesResponse.entity());

                // Increment the page number
                page++;
            } else {
                // Handle the case where the response doesn't contain content types or an error occurred
                break;
            }
        }

        return allContentTypes;
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