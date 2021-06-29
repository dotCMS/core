package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * This task takes care of changing the name of {@code "id"} column of the
 * {@code notification} table to {@code "group_id"}. Additionally, the table
 * constraints - primary key, default constraint, and index - are being declared
 * explicitly.
 * 
 * @author Jonathan Gamba
 * @version 3.7
 * @since Dec 2, 2016
 *
 */
public class Task03725NewNotificationTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "  group_id           VARCHAR(36)  NOT NULL,\n" +
                "  user_id            VARCHAR(255) NOT NULL,\n" +
                "  message            TEXT         NOT NULL,\n" +
                "  notification_type  VARCHAR(100),\n" +
                "  notification_level VARCHAR(100),\n" +
                "  time_sent          TIMESTAMP    NOT NULL,\n" +
                "  was_read           BOOL\n" +
                ");\n" +
                "ALTER TABLE notification ADD CONSTRAINT pk_notification PRIMARY KEY (group_id, user_id);\n" + 
                "ALTER TABLE notification ALTER was_read SET DEFAULT FALSE;\n" + 
                "CREATE INDEX idx_not_read ON notification (was_read);";
    }

    @Override
    public String getMySQLScript() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "  group_id           VARCHAR(36)  NOT NULL,\n" +
                "  user_id            VARCHAR(255) NOT NULL,\n" +
                "  message            TEXT         NOT NULL,\n" +
                "  notification_type  VARCHAR(100),\n" +
                "  notification_level VARCHAR(100),\n" +
                "  time_sent          DATETIME     NOT NULL,\n" +
                "  was_read           BIT\n" +
                ");\n" +
                "ALTER TABLE notification ADD CONSTRAINT pk_notification PRIMARY KEY (group_id, user_id);\n" + 
                "ALTER TABLE notification MODIFY was_read BIT DEFAULT 0;\n" + 
                "CREATE INDEX idx_not_read ON notification (was_read);";
    }

    @Override
    public String getOracleScript() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "  group_id           VARCHAR2(36)  NOT NULL,\n" +
                "  user_id            VARCHAR2(255) NOT NULL,\n" +
                "  message            NCLOB         NOT NULL,\n" +
                "  notification_type  VARCHAR2(100),\n" +
                "  notification_level VARCHAR2(100),\n" +
                "  time_sent          TIMESTAMP     NOT NULL,\n" +
                "  was_read           NUMBER(1, 0)\n" +
                ");\n" +
                "ALTER TABLE notification ADD CONSTRAINT pk_notification PRIMARY KEY (group_id, user_id);\n" + 
                "ALTER TABLE notification MODIFY was_read DEFAULT 0;\n" +
                "CREATE INDEX idx_not_read ON notification (was_read);";
    }

    @Override
    public String getMSSQLScript() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "    group_id           NVARCHAR(36)  NOT NULL,\n" +
                "    user_id            NVARCHAR(255) NOT NULL,\n" +
                "    message            NVARCHAR(MAX) NOT NULL,\n" +
                "    notification_type  NVARCHAR(100),\n" +
                "    notification_level NVARCHAR(100),\n" +
                "    time_sent          DATETIME      NOT NULL,\n" +
                "    was_read           TINYINT\n" +
                "  );\n" +
                "ALTER TABLE notification ADD CONSTRAINT pk_notification PRIMARY KEY (group_id, user_id);\n" +
                "ALTER TABLE notification ADD CONSTRAINT df_notification_was_read DEFAULT ((0)) FOR was_read;\n" +
                "CREATE INDEX idx_not_read ON notification (was_read);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
