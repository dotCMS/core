package com.dotcms.publishing;

import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;

/**
 * This class represents the YML file that stores data about a specific Push Publishing Filter. The filter key will be
 * set based on the fileName. Such a file might look like this:
 * <ul>
 *     <li>title: The human-readable title for the filter.</li>
 *     <li>sort: Determines the order in which this Filter will be returned in the list of Filters. If not specified, a
 *     default value will be assigned by default via the {@link #DEFAULT_SORT_VALUE} variable.</li>
 *     <li>defaultFilter: true|false</li>
 *     <li>roles: List of roles that can access the filter</li>
 *     <li>filters:</li>
 *     <ul>
 *       <li>excludeDependencyQuery:   Lucene query that dependent content is checked against to exclude</li>
 *       <li>excludeQuery:             Lucene query that added content is checked against to exclude</li>
 *       <li>excludeDependencyClasses: List of classes that should never be pushed as dependencies</li>
 *       <li>excludeClasses:           List of classes that should never be pushed</li>
 *       <li>forcePush:                true|false . (basically, force push - should the push history be consulted when
 *                                     pushing? Defaults to false)</li>
 *       <li>dependencies:             true|false, defaults to true</li>
 *       <li>relationships:            true|false, defaults to true</li>
 *     </ul>
 * </ul>
 *
 * @author Erick Gonzalez
 * @since Mar 6th, 2020
 */
public class FilterDescriptor implements Comparable<FilterDescriptor> {

    private String key;
    private final String title;
    private final String sort;
    private final boolean defaultFilter;
    private final String roles;
    private final Map<String,Object> filters;
    public static final String DEPENDENCIES_KEY = "dependencies";
    public static final String RELATIONSHIPS_KEY = "relationships";
    public static final String EXCLUDE_CLASSES_KEY = "excludeClasses";
    public static final String EXCLUDE_DEPENDENCY_CLASSES_KEY = "excludeDependencyClasses";
    public static final String EXCLUDE_QUERY_KEY = "excludeQuery";
    public static final String EXCLUDE_DEPENDENCY_QUERY_KEY = "excludeDependencyQuery";
    public static final String FORCE_PUSH_KEY = "forcePush";
    public static final String DEFAULT_SORT_VALUE = "1000";

    /**
     * This constructor isn't used by the object mapper that reads the yml files, which is only meant to be used for
     * testing.
     *
     * @param key           The name of the YAML file containing the information for this filter.
     * @param title         The human-readable title for the filter.
     * @param filters       The Map of filters indicating what objects are included, excluded, how to handle
     *                      dependencies, relationships, force pushes, etc.
     * @param defaultFilter If this should be the default Push Publishing Filter, set to {@code true}.
     * @param roles         The dotCMS Roles that can use this filter.
     */
    @VisibleForTesting
    public FilterDescriptor(@JsonProperty("key")final String key,
                            @JsonProperty("title")final String title,
                            @JsonProperty("filters")final Map<String, Object> filters,
                            @JsonProperty("default") final boolean defaultFilter,
                            @JsonProperty("roles")final String roles) {
        this(key, title, StringPool.BLANK, filters, defaultFilter, roles);
    }

    /**
     * This constructor isn't used by the object mapper that reads the yml files, which is only meant to be used for
     * testing.
     *
     * @param key           The name of the YAML file containing the information for this filter.
     * @param title         The human-readable title for the filter.
     * @param sort          The order or position in which this filter will be returned -- usually for UI purposes.
     * @param filters       The Map of filters indicating what objects are included, excluded, how to handle
     *                      dependencies, relationships, force pushes, etc.
     * @param defaultFilter If this should be the default Push Publishing Filter, set to {@code true}.
     * @param roles         The dotCMS Roles that can use this filter.
     */
    @VisibleForTesting
    @JsonCreator
    public FilterDescriptor(@JsonProperty("key")final String key,
            @JsonProperty("title")final String title,
            @JsonProperty("sort") final String sort,
            @JsonProperty("filters")final Map<String, Object> filters,
            @JsonProperty("default") final boolean defaultFilter,
            @JsonProperty("roles")final String roles) {
        this.key = key;
        this.title = title;
        this.sort = sort;
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

    /**
     * Returns the order in which this Filter Descriptor will be returned -- usually for UI purposes. If no value is
     * set, then the value specified by {@link #DEFAULT_SORT_VALUE} will be used instead.
     *
     * @return The sort order for the Filter Descriptor.
     */
    public String getSort() {
        return UtilMethods.isSet(this.sort) ? this.sort : DEFAULT_SORT_VALUE;
    }

    public Map<String,Object> getFilters() {
        return filters;
    }

    public boolean isDefaultFilter(){ return defaultFilter;}

    public String getRoles() { return roles; }

    public void setKey(final String key){
        this.key = key;
    }

    /**
     * Validates the structure and the data of the YAML File and adds the errors to a list.
     * Validates:
     * - Remove required Property (Title, Roles or Filters, default property if not set it will be set to true as default, properties inside Filters are not required).
     * - Add New Property to Filters property.
     * - Boolean property set as any other value than true | false
     * @throws DotDataValidationException
     */
    public void validate() throws DotDataValidationException {
        final List<String> errors = new ArrayList<>();

        if(isNotSet(getTitle())){
            errors.add("The required field `Title` isn't set on the incoming file.");
        }
        if (isSet(getSort()) && !NumberUtils.isNumber(getSort())) {
            errors.add("The field `Sort` is not a valid number");
        }
        if(isNotSet(getRoles())){
            errors.add("The required field `Roles` isn't set on the incoming file.");
        }

        if(!isSet(getFilters())){
            errors.add("The required field `Filters` isn't set on the incoming file.");
        }

        final List <String> listOfPossibleFilters = new ArrayList<>();
        listOfPossibleFilters.add(DEPENDENCIES_KEY);
        listOfPossibleFilters.add(RELATIONSHIPS_KEY);
        listOfPossibleFilters.add(EXCLUDE_CLASSES_KEY);
        listOfPossibleFilters.add(EXCLUDE_DEPENDENCY_CLASSES_KEY);
        listOfPossibleFilters.add(EXCLUDE_QUERY_KEY);
        listOfPossibleFilters.add(EXCLUDE_DEPENDENCY_QUERY_KEY);
        listOfPossibleFilters.add(FORCE_PUSH_KEY);

        if(!getFilters().keySet().stream().allMatch(element -> listOfPossibleFilters.contains(element))){
            errors.add("The field `Filters` has a property that is not expected. Possible Properties: " + listOfPossibleFilters.toString());
        }

        if(getFilters().containsKey(DEPENDENCIES_KEY)) {
            try {
                Boolean.class.cast(getFilters()
                        .get(FilterDescriptor.DEPENDENCIES_KEY));
            } catch (final ClassCastException e){
                errors.add("The value of the field `dependencies` cannot be cast to Boolean");
            }
        }

        if(getFilters().containsKey(RELATIONSHIPS_KEY)) {
            try {
                Boolean.class.cast(getFilters()
                        .get(FilterDescriptor.RELATIONSHIPS_KEY));
            } catch (final ClassCastException e){
                errors.add("The value of the field `relationships` cannot be cast to Boolean");
            }
        }

        if(getFilters().containsKey(FORCE_PUSH_KEY)) {
            try {
                Boolean.class.cast(getFilters()
                        .get(FilterDescriptor.FORCE_PUSH_KEY));
            } catch (final ClassCastException e){
                errors.add("The value of the field `forcePush` cannot be cast to Boolean");
            }
        }

        if(!errors.isEmpty()){
            throw new DotDataValidationException(errors.size() + " error(s): " + String.join(" , ", errors));
        }
    }

    @Override
    public String toString() {
        return "FilterDescriptor{" +
                "key=" + this.key +
                ", title=" + this.title +
                ", sort=" + this.sort +
                ", default=" + this.defaultFilter +
                ", roles=" + this.roles +
                ", filters{" + this.filters + "}" +
                '}';
    }

    @Override
    public int compareTo(@NotNull final FilterDescriptor o) {
        final int currentDescriptor = Integer.parseInt(this.getSort());
        final int incomingDescriptor = Integer.parseInt(o.getSort());
        // Order PP Filters based on their sort parameter. If it is the same, order them based on their title in
        // ascending order.
        return (currentDescriptor == incomingDescriptor) ? this.title.compareTo(o.getTitle()) :
                       currentDescriptor - incomingDescriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FilterDescriptor that = (FilterDescriptor) o;
        return this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key);
    }

}
