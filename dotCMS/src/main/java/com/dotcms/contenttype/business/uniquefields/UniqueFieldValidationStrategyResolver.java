package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.DBUniqueFieldValidationStrategy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Utility class responsible for returning the appropriate {@link UniqueFieldValidationStrategy}
 * based on the configuration setting of ENABLED_UNIQUE_FIELDS_DATABASE_VALIDATION. If this setting is true,
 * an {@link DBUniqueFieldValidationStrategy} is returned; otherwise,
 * an {@link ESUniqueFieldValidationStrategy} is used.
 *
 */
@ApplicationScoped
public class UniqueFieldValidationStrategyResolver {

    private final ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy;
    private final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy;
    @Inject
    public  UniqueFieldValidationStrategyResolver(final ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy,
                                                 final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy){
        this.esUniqueFieldValidationStrategy = esUniqueFieldValidationStrategy;
        this.extraTableUniqueFieldValidationStrategy = extraTableUniqueFieldValidationStrategy;
    }

    public UniqueFieldValidationStrategy get() {
        return ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation() ?
                extraTableUniqueFieldValidationStrategy : esUniqueFieldValidationStrategy;
    }
}
