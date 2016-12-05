package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         12/2/16
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
                "  was_read           BOOL DEFAULT FALSE,\n" +
                "  PRIMARY KEY (group_id, user_id)\n" +
                ");\n" +
                "create index idx_not_read ON notification (was_read);";
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
                "  was_read           BIT DEFAULT 0,\n" +
                "  PRIMARY KEY (group_id, user_id)\n" +
                ");\n" +
                "create index idx_not_read ON notification (was_read);";
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
                "  was_read           NUMBER(1, 0) DEFAULT 0,\n" +
                "  PRIMARY KEY (group_id, user_id)\n" +
                ");\n" +
                "create index idx_not_read ON notification (was_read);";
    }

    @Override
    public String getMSSQLScript() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "    group_id           VARCHAR(36)  NOT NULL,\n" +
                "    user_id            VARCHAR(255) NOT NULL,\n" +
                "    message            TEXT         NOT NULL,\n" +
                "    notification_type  VARCHAR(100),\n" +
                "    notification_level VARCHAR(100),\n" +
                "    time_sent          DATETIME     NOT NULL,\n" +
                "    was_read           TINYINT DEFAULT 0,\n" +
                "    PRIMARY KEY (group_id, user_id)\n" +
                "  );\n" +
                "create index idx_not_read ON notification (was_read);";
    }

    @Override
    public String getH2Script() {
        return "DROP TABLE notification;\n" +
                "CREATE TABLE notification (\n" +
                "  group_id           VARCHAR(36)  NOT NULL,\n" +
                "  user_id            VARCHAR(255) NOT NULL,\n" +
                "  message            TEXT         NOT NULL,\n" +
                "  notification_type  VARCHAR(100),\n" +
                "  notification_level VARCHAR(100),\n" +
                "  time_sent          TIMESTAMP    NOT NULL,\n" +
                "  was_read           BIT DEFAULT 0,\n" +
                "  PRIMARY KEY (group_id, user_id)\n" +
                ");\n" +
                "create index idx_not_read ON notification (was_read);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        // TODO Auto-generated method stub
        return null;
    }

}
