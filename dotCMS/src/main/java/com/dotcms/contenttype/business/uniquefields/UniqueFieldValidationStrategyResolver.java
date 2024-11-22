package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.DBUniqueFieldValidationStrategy;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This Strategy Resolver responsible for returning the appropriate mechanism for validating unique
 * fields in dotCMS. There are currently two unique field validation strategies:
 * <ul>
 *     <li>The {@link ESUniqueFieldValidationStrategy}, which uses Elasticsearch to detect
 *     duplicate field values. This has been the only strategy we had, so far.</li>
 *     <li>The more recent {@link DBUniqueFieldValidationStrategy}, which uses a database query
 *     to detect duplicate field values.</li>
 * </ul>
 * The {@code ENABLED_UNIQUE_FIELDS_DATABASE_VALIDATION} configuration parameter allows you to
 * enable each strategy. If set
 * to {@code true}, the {@link DBUniqueFieldValidationStrategy} will be used. Otherwise, the
 * {@link ESUniqueFieldValidationStrategy}
 * will be used.
 * <p>The main goal of the database strategy is to solve certain situations in which the
 * Elasticsearch Index was not
 * fully refreshed. This was causing duplicate unique values to NOT be detected correctly.</p>
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
@Dependent
public class UniqueFieldValidationStrategyResolver {

    private final ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy;

    private final DBUniqueFieldValidationStrategy dbUniqueFieldValidationStrategy;

    @Inject
    public  UniqueFieldValidationStrategyResolver(final ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy,
                                                 final DBUniqueFieldValidationStrategy dbUniqueFieldValidationStrategy){
        this.esUniqueFieldValidationStrategy = esUniqueFieldValidationStrategy;
        this.dbUniqueFieldValidationStrategy = dbUniqueFieldValidationStrategy;
    }

    /**
     * Returns the appropriate unique field validation strategy based on the value of the
     * {@code ENABLED_UNIQUE_FIELDS_DATABASE_VALIDATION} configuration parameter.
     *
     * @return The {@link UniqueFieldValidationStrategy} to use.
     */
    public UniqueFieldValidationStrategy get() {
        return ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation() ?
                dbUniqueFieldValidationStrategy : esUniqueFieldValidationStrategy;
    }

}
