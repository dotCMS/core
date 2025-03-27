package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.sql.SQLException;

/**
 * Initializer in charge of check when dotCMS start up if the Unique Fields Data Base validation was enabled
 * to create and populate the unique_fields table.
 *
 * It check if the table already exists and:
 * - If it exists and the Database validation is disabled then drop the table.
 * - If it does not exist and the Database validation is enabled then it created and populate it.
 * - If it exists and the Database validation is enabled do nothing.
 * - If it does not exist  and the Database validation is disabled do nothing.
 */
@Dependent
public class UniqueFieldsValidationInitializer  implements DotInitializer {

    private final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil;
    private final DotDatabaseMetaData dotDatabaseMetaData;

    @Inject
    public UniqueFieldsValidationInitializer(final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil){
        this.uniqueFieldDataBaseUtil = uniqueFieldDataBaseUtil;
        this.dotDatabaseMetaData = new DotDatabaseMetaData();
    }

    @Override
    public void init() {
        final boolean featureFlagDbUniqueFieldValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();
        boolean uniqueFieldsTableExists = uniqueFieldsTableExists();

        try {
            if (featureFlagDbUniqueFieldValidation && !uniqueFieldsTableExists) {
                this.uniqueFieldDataBaseUtil.createTableAndPopulate();
            } else if (!featureFlagDbUniqueFieldValidation && uniqueFieldsTableExists) {
                this.uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();
            }
        } catch (DotDataException e) {
            Logger.error(UniqueFieldsValidationInitializer.class, e);
        }
    }

    private boolean uniqueFieldsTableExists(){
        try {
            return dotDatabaseMetaData.tableExists(DbConnectionFactory.getConnection(), "unique_fields");
        } catch (SQLException e) {
            return false;
        }
    }
}
