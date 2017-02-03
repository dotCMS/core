package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

/**
 * This task drops the container_structure table and re-creates it.
 * 
 * @author Daniel Silva
 *
 */
public class Task03000CreateContainertStructures implements StartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {

		String createIndex = "create index idx_container_id on container_structures(container_id)";
		String addTemplateFK = "alter table container_structures add constraint FK_cs_container_id foreign key (container_id) references identifier(id)";
		String container_structures_relations = "Select identifier,structure_inode, code from containers where max_contentlets > 0 ";
		String delete_code_when_content = "update containers set code='' where max_contentlets > 0";
		String drop_structure_column = "alter table containers drop column structure_inode";
		String drop_metadata_column = "alter table containers drop column for_metadata";

		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
			DotConnect dc = new DotConnect();
			try {
				dc.executeStatement("drop table container_structures");
			} catch (SQLException e) {
				Logger.info(getClass(),
						"container_structures table does not exist. Will be created.");
			}
			String createTable = "Create table container_structures"
					+ "(id varchar(36) NOT NULL  primary key,"
					+ "container_id varchar(36) NOT NULL,"
					+ "structure_id varchar(36) NOT NULL, " + "code text)";

			if (DbConnectionFactory.isOracle()) {
				createTable = createTable.replaceAll("varchar\\(",
						"varchar2\\(");
				createTable = createTable.replaceAll("text", "nclob");
			} else if (DbConnectionFactory.isMySql()) {
				createTable = createTable.replaceAll("text", "longtext");
			}
			dc.executeStatement(createTable);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		
		//DDL Operations must be outside a Transaction for SQL Server Databases
		try {
			DotConnect dc = new DotConnect();
			dc.executeStatement(addTemplateFK);
			dc.executeStatement(createIndex);

		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}

		//Migrating all the data.
		HibernateUtil.startTransaction();
		try {
			DotConnect dc = new DotConnect();
			if (DbConnectionFactory.isMsSql()) {
				dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
			}

			dc.setSQL(container_structures_relations);
			List<Map<String, String>> relations = dc.loadResults();

			for(Map<String,String> relation : relations){
				String containerId = relation.get("identifier");
				String structureId = relation.get("structure_inode");
				String code = relation.get("code");
				String uuid = UUIDGenerator.generateUuid();

				dc.setSQL("insert into container_structures(id,container_id,structure_id,code) values(?,?,?,?)");
				dc.addParam(uuid);
				dc.addParam(containerId);
				dc.addParam(structureId);
				dc.addParam(code);
				dc.loadResult();
			}
			dc.executeStatement(delete_code_when_content);

		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(this, e.getMessage(),e);
		}
		
		try{
			HibernateUtil.commitTransaction();
		}catch (Exception e){
			//If for any reason the inserts fail, Throw exception and avoid next queries to drop columns from dot_containers table
			throw new DotDataException(e.getMessage(), e);
		}
		
		//Lets remove the foreign key if exists prior to drop the column.
		try {
			DotConnect dc = new DotConnect();
			if (DbConnectionFactory.isMySql()){
				dc.executeStatement("alter table containers drop foreign key structure_fk ");
			} else{
				dc.executeStatement("alter table containers drop constraint structure_fk ");
			}
		} catch(Exception e) {
			Logger.info(this, "foreign key for structure_inode on containers table didn't exist, not dropping anything here");
		}
		
		//Lets remove the foreign key if exists prior to drop the column.
		//DDL Operations must be outside a Transaction for SQL Server Databases
		try {
			DotConnect dc = new DotConnect();
			dc.executeStatement(drop_structure_column);
			dc.executeStatement(drop_metadata_column);
		} catch(Exception e) {
			Logger.info(this, "Columns from containers table could not be dropped: " + e.getMessage());
		}

	}

}
