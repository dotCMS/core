package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220402UpdateDateTimezonesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (\n"
            + "   id NUMERIC(19,0) NOT NULL,\n"
            + "   language_code NVARCHAR(5) NULL,\n"
            + "   country_code NVARCHAR(255) NULL,\n"
            + "   language NVARCHAR(255) NULL,\n"
            + "   country NVARCHAR(255) NULL,\n"
            + "   add_date datetime,\n"
            + "   mod_date datetime NOT NULL DEFAULT GETDATE(),\n"
            + "   PRIMARY KEY (id), \n"
            + "   UNIQUE (language_code,country_code) \n"
            + ")\n";


    final String CREATE_INDEX = "create index idx_lol on %s (add_date,country_code)";

    final String INSERT = "INSERT %s\n"
            + "(id, language_code, country_code, [language], country, add_date)\n"
            + "VALUES(%d, 'ES', 'CR', 'Spanish', 'Costa Rica', GetDate())";

    final String DROP_TABLE = "drop table %s";

    /**
     * <b>Method to Test:</b> {@link Task220402UpdateDateTimezones#executeUpgrade()} <p>
     * <b>Given Scenario:</b> When ms-sql is used, the upgrade task should be executed <p>
     * <b>Expected Result:</b> When using ms-sql, dates should be declared as timestamps with
     * timezone
     */
    @Test
    public void Test_Upgrade_Task() throws Exception {

        final String tableName = "a1_"+System.currentTimeMillis();

        final Task220402UpdateDateTimezones task = new Task220402UpdateDateTimezones(
           ImmutableMap.of(tableName,
                ImmutableMap.of(
                        "idx_lol", ImmutableList.of(String.format(CREATE_INDEX,tableName)),
                        "uq",ImmutableList.of(String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (language_code,country_code) ", tableName, "UQ_"+tableName+"_"+System.currentTimeMillis())),
                        "pk",ImmutableList.of(String.format("ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY CLUSTERED (id)",tableName, "PK_"+tableName+"_"+System.currentTimeMillis())),
                        "df",ImmutableList.of(String.format("ALTER TABLE %s ADD CONSTRAINT DF_%s_%d DEFAULT getDate() FOR mod_date ", tableName, tableName, System.currentTimeMillis()))
                )
           )
        );
        if (task.forceRun()) {

            try {
                DbConnectionFactory.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }

            final DotConnect dotConnect = new DotConnect();

          try {
              //Create the table and index
              dotConnect.executeStatement(String.format(CREATE_TABLE_TEMPLATE, tableName));
              dotConnect.executeStatement(String.format(CREATE_INDEX, tableName));
              //Now Lets try a regular insert
              Assert.assertTrue(
                      Try.of(() -> {
                                  dotConnect.executeStatement(String.format(INSERT, tableName, 1));
                                  return true;
                              })
                              .getOrElse(false));
               //This insert is expected to break uniqueness
              Assert.assertFalse(
                      Try.of(() -> {
                                  dotConnect.executeStatement(String.format(INSERT, tableName, 2));
                                  return true;
                              })
                              .getOrElse(false));
              //Run the upgrade
              task.executeUpgrade();
              assertTrue(task.getTablesCount() >= 1);
              //Still expected to fail this insert. If it doesn't fail the table structure is messed up
              Assert.assertFalse(
                      Try.of(() -> {
                                  dotConnect.executeStatement(String.format(INSERT, tableName, 2));
                                  return true;
                              })
                              .getOrElse(false));

          }finally {
              dotConnect.executeStatement(String.format(DROP_TABLE, tableName));
          }
        }
    }

}
