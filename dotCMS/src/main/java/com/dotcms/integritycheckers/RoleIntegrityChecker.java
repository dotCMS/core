package com.dotcms.integritycheckers;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.ConfigUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * Role integrity checker implementation
 * 
 */
public class RoleIntegrityChecker extends AbstractIntegrityChecker {

    private final PermissionAPI permissionAPI;
	private final RoleAPI roleAPI;
	private final UserAPI userAPI;
	private final WorkflowAPI workflowAPI;
	
    public RoleIntegrityChecker() {
        permissionAPI = APILocator.getPermissionAPI();
    	roleAPI = APILocator.getRoleAPI();
    	userAPI = APILocator.getUserAPI();
    	workflowAPI = APILocator.getWorkflowAPI();
    }

    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.CMS_ROLES;
    }

    @Override
    public String[] getTempTableNames(String endpointId) {
    	return new String[]{ getTempTableName(endpointId, "local"), getTempTableName(endpointId, "remote") };
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        File csvFile = new File(outputFile);
        try {
        	CsvWriter writer = new CsvWriter(new FileWriter(csvFile, true), '|');

        	try {
        		MutableInt count = new MutableInt(0);

        		queryAndConsumeRoles(role -> {
        			try {
	        			writer.write(role.getRoleId());
	        			writer.write(role.getRoleKey());
	        			writer.write(role.getRoleName());
	        			writer.write(role.getQualifiedId());
	        			writer.write(role.getQualifiedName());
	
	                    writer.endRecord();
	
	                    count.increment();
	                    if ((count.intValue() % 500) == 0) {
	                        writer.flush();
	                    }
        			} catch(IOException e) {
        				throw new RuntimeException(e);
        			}
        		});

        		writer.flush();
            } finally {
            	writer.close();
            }
        } catch(Exception e) {
        	throw e;
        }

        return csvFile;
    }

    @Override
    public boolean generateIntegrityResults(String endpointId) throws Exception {
        try {
            final DotConnect dc = new DotConnect();

            // Create a temp table and insert all roles coming from local db
            final String NAME_TEMP_TABLE_LOCAL = getTempTableName(endpointId, "local");
			final String INSERT_TEMP_TABLE_LOCAL = getTempTableInsertStatement(NAME_TEMP_TABLE_LOCAL);

			dc.executeStatement(getTempTableCreateStatement(NAME_TEMP_TABLE_LOCAL));

            queryAndConsumeRoles(role -> {
				try {
					dc.setSQL(INSERT_TEMP_TABLE_LOCAL);

					dc.addParam(role.getRoleId());
					dc.addParam(role.getRoleKey());
					dc.addParam(role.getRoleName());
					dc.addParam(role.getQualifiedId());
					dc.addParam(role.getQualifiedName());

					dc.loadResult();
				} catch (DotDataException e) {
					throw new RuntimeException("An error occured when generating local temp table for role: "+ role.getRoleId(), e);
				}
            });


            // Create a temp table and insert all roles coming from remote CSV file
            final String NAME_TEMP_TABLE_REMOTE = getTempTableName(endpointId, "remote");
			final String INSERT_TEMP_TABLE_REMOTE = getTempTableInsertStatement(NAME_TEMP_TABLE_REMOTE);

			dc.executeStatement(getTempTableCreateStatement(NAME_TEMP_TABLE_REMOTE));

            CsvReader roles = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(),
                    '|', Charset.forName("UTF-8"));
            while (roles.readRecord()) {
				try {
					CmsRole role = new CmsRole(roles.get(0), roles.get(1), roles.get(2), roles.get(3), roles.get(4));

					dc.setSQL(INSERT_TEMP_TABLE_REMOTE);
					dc.addParam(role.getRoleId());
					dc.addParam(role.getRoleKey());
					dc.addParam(role.getRoleName());
					dc.addParam(role.getQualifiedId());
					dc.addParam(role.getQualifiedName());

					dc.loadResult();
				} catch (DotDataException e) {
					throw new DotDataException("An error occured when generating remote temp table for role: "+ roles.get(0), e);
				}
            }


            // Compare roles from local and remote temp table and see if there are any conflicts
            dc.setSQL("select 1 from "+NAME_TEMP_TABLE_LOCAL +" tr1 join "+NAME_TEMP_TABLE_REMOTE +" tr2 on tr1.role_key = tr2.role_key "
                    + "where tr1.qualified_name = tr2.qualified_name and tr1.qualified_id <> tr2.qualified_id");

            List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // If there are conflicts, populate the results table

                final String INSERT_INTO_RESULTS_TABLE = "insert into "
                	+ getIntegrityType().getResultsTableName() 
                	+ " (name, role_key, local_role_id, remote_role_id, local_role_fqn, remote_role_fqn, endpoint_id)"
                	+ " select tr1.qualified_name as name, tr1.role_key as role_key, tr1.id as local_role_id, tr2.id as remote_role_id,"
                	+	" tr1.qualified_id as local_role_fqn, tr2.qualified_id as remote_role_fqn,"
                	+ " '"+ endpointId+ "'"
                	+ " from "+NAME_TEMP_TABLE_LOCAL +" tr1 join "+NAME_TEMP_TABLE_REMOTE +" tr2 on tr1.role_key = tr2.role_key"
                    + " where tr1.qualified_name = tr2.qualified_name and tr1.qualified_id <> tr2.qualified_id order by name asc";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);

            }

            return (Long) dc.getRecordCount(getIntegrityType().getResultsTableName(), "where endpoint_id = '"+ endpointId+ "'") > 0;
        } catch (Exception e) {
            throw new Exception("Error running the Roles Integrity Check", e);
        }
    }

    /**
     * Fixes role inconsistencies for a given server id. Fixing a role means
     * updating its id with the one received from the other end
     *
     * @param remoteIP
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void executeFix(final String key) throws DotDataException, DotSecurityException {
        
        DotConnect dc = new DotConnect();

        dc.setSQL("select name, local_role_id, remote_role_id from "+ getIntegrityType().getResultsTableName() +
            	 " where endpoint_id = ? order by name asc");
        dc.addParam(key);

        for (Map<String, Object> result : dc.loadObjectResults()) {
        	String oldRoleId = (String) result.get("local_role_id");
        	String newRoleId = (String) result.get("remote_role_id");

        	Role oldRole = roleAPI.loadRoleById(oldRoleId);
        	Role newRole = roleAPI.loadRoleById(newRoleId);

        	// If the local role still exists and has an id different than the one in remote role 
        	if (oldRole != null && newRole == null && !oldRoleId.equals(newRoleId)) {
        		applyFixToRole(dc, oldRole, oldRoleId, newRoleId);
                //clean up permissions for updated role
                permissionAPI.removePermissionableFromCache(oldRole.getId());
        	}
        }
	}

    private void applyFixToRole( DotConnect dc, Role oldRole, String oldRoleId, String newRoleId) throws DotDataException, DotSecurityException {
		// Create new role with dummy role_key to ensure uniqueness
		dc.setSQL(
			"INSERT INTO cms_role (id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, locked, system) "+
			"SELECT '"+newRoleId+"', role_name, description, '"+newRoleId+"', REPLACE(db_fqn, '"+oldRoleId+"', '"+newRoleId+"'), parent, edit_permissions, edit_users, edit_layouts, locked, system "+
			"FROM cms_role WHERE id='"+ oldRoleId +"'"
		);
		dc.loadResult();

		// Replace references from old role to new role
		dc.setSQL("UPDATE cms_role SET db_fqn=REPLACE(db_fqn, '"+oldRoleId+"', '"+newRoleId+"') WHERE id <> '"+ oldRoleId +"' AND db_fqn LIKE '%"+ oldRoleId +"%'");
		dc.loadResult();

		dc.setSQL("UPDATE cms_role SET parent=? WHERE parent=?");
		dc.addParam(newRoleId);
		dc.addParam(oldRoleId);
		dc.loadResult();

		dc.setSQL("UPDATE users_cms_roles SET role_id=? WHERE role_id=?");
		dc.addParam(newRoleId);
		dc.addParam(oldRoleId);
		dc.loadResult();

		dc.setSQL("UPDATE permission SET roleid=? WHERE roleid=?");
		dc.addParam(newRoleId);
		dc.addParam(oldRoleId);
		dc.loadResult();

		dc.setSQL("UPDATE layouts_cms_roles SET role_id = ? WHERE role_id = ?");
		dc.addParam(newRoleId);
		dc.addParam(oldRoleId);
		dc.loadResult();

		workflowAPI.updateUserReferences(userAPI.getSystemUser().getUserId(), oldRoleId, userAPI.getSystemUser().getUserId(), newRoleId);

		// Delete old role
		dc.setSQL("DELETE FROM cms_role WHERE id=?");
		dc.addParam(oldRoleId);
		dc.loadResult();

		// Restore original role_key into the new role
		dc.setSQL("UPDATE cms_role SET role_key=? WHERE role_key=?");
		dc.addParam(oldRole.getRoleKey());
		dc.addParam(newRoleId);
		dc.loadResult();
    }

	private String getTempTableCreateStatement(String tempTableName) {
		String createTempTableSql = "create "
		        + DbConnectionFactory.getTempKeyword()
		        + " table "
		        + tempTableName
		        + " (id varchar(36) not null, role_key varchar(255) , role_name varchar(255) not null, "
		        + "qualified_id varchar(1000) not null, qualified_name varchar(1000) not null, primary key (id) )"
		        + (DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : "");

		if (DbConnectionFactory.isOracle()) {
			createTempTableSql = createTempTableSql.replaceAll("varchar\\(", "varchar2\\(");
		}
		return createTempTableSql;
	}

	private String getTempTableInsertStatement(String tempTableName) {
		return "insert into " + tempTableName + " (id, role_key, role_name, qualified_id, qualified_name) values(?,?,?,?,?)";
	}

    private void queryAndConsumeRoles(Consumer<CmsRole> consumer) throws DotDataException {
    	String query = "select id, role_key, role_name, parent, db_fqn from cms_role where role_key <> '"+ RoleAPI.DEFAULT_USER_ROLE_KEY + "' order by db_fqn asc";

        Connection conn = DbConnectionFactory.getConnection();
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            try (ResultSet rs = statement.executeQuery()) {

            	Stack<CmsRole> currentRolePath = new Stack<>();

                while (rs.next()) {
                	// Read role attributes from database
                	String roleId = rs.getString("id");
                	String roleKey = StringUtils.defaultIfEmpty(rs.getString("role_key"), "");
                	String roleName = rs.getString("role_name");
                	String roleParent = rs.getString("parent");

                	// Resolve parent
                	CmsRole parent = null;
                	while(!currentRolePath.isEmpty()) {
                		if (currentRolePath.peek().getRoleId().equals(roleParent)) {
                			parent = currentRolePath.peek();
                			break;
                		}
            			currentRolePath.pop();
                	}

                	// Create, register and consume role
                	CmsRole role = new CmsRole(roleId, roleKey, roleName, parent);
                	currentRolePath.push(role);
                	consumer.accept(role);
                }
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }


    private static class CmsRole {
    	private final String roleId;
    	private final String roleKey;
    	private final String roleName;
    	private final String qualifiedId;    	
    	private final String qualifiedName;

    	public CmsRole(String roleId, String roleKey, String roleName, CmsRole parent) {
    		this.roleId = roleId;
    		this.roleKey = roleKey;
    		this.roleName = roleName;

    		if (parent == null) {
    			this.qualifiedId = roleId;
    			this.qualifiedName = roleName;
    		} else {
    			this.qualifiedId = parent.getQualifiedId() +" --> "+ roleId;
    			this.qualifiedName = parent.getQualifiedName() +" --> "+ roleName;
    		}
    	}

    	public CmsRole(String roleId, String roleKey, String roleName, String qualifiedId, String qualifiedName) {
    		this.roleId = roleId;
    		this.roleKey = roleKey;
    		this.roleName = roleName;

			this.qualifiedId = qualifiedId;
			this.qualifiedName = qualifiedName;
    	}


		public String getRoleId() {
			return roleId;
		}

		public String getRoleKey() {
			return roleKey;
		}

		public String getRoleName() {
			return roleName;
		}

		public String getQualifiedId() {
			return qualifiedId;
		}

		public String getQualifiedName() {
			return qualifiedName;
		}
    }
}