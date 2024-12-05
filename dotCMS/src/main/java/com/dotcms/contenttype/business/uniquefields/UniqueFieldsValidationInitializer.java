package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.sql.SQLException;

@Dependent
public class UniqueFieldsValidationInitializer  implements DotInitializer {

    private UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil;
    private DotDatabaseMetaData dotDatabaseMetaData;

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
                this.uniqueFieldDataBaseUtil.createTableAnsPopulate();
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
