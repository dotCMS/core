package com.dotcms.publishing;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.Map;

/**
 * This bean to read data from the yml file that stores the Push Publishing Filters
 * The file might look like this:
 * key:   (this would be the filename)
 * title: (arbitrary title for the exclude filter)
 * defaultFilter: true|false
 * roles: [ list of roles that can access the filter]
 * filters:
 * 	excludeDependencyQuery:   ( lucene query that dependent content is checked against to exclude)
 * 	excludeQuery:             (lucene query that added content is checked against to exclude)
 * 	excludeDependencyClasses: [ list of classes that should never be pushed as dependencies]
 * 	excludeClasses:           [ list of classes that should never be pushed]
 * 	incremental:              true|false . (basically, force push - should the push history be consulted when pushing?  Defaults to true)
 * 	dependencies:             true|false, defaults to true
 * 	relationships:            true|false, defaults to true
 */

public class FilterDescriptor {

    private final String key;
    private final String title;
    private final boolean defaultFilter;
    private final String roles;
    private final Map<String,Object> filters;

    /**
     * This constructor isn't used by the object mapper that reads the yml files.
     * it's only meant to be used for testing
     * @param key
     * @param title
     * @param defaultFilter
     * @param roles
     * @param filters
     */
    @VisibleForTesting
    @JsonCreator
    public FilterDescriptor(@JsonProperty("key")final String key,
            @JsonProperty("title")final String title,
            @JsonProperty("filters")final Map<String, Object> filters,
            @JsonProperty("default") final boolean defaultFilter,
            @JsonProperty("roles")final String roles) {
        this.key = key;
        this.title = title;
        this.filters = filters;
        this.defaultFilter = defaultFilter;
        this.roles = roles;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public Map<String,Object> getFilters() {
        return filters;
    }

    public boolean isDefaultFilter(){ return defaultFilter;}

    public String getRoles() { return roles; }

    @Override
    public String toString() {
        return "FilterDescriptor{" +
                "key=" + this.key +
                ", title=" + this.title +
                ", default=" + this.defaultFilter +
                ", roles=" + this.roles +
                ", filters{" + this.filters + "}" +
                '}';
    }
}
