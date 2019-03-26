package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

public class Task03040AddIndexesToStructureFields extends AbstractJDBCStartupTask {

  private static final String CREATE_STRUCTURE_INDEXES_SQL =
      "create index idx_structure_host on structure (host);\n"
          + "create index idx_structure_folder on structure (folder);\n";

  @Override
  public boolean forceRun() {
    return true;
  }

  @Override
  public String getPostgresScript() {
    return CREATE_STRUCTURE_INDEXES_SQL;
  }

  @Override
  public String getMySQLScript() {
    return null;
  }

  @Override
  public String getOracleScript() {
    return null;
  }

  @Override
  public String getMSSQLScript() {
    return null;
  }

  @Override
  public String getH2Script() {
    return null;
  }

  @Override
  protected List<String> getTablesToDropConstraints() {
    return null;
  }
}
