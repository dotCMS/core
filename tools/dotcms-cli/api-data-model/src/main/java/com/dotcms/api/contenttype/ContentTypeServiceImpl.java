package com.dotcms.api.contenttype;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.contenttype.ContentType;
import io.quarkus.arc.DefaultBean;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@DefaultBean
@ApplicationScoped
public class ContentTypeServiceImpl implements ContentTypeService {

    @Inject
    RestClientFactory clientFactory;

    @Override
    public List<ContentType> getContentTypes() {
        final ContentTypeAPI api = clientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> result = api.getContentTypes(null,null, null, null, null, null, null);
        return result.entity();
    }
}
