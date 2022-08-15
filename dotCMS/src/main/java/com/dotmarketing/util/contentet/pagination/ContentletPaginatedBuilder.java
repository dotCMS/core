package com.dotmarketing.util.contentet.pagination;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

public class ContentletPaginatedBuilder {

    private String luceneQuery;
    private User user = APILocator.systemUser();
    private boolean respectFrontendRoles;
    private ContentletAPI contentletAPI;
    private int perPage = -1;
    private Lazy<Integer> perPageDefaultValue = Lazy.of(() -> Config.getIntProperty("CONTENTLET_PER_PAGE", 1000));

    public ContentletPaginatedBuilder setLuceneQuery(String luceneQuery) {
        this.luceneQuery = luceneQuery;
        return this;
    }

    public ContentletPaginatedBuilder setUser(User user) {
        this.user = user;
        return this;
    }

    public ContentletPaginatedBuilder setRespectFrontendRoles(boolean respectFrontendRoles) {
        this.respectFrontendRoles = respectFrontendRoles;
        return this;
    }

    @VisibleForTesting
    public ContentletPaginatedBuilder setContentletAPI(ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
        return this;
    }

    public ContentletPaginatedBuilder setPerPage(int perPage) {
        this.perPage = perPage;
        return this;
    }

    public ContentletsPaginated build(){
        if (!UtilMethods.isSet(contentletAPI)) {
            contentletAPI = APILocator.getContentletAPI();
        }

        if (perPage < 0) {
            perPage = perPageDefaultValue.get();
        }

        return new ContentletsPaginated(luceneQuery, user, respectFrontendRoles, perPage, contentletAPI);
    }
}
