package com.dotmarketing.db.commands;

import com.dotcms.system.SimpleMapAppContext;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.liferay.util.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Generic UpsertCommand that can be used to Generate and execute Native SQL Upsert Queries
 * @author Andre Curione
 */
public abstract class UpsertCommand implements DatabaseCommand {

    /**
     * Method to execute The Upsert Query... This method works for most DBTypes,
     * and its overriden for DBTypes with particularities
     * @param dotConnect Connection
     * @param queryReplacements key,values to be added to the final upsert query
     * @param parameters or values
     * @throws DotDataException
     */
    public void execute(DotConnect dotConnect, SimpleMapAppContext queryReplacements, Object... parameters)
            throws DotDataException {
        String query = generateSQLQuery(queryReplacements);
        DotConnect dc = (dotConnect != null) ? dotConnect : new DotConnect();
        ArrayList<Object> params =  new ArrayList<>();
        Collections.addAll(params, parameters); //Insert parameters
        if (!queryReplacements.doNothingOnConflict()) {
            Collections.addAll(params, parameters); //Update parameters
        }
        dc.executeUpdate(query, params.toArray());
    }

    // Misc Methods:
    /**
     * Generates the Update statement query part, with the format:
     * column = ?, column = ?, etc.
     */
    protected String getUpdateColumnValuePairs(SimpleMapAppContext replacements) {
        StringBuilder builder = new StringBuilder();
        if (replacements.getAttribute(QueryReplacements.ID_COLUMN) != null) {
            builder.append(replacements.getAttribute(QueryReplacements.ID_COLUMN).toString());
            builder.append("=");

            //keeps the same value in db
            if (replacements.doNothingOnConflict()){
                builder.append(replacements.getAttribute(QueryReplacements.ID_COLUMN).toString());
            } else{
                builder.append(replacements.getAttribute(QueryReplacements.ID_VALUE).toString());
            }

            builder.append(",");
        }
        if (replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN) != null
                && !(this instanceof OracleUpsertCommand)) { //Oracle does not allow to Update the Conditional Column
            builder.append(replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN).toString());

            //keeps the same value in db
            if (replacements.doNothingOnConflict()) {
                builder.append("=")
                        .append(replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN)
                                        .toString()).append(",");
            } else {
                builder.append("=?,");
            }
        }
        if (replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS) != null) {
            String[] extraColumns = replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS);
            for (String column : extraColumns) {
                builder.append(column);

                //keeps the same value in db
                if (replacements.doNothingOnConflict()) {
                    builder.append("=").append(column).append(",");
                } else {
                    builder.append("=?,");
                }
            }
        }
        String update = builder.toString();
        if (update.endsWith(",")) {
            update = update.substring(0, update.length() - 1);
        }
        return update;
    }

    /**
     * Generates the Insert Columns string, in comma separated format
     * @param replacements
     * @return
     */
    protected String getInsertColumnsString (SimpleMapAppContext replacements) {
        ArrayList<String> columns = new ArrayList<>();
        if (replacements.getAttribute(QueryReplacements.ID_COLUMN) != null) {
            columns.add(replacements.getAttribute(QueryReplacements.ID_COLUMN));
        }
        if (replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN) != null) {
            columns.add(replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN));
        }
        if (replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS) != null) {
            columns.addAll(Arrays.asList(replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS)));
        }
        return StringUtil.merge(columns.toArray(new String[0]));
    }

    /**
     * Generates the Insert Values parameter string, in comma separated format
     * @param replacements
     * @return
     */
    protected String getInsertValuesString(SimpleMapAppContext replacements) {
        ArrayList<String> values = new ArrayList<>();
        if (replacements.getAttribute(QueryReplacements.ID_COLUMN) != null) {
            values.add(replacements.getAttribute(QueryReplacements.ID_VALUE));
        }
        if (replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN) != null) {
            values.add(SQLUtil.PARAMETER);
        }
        if (replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS) != null) {
            String[] extraColumns = replacements.getAttribute(QueryReplacements.EXTRA_COLUMNS);
            for (int i=0; i<extraColumns.length; i++) {
                values.add(SQLUtil.PARAMETER);
            }
        }
        return StringUtil.merge(values.toArray(new String[0]));
    }

}

final class PostgreUpsertCommand extends UpsertCommand {

    /**
     * Postgres Upsert Example:
     * INSERT INTO table (columns) VALUES (values)
     * ON CONFLICT (conditionalColumn) DO UPDATE SET column1=value1, column2=value2, etc...
     */

    private static final float POSTGRES_UPSERT_MINIMUM_VERSION = 9.5F;

    private static final String POSTGRES_UPSERT_QUERY =
        "INSERT INTO %s (%s) "
        + "VALUES (%s) ON CONFLICT (%s) "
        + "DO UPDATE SET %s";

    private static final String POSTGRES_UPSERT_QUERY_DO_NOTHING =
            "INSERT INTO %s (%s) "
                    + "VALUES (%s) ON CONFLICT (%s) "
                    + "DO NOTHING %s";

    private static final String POSTGRES_INSERT_QUERY =
            "INSERT INTO %s (%s) VALUES (%s)";


    private static final String POSTGRES_UPDATE_QUERY =
            "UPDATE %s SET %s WHERE %s='%s'";

    @Override
    public String generateSQLQuery(SimpleMapAppContext replacements) {

        return
            String.format(replacements.doNothingOnConflict()?
                            POSTGRES_UPSERT_QUERY_DO_NOTHING: POSTGRES_UPSERT_QUERY,
                replacements.getAttribute(QueryReplacements.TABLE),
                getInsertColumnsString(replacements),
                getInsertValuesString(replacements),
                replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                replacements.doNothingOnConflict()? "": getUpdateColumnValuePairs(replacements)
            );
    }

    //If PostgreSQL gets upgraded to 9.5+ (to Support ON CONFLICT) , this override can be removed and let it use the super method execute()
    @Override
    public void execute(DotConnect dotConnect, SimpleMapAppContext queryReplacements,
            Object... parameters) throws DotDataException {
        DotConnect dc = (dotConnect != null) ? dotConnect : new DotConnect();

        float version = DbConnectionFactory.getDbFullVersion();
        if (version >= POSTGRES_UPSERT_MINIMUM_VERSION) {
            this.executeUpsert(dc, queryReplacements, parameters);
        } else {
            this.executeUpdateInsert(dc, queryReplacements, parameters);
        }
    }

    private void executeUpsert(DotConnect dotConnect, SimpleMapAppContext queryReplacements,
            Object... parameters) throws DotDataException {
        super.execute(dotConnect, queryReplacements, parameters);
    }

    private void executeUpdateInsert(DotConnect dotConnect, SimpleMapAppContext queryReplacements,
            Object... parameters) throws DotDataException {
        try {
            //In Postgre 9.4- Upsert Statement is not supported. Attempt to Insert first.
            String insertQuery =
                    String.format(
                            POSTGRES_INSERT_QUERY,
                            queryReplacements.getAttribute(QueryReplacements.TABLE),
                            getInsertColumnsString(queryReplacements),
                            getInsertValuesString(queryReplacements)
                    );
            dotConnect.executeUpdate(insertQuery, false, parameters);

        } catch (DotDataException ex) {
            if (SQLUtil.isUniqueConstraintException(ex) && !queryReplacements.doNothingOnConflict()) {
                //On Unique Constraint exception, attempt to update:
                DbConnectionFactory.closeAndCommit();
                String updateQuery =
                    String.format(
                        POSTGRES_UPDATE_QUERY,
                        queryReplacements.getAttribute(QueryReplacements.TABLE),
                        getUpdateColumnValuePairs(queryReplacements),
                        queryReplacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                        queryReplacements.getAttribute(QueryReplacements.CONDITIONAL_VALUE)
                    );
                dotConnect.executeUpdate(updateQuery, parameters);
            } else {
                throw ex;
            }
        }
    }
}

final class MySQLUpsertCommand extends UpsertCommand {

    /**
     * MySQL Upsert Example:
     * INSERT INTO table (columns) VALUES (values)
     * ON DUPLICATE KEY UPDATE column1=value1, column2=value2, etc...
     */
    private static final String MYSQL_UPSERT_QUERY =
        "INSERT INTO %s (%s) "
        + "VALUES (%s) ON DUPLICATE KEY "
        + "UPDATE %s";


    @Override
    public String generateSQLQuery(SimpleMapAppContext replacements) {
        return
            String.format(
                MYSQL_UPSERT_QUERY,
                replacements.getAttribute(QueryReplacements.TABLE),
                getInsertColumnsString(replacements),
                getInsertValuesString(replacements),
                getUpdateColumnValuePairs(replacements)
            );
    }
}

final class MSSQLUpsertCommand extends UpsertCommand {

    /**
     * MSSQL Server Upsert Example:
     * MERGE table WITH (HOLDLOCK) AS [Target] USING
     * (SELECT 'conditionalValue' AS conditionalColumn) AS [Source] ON [Target].conditionalColumn = [Source].conditionalColumn
     * WHEN MATCHED THEN UPDATE SET column1=value1, column2=value2, etc...
     * WHEN NOT MATCHED THEN INSERT (columns) VALUES (values);
     */
    private static final String MSSQL_UPSERT_QUERY =
        "MERGE %s WITH (HOLDLOCK) AS [Target] USING "
        + "(SELECT '%s' AS %s) AS [Source] ON [Target].%s = [Source].%s "
        + "WHEN MATCHED THEN "
        + "  UPDATE SET %s "
        + "WHEN NOT MATCHED THEN "
        + "  INSERT (%s) "
        + "  VALUES (%s);";

    private static final String MSSQL_UPSERT_QUERY_DO_NOTHING_ON_CONFLICT =
            "MERGE %s WITH (HOLDLOCK) AS [Target] USING "
                    + "(SELECT '%s' AS %s) AS [Source] ON [Target].%s = [Source].%s "
                    + "WHEN NOT MATCHED THEN "
                    + "  INSERT (%s) "
                    + "  VALUES (%s);";

    @Override
    public String generateSQLQuery(SimpleMapAppContext replacements) {
        if (replacements.doNothingOnConflict()){
            return
                    String.format(MSSQL_UPSERT_QUERY_DO_NOTHING_ON_CONFLICT,
                            replacements.getAttribute(QueryReplacements.TABLE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_VALUE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            getInsertColumnsString(replacements),
                            getInsertValuesString(replacements)
                    );
        } else{
            return
                    String.format(MSSQL_UPSERT_QUERY,
                            replacements.getAttribute(QueryReplacements.TABLE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_VALUE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            getUpdateColumnValuePairs(replacements),
                            getInsertColumnsString(replacements),
                            getInsertValuesString(replacements)
                    );
        }
    }
}

final class OracleUpsertCommand extends UpsertCommand {

    /**
     * Oracle Upsert Example:
     * INSERT INTO table Target USING
     * (SELECT 'conditionalValue' conditionalColumn FROM dual) Source ON (Target.conditionalColumn = Source.conditionalColumn)
     * WHEN MATCHED THEN UPDATE SET column1=value1, column2=value2, etc...
     * WHEN NOT MATCHED THEN INSERT (columns) VALUES (values)
     */
    private static final String ORACLE_UPSERT_QUERY =
        "MERGE INTO %s Target USING "
        + "(SELECT '%s' %s FROM DUAL) Source ON (Target.%s = Source.%s) "
        + "WHEN MATCHED THEN "
        + "  UPDATE SET %s "
        + "WHEN NOT MATCHED THEN "
        + "  INSERT (%s) "
        + "  VALUES (%s)";

    private static final String ORACLE_UPSERT_QUERY_DO_NOTHING_ON_CONFLICT =
            "MERGE INTO %s Target USING "
                    + "(SELECT '%s' %s FROM DUAL) Source ON (Target.%s = Source.%s) "
                    + "WHEN NOT MATCHED THEN "
                    + "  INSERT (%s) "
                    + "  VALUES (%s)";

    @Override
    public String generateSQLQuery(SimpleMapAppContext replacements) {
        if (replacements.doNothingOnConflict()){
            return
                    String.format(ORACLE_UPSERT_QUERY_DO_NOTHING_ON_CONFLICT,
                            replacements.getAttribute(QueryReplacements.TABLE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_VALUE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            getInsertColumnsString(replacements),
                            getInsertValuesString(replacements)
                    );
        }else{
            return
                    String.format(ORACLE_UPSERT_QUERY,
                            replacements.getAttribute(QueryReplacements.TABLE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_VALUE),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            replacements.getAttribute(QueryReplacements.CONDITIONAL_COLUMN),
                            getUpdateColumnValuePairs(replacements),
                            getInsertColumnsString(replacements),
                            getInsertValuesString(replacements)
                    );
        }
    }

    @Override
    public void execute(DotConnect dotConnect, SimpleMapAppContext queryReplacements, Object... parameters)
            throws DotDataException {

        String query = generateSQLQuery(queryReplacements);
        DotConnect dc = (dotConnect != null) ? dotConnect : new DotConnect();
        ArrayList<Object> params =  new ArrayList<>();

        //Update parameters, skip the Conditional parameter (assumed first) because Oracle cannot Update the conditional column
        if (!queryReplacements.doNothingOnConflict()) {
            for (int i = 1; i < parameters.length; i++) {
                params.add(parameters[i]);
            }
        }
        //Insert parameters
        Collections.addAll(params, parameters);

        try {
            //In Oracle the Upsert (Merge) is not thread safe. Attempt to insert first:
            dc.executeUpdate(query, false, params.toArray());

        } catch (DotDataException ex) {
            if (SQLUtil.isUniqueConstraintException(ex) && !queryReplacements.doNothingOnConflict()) {
                //On Unique constraint exception, attempt again... to update:
                dc.executeUpdate(query, params.toArray());
            } else {
                throw ex;
            }
        }
    }
}