package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.ExtraTableUniqueFieldValidationStrategy;

public enum UniqueFieldValidationStrategyResolver {

    INSTANCE;

    public UniqueFieldValidationStrategy get() {
        return ESContentletAPIImpl.getEnabledUniqueFieldsDataBaseValidation() ?
                new ExtraTableUniqueFieldValidationStrategy()  : new ESUniqueFieldValidationStrategy();
    }
}
