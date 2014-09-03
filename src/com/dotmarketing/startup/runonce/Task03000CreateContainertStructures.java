package com.dotmarketing.startup.runonce;

import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

public class Task03000CreateContainertStructures implements StartupTask{

	public void executeUpgrade() throws DotDataException, DotRuntimeException {

		DotConnect dc = new DotConnect();

		String createIndex = "create index idx_container_id on container_structures(container_id)";

		String addTemplateFK = "alter table container_structures add constraint FK_cs_container_id foreign key (container_id) references identifier(id)";

		String container_structures_relations = "Select identifier,structure_inode, code from containers where max_contentlets > 0 ";

		String delete_code_when_content = "update containers set code='' where max_contentlets > 0";
		String drop_structure_column = "alter table containers drop column structure_inode";
		String drop_metadata_column = "alter table containers drop column for_metadata";


		HibernateUtil.startTransaction();
		try {
			if (DbConnectionFactory.isMsSql())
				  dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");

			dc.executeStatement(addTemplateFK);
			dc.executeStatement(createIndex);

			dc.setSQL(container_structures_relations);
			@SuppressWarnings("unchecked")
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

			// let's remove the foreign key if exists prior to drop the column
			try {
				if (DbConnectionFactory.isMySql())
					dc.executeStatement("alter table containers drop foreign key structure_fk ");
				else
					dc.executeStatement("alter table containers drop constraint structure_fk ");
			} catch(Exception e) {
				Logger.info(this, "foreign key for structure_inode on containers table didn't exist, not dropping anything here");
			}

			dc.executeStatement(drop_structure_column);
			dc.executeStatement(drop_metadata_column);
		} catch (Exception e) {
		 HibernateUtil.rollbackTransaction();
		 Logger.error(this, e.getMessage(),e);
		}

		HibernateUtil.commitTransaction();
	}

	public boolean forceRun() {
		return true;
	}


}
