package com.dotcms.contenttype.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;

import java.util.HashMap;
import java.util.Map;

/**
 * This Helper allow you to interact with the unique_fields table
 */
public class UniqueFieldAPIHelper {

    private final UniqueFieldFactory uniqueFieldFactory;

    public UniqueFieldAPIHelper(){
        this(FactoryLocator.getUniqueFieldFactory());
    }

    public UniqueFieldAPIHelper(final UniqueFieldFactory uniqueFieldFactory) {
        this.uniqueFieldFactory = uniqueFieldFactory;
    }


    /**
     * Insert a new unique field value, if the value is duplicated then a {@link java.sql.SQLException} is thrown.
     *
     * @param uniqueFieldCriteria
     * @param contentletId
     *
     * @throws UniqueFieldValueDupliacatedException when the Value is duplicated
     * @throws DotDataException when a DotDataException is throws
     */
    public void insert(final UniqueFieldCriteria uniqueFieldCriteria, final String contentletId)
            throws UniqueFieldValueDupliacatedException, DotDataException {

        if (!uniqueFieldCriteria.field().unique()) {
            throw new IllegalArgumentException(String.format("The Field %s is not unique", uniqueFieldCriteria.field().variable()));
        }

        final boolean uniqueForSite = uniqueFieldCriteria.field().fieldVariableValue(ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);

        final Map<String, Object> supportingValues =  new HashMap<>(uniqueFieldCriteria.toMap());
        supportingValues.put("contentletsId", CollectionsUtils.list(contentletId));
        supportingValues.put("uniquePerSite", uniqueForSite);

        try {
            uniqueFieldFactory.insert(uniqueFieldCriteria.hash(), supportingValues);
        } catch (DotDataException e) {

            if (isDuplicatedKeyError(e)) {
                final String duplicatedValueMessage = String.format("The value %s for the field %s in the Content type %s is duplicated",
                        uniqueFieldCriteria.value(), uniqueFieldCriteria.field().variable(),
                        uniqueFieldCriteria.contentType().variable());

                throw new UniqueFieldValueDupliacatedException(duplicatedValueMessage);
            }
        }
    }

    private static boolean isDuplicatedKeyError(final Exception exeption) {
        final String originalMessage = exeption.getMessage();

        return originalMessage != null && originalMessage.startsWith(
                "ERROR: duplicate key value violates unique constraint \"unique_fields_pkey\"");
    }
}
