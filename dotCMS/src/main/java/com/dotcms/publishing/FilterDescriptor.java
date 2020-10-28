package com.dotcms.publishing;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotmarketing.exception.DotDataValidationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This bean to read data from the yml file that stores the Push Publishing Filters
 * The Key will be set from the fileName.
 * The file might look like this:
 * title: (arbitrary title for the exclude filter)
 * defaultFilter: true|false
 * roles: [ list of roles that can access the filter]
 * filters:
 * 	excludeDependencyQuery:   ( lucene query that dependent content is checked against to exclude)
 * 	excludeQuery:             (lucene query that added content is checked against to exclude)
 * 	excludeDependencyClasses: [ list of classes that should never be pushed as dependencies]
 * 	excludeClasses:           [ list of classes that should never be pushed]
 * 	forcePush:              true|false . (basically, force push - should the push history be consulted when pushing?  Defaults to false)
 * 	dependencies:             true|false, defaults to true
 * 	relationships:            true|false, defaults to true
 */

public class FilterDescriptor {

    private String key;
    private final String title;
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
            } catch (ClassCastException e){
            errors.add("The value of the field `dependencies` cannot be cast to Boolean");
            }
        }

        if(getFilters().containsKey(RELATIONSHIPS_KEY)) {
            try {
                Boolean.class.cast(getFilters()
                        .get(FilterDescriptor.RELATIONSHIPS_KEY));
            } catch (ClassCastException e){
                errors.add("The value of the field `relationships` cannot be cast to Boolean");
            }
        }

        if(getFilters().containsKey(FORCE_PUSH_KEY)) {
            try {
                Boolean.class.cast(getFilters()
                        .get(FilterDescriptor.FORCE_PUSH_KEY));
            } catch (ClassCastException e){
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
                ", default=" + this.defaultFilter +
                ", roles=" + this.roles +
                ", filters{" + this.filters + "}" +
                '}';
    }
}
