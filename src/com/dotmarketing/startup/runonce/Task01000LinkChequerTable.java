package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01000LinkChequerTable extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return "CREATE TABLE broken_link (\n"+
                "   id VARCHAR(36) PRIMARY KEY,\n"+
                "   inode VARCHAR(36) NOT NULL, \n"+
                "   field VARCHAR(36) NOT NULL,\n"+
                "   link VARCHAR(255) NOT NULL,\n"+
                "   title VARCHAR(255) NOT NULL,\n"+
                "   status_code integer NOT NULL\n"+
                ");\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_content\n"+
                "    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_field\n"+
                "    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;\n";
    }
    
    @Override
    public String getMySQLScript() {
        return "CREATE TABLE broken_link (\n"+
                "   id VARCHAR(36) PRIMARY KEY,\n"+
                "   inode VARCHAR(36) NOT NULL, \n"+
                "   field VARCHAR(36) NOT NULL,\n"+
                "   link VARCHAR(255) NOT NULL,\n"+
                "   title VARCHAR(255) NOT NULL,\n"+
                "   status_code integer NOT NULL\n"+
                ");\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_content\n"+
                "    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_field\n"+
                "    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;\n";
    }
    
    @Override
    public String getOracleScript() {
        return "CREATE TABLE broken_link (\n"+
                "   id VARCHAR2(36) PRIMARY KEY,\n"+
                "   inode VARCHAR2(36) NOT NULL, \n"+
                "   field VARCHAR2(36) NOT NULL,\n"+
                "   link VARCHAR2(255) NOT NULL,\n"+
                "   title VARCHAR2(255) NOT NULL,\n"+
                "   status_code integer NOT NULL\n"+
                ");\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_content\n"+
                "    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_field\n"+
                "    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;\n";
    }
    
    @Override
    public String getMSSQLScript() {
        return "CREATE TABLE broken_link (\n"+
                "   id VARCHAR(36) PRIMARY KEY,\n"+
                "   inode VARCHAR(36) NOT NULL, \n"+
                "   field VARCHAR(36) NOT NULL,\n"+
                "   link VARCHAR(255) NOT NULL,\n"+
                "   title VARCHAR(255) NOT NULL,\n"+
                "   status_code bigint NOT NULL\n"+
                ");\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_content\n"+
                "    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;\n"+
                "\n"+
                "alter table broken_link add CONSTRAINT fk_brokenl_field\n"+
                "    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;\n";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
