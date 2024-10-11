package com.dotcms.contenttype.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

/**
 * Default implementation of {@link UniqueFieldFactory}
 */
public class UniqueFieldFactoryImpl implements UniqueFieldFactory {

    private final static String INSERT_SQL = "INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (?, ?)";
    /**
     * Default implementation of {@link UniqueFieldFactory#insert(String, Map)}
     *
     * @param key
     * @param supportingValues
     */
    @Override
    public void insert(final String key, final Map<String, Object> supportingValues) throws DotDataException {
        new DotConnect().setSQL(INSERT_SQL).addParam(key).addJSONParam(supportingValues).loadObjectResults();
    }
}
