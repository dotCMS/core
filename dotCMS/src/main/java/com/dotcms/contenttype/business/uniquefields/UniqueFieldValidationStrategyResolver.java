package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.DBUniqueFieldValidationStrategy;
import com.dotmarketing.exception.DotRuntimeException;
import com.google.common.annotations.VisibleForTesting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Utility class responsible for returning the appropriate {@link UniqueFieldValidationStrategy}
 * based on the configuration setting of ENABLED_UNIQUE_FIELDS_DATABASE_VALIDATION. If this setting is true,
 * an {@link DBUniqueFieldValidationStrategy} is returned; otherwise,
 * an {@link ESUniqueFieldValidationStrategy} is used.
 *
 */
@ApplicationScoped
public class UniqueFieldValidationStrategyResolver {

    @Inject
    private ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy;
    @Inject
    private  DBUniqueFieldValidationStrategy dbUniqueFieldValidationStrategy;

    public UniqueFieldValidationStrategyResolver(){}

    @VisibleForTesting
    public  UniqueFieldValidationStrategyResolver(final ESUniqueFieldValidationStrategy esUniqueFieldValidationStrategy,
                                                 final DBUniqueFieldValidationStrategy dbUniqueFieldValidationStrategy){
        this.esUniqueFieldValidationStrategy = esUniqueFieldValidationStrategy;
        this.dbUniqueFieldValidationStrategy = dbUniqueFieldValidationStrategy;

    }

    public UniqueFieldValidationStrategy get() {
        return ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation() ?
                dbUniqueFieldValidationStrategy : esUniqueFieldValidationStrategy;
    }
}
