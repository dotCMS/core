package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * When dotCMS starts up, this Initializer is in charge of checking if the Unique Fields Data Base
 * validation was enabled to create and populate the unique_fields table. It checks if the table
 * already exists, and:
 * <ul>
 *     <li>If it exists and the Database validation is disabled, then drop the table.</li>
 *     <li>If it does not exist and the Database validation is enabled, then it creates it and
 *     populates it.</li>
 *     <li>If it exists and the Database validation is enabled, do nothing.</li>
 *     <li>If it does not exist and the Database validation is disabled, do nothing.</li>
 *     <li>If any error occurred, drop the table nd fail to start.</li>
 * </ul>
 *
 * @author Freddy Rodriguez
 * @since Dec 6th, 2024
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
        final boolean uniqueFieldsTableExists = uniqueFieldsTableExists();
        try {
            if (featureFlagDbUniqueFieldValidation && !uniqueFieldsTableExists) {
                Logger.info(this, "Creating and populating the Unique Fields table");
                this.uniqueFieldDataBaseUtil.createTableAndPopulate();
            } else if (!featureFlagDbUniqueFieldValidation && uniqueFieldsTableExists) {
                Logger.info(this, "Dropping the Unique Fields table as the validation via database has been disabled");
                this.uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();
            }
        } catch (final DotRuntimeException | DotDataException e) {
            Logger.warnAndDebug(this.getClass(), ExceptionUtil.getErrorMessage(e), e);
            try {
                Logger.warn(this.getClass(), "Dropping 'unique_fields' table so that the process can run again the next time");
                this.uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();
            } catch (final DotDataException ex) {
                Logger.error(this.getClass(), "Failed to drop the 'unique_fields' table. Please drop it manually before restarting dotCMS");
            }
            throw new DotRuntimeException(ExceptionUtil.getErrorMessage(e), e);
        }
    }

    /**
     * Checks if the {@code unique_fields} table exists in the database.
     *
     * @return If the table exists, returns {@code true}; otherwise, returns {@code false}.
     */
    private boolean uniqueFieldsTableExists(){
        try (final Connection conn = DbConnectionFactory.getConnection()) {
            return dotDatabaseMetaData.tableExists(conn, "unique_fields");
        } catch (final SQLException e) {
            return false;
        }
    }

}
