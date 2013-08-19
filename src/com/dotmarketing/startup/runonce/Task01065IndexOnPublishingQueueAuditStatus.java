package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01065IndexOnPublishingQueueAuditStatus extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status)";
    }

    @Override
    public String getMySQLScript() {
        return "CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status)";
    }

    @Override
    public String getOracleScript() {
        return "CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status)";
    }

    @Override
    public String getMSSQLScript() {
        return "CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status)";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
