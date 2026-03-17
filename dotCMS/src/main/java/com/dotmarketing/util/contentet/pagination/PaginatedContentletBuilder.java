package com.dotmarketing.util.contentet.pagination;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

/**
 * Builder for {@link PaginatedContentlets}
 */
public class PaginatedContentletBuilder {

    private String luceneQuery;
    private User user = APILocator.systemUser();
    private boolean respectFrontendRoles;
    private ContentletAPI contentletAPI;
    private int perPage = -1;
    private int scrollApiThreshold = -1;
    private Lazy<Integer> perPageDefaultValue = Lazy.of(() -> Config.getIntProperty("CONTENTLET_PER_PAGE", 1000));

    public PaginatedContentletBuilder setLuceneQuery(String luceneQuery) {
        this.luceneQuery = luceneQuery;
        return this;
    }

    public PaginatedContentletBuilder setUser(User user) {
        this.user = user;
        return this;
    }

    public PaginatedContentletBuilder setRespectFrontendRoles(boolean respectFrontendRoles) {
        this.respectFrontendRoles = respectFrontendRoles;
        return this;
    }

    @VisibleForTesting
    public PaginatedContentletBuilder setContentletAPI(ContentletAPI contentletAPI) {
        this.contentletAPI = contentletAPI;
        return this;
    }

    public PaginatedContentletBuilder setPerPage(int perPage) {
        this.perPage = perPage;
        return this;
    }

    @VisibleForTesting
    public PaginatedContentletBuilder setScrollApiThreshold(int scrollApiThreshold) {
        this.scrollApiThreshold = scrollApiThreshold;
        return this;
    }

    public PaginatedContentlets build(){
        if (!UtilMethods.isSet(contentletAPI)) {
            contentletAPI = APILocator.getContentletAPI();
        }

        if (perPage < 0) {
            perPage = perPageDefaultValue.get();
        }

        return new PaginatedContentlets(luceneQuery, user, respectFrontendRoles, perPage, contentletAPI, scrollApiThreshold);
    }
}
