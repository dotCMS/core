package com.dotcms.api.client.push.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentFetcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@Dependent
public class ContentTypeFetcher implements ContentFetcher<ContentType> {

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<ContentType> fetch() {

        var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final int pageSize = 10;
        int page = 1;

        // Create a list to store all the retrieved sites
        List<ContentType> allContentTypes = new ArrayList<>();

        while (true) {

            // Retrieve a page of sites
            ResponseEntityView<List<ContentType>> response = contentTypeAPI.getContentTypes(
                    null,
                    page,
                    pageSize,
                    "variable",
                    null,
                    null,
                    null
            );

            // Check if the response contains sites
            if (response.entity() != null && !response.entity().isEmpty()) {

                allContentTypes.addAll(response.entity());

                // Increment the page number
                page++;
            } else {
                // Handle the case where the response doesn't contain sites or an error occurred
                break;
            }
        }

        return allContentTypes;
    }

}