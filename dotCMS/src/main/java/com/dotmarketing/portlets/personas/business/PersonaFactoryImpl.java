package com.dotmarketing.portlets.personas.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class PersonaFactoryImpl implements PersonaFactory {

	private final String INSERT_DEFAULT_STRUC_QUERY = "insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, folder, mod_date, default_structure) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";
	private final String INSERT_DEFAULT_STRUC_INODE_QUERY = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";
	
	// make sure we have inode and structure entries
	private final String SELECT_DEFAULT_STRUC_QUERY = "select inode.inode from inode,structure where inode.inode = ? and structure.inode = inode.inode";
	
	// these need to be fixed and identical in all installations
	private final String[] DEFAUTL_PERSONA_FIELD_INODES = { "606ac3af-63e5-4bd4-bfa1-c4c672bb8eb8", "0ea2bd92-4b2d-48a2-a394-77fd560b1fce",
		"6b25d960-034d-4030-b785-89cc01baaa3d", "07cfbc2c-47de-4c78-a411-176fe8bb24a5", "2dab7223-ebb5-411b-922f-611a30bc2a2b",
		"65e4e742-d87a-47ff-84ef-fde44e889e27", "f9fdd242-6fac-4d03-9fa3-b346d6995779" };

	
	
	/**
	 *  ALL OF of this logic is DUPED in com.dotmarketing.startup.runonce.Task03510CreateDefaultPersona
	 */
	@Override
	public void createDefualtPersonaStructure() throws DotDataException {

		
		
		
		
		
		boolean localTransaction = false;
		DotConnect dc = new DotConnect();

		try {
			localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
		
			

			dc.setSQL(SELECT_DEFAULT_STRUC_QUERY);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
			if (dc.loadResults().size() > 0) {
				return;
			}

			dc.setSQL(INSERT_DEFAULT_STRUC_INODE_QUERY);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
			dc.addParam(UserAPI.SYSTEM_USER_ID);
			dc.addParam(new Date());
			dc.addParam(new Structure().getType());
			dc.loadResult();
	
	
			/**
			 * inode name description structuretype system fixed velocity_var_name
			 * host folder mod_date
			 */
			 dc = new DotConnect();
			dc.setSQL(INSERT_DEFAULT_STRUC_QUERY);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_NAME);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION);
			dc.addParam(Structure.STRUCTURE_TYPE_PERSONA);
			dc.addParam(false);
			dc.addParam(false);
			dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME);
			dc.addParam(Host.SYSTEM_HOST);
			dc.addParam(FolderAPI.SYSTEM_FOLDER);
			dc.addParam(new Date());
			dc.addParam(false);
			dc.loadResult();
	
			
			
			Structure proxy = new Structure();
			proxy.setInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
			List<Field> fields = APILocator.getPersonaAPI().getBasePersonaFields(proxy);
			int i = 0;
			for (Field f : fields) {
				f.setInode(DEFAUTL_PERSONA_FIELD_INODES[i++]);
				FieldFactory.saveField(f, f.getInode());
			}
		} catch (final Exception e) {
			if (localTransaction) {
				HibernateUtil.rollbackTransaction();
			}
			Logger.error(this, "defualty persona creation failed:" + e, e);
			throw new DotDataException(e.toString());
		}
		finally{
			if (localTransaction) {
				try{
					HibernateUtil.commitTransaction();
					HibernateUtil.closeSession();
					DbConnectionFactory.closeConnection();
				}
				catch(Exception e){
					Logger.error(this, "should not be here failed:" + e, e);
				}
			}
		}


	}

}
