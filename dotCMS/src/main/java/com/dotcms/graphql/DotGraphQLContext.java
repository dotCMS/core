package com.dotcms.graphql;

import static com.dotcms.util.CollectionsUtils.map;

import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import graphql.kickstart.execution.context.DefaultGraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.dataloader.DataLoaderRegistry;

/**
 * Extends the {@link DefaultGraphQLContext} to be able to set the dotCMS user and have it
 * available from the different {@link graphql.schema.DataFetcher}s
 */
public class DotGraphQLContext extends DefaultGraphQLContext implements
        GraphQLServletContext {

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final User user;
    private final List<Map<String, Object>> fieldCountMaps;
    private final Map<String, Object> params;

    private DotGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse, User user) {
        super(dataLoaderRegistry, subject);
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.user = user;
        this.fieldCountMaps = new ArrayList<>();
        this.params = new HashMap<>();
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    public List<Part> getFileParts() {
        try {
            return httpServletRequest.getParts().stream()
                    .filter(part -> part.getContentType() != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<Part>> getParts() {
        try {
            return httpServletRequest.getParts()
                    .stream()
                    .collect(Collectors.groupingBy(Part::getName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser() {
        return user;
    }

    public void addFieldCount(final String field, final long count) {
        this.fieldCountMaps.add(map("fieldName", field, "totalCount", count));
    }

    public List<Map<String, Object>> getFieldCountMaps() {
        return ImmutableList.copyOf(fieldCountMaps);
    }

    public static Builder createServletContext(DataLoaderRegistry registry, Subject subject) {
        return new Builder(registry, subject);
    }

    public static Builder createServletContext() {
        return new Builder(new DataLoaderRegistry(), null);
    }

    public void addParam(final String key, final Object value) {
        params.put(key, value);
    }

    public Object getParam(final String key) {
        return params.get(key);
    }

    public static class Builder {
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;
        private DataLoaderRegistry dataLoaderRegistry;
        private Subject subject;
        private User user;

        private Builder(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            this.subject = subject;
        }

        public DotGraphQLContext build() {
            return new DotGraphQLContext(dataLoaderRegistry, subject, httpServletRequest,
                    httpServletResponse, user);
        }

        public Builder with(HttpServletRequest httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
            return this;
        }

        public Builder with(DataLoaderRegistry dataLoaderRegistry) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            return this;
        }

        public Builder with(Subject subject) {
            this.subject = subject;
            return this;
        }

        public Builder with(User user) {
            this.user = user;
            return this;
        }

        public Builder with(HttpServletResponse httpServletResponse) {
            this.httpServletResponse = httpServletResponse;
            return this;
        }
    }
}
