package com.dotmarketing.startup.runonce;

import java.util.Date;
import java.util.List;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.personas.business.PersonaAPI;
import com.dotcms.repackage.org.python.modules.newmodule;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to change status_pojo column type from TEXT to LONGTEXT in publishing_queue_audit table
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 12-04-2015
 */
public class Task03510CreateDefaultPersona extends AbstractJDBCStartupTask {

    private final String INSERT_QUERY = "insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, folder, mod_date, default_structure) value ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";
    private final String INSERT_INODE = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";
    // make sure we have inode and structure entries
    private final String SELECT_QUERY = "select inode.inode from inode,structure where inode.inode = ? and structure.inode = inode.inode";

    @Override
    public boolean forceRun() {
        return true;
    }

  


    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        
    	DotConnect dc = new DotConnect();
    	dc.setSQL(SELECT_QUERY);
    	dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
    	if(dc.loadResults().size()>0){
    		return;
    	}
    	
		
		dc.setSQL(INSERT_INODE);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		dc.addParam(UserAPI.SYSTEM_USER_ID);
		dc.addParam(new Date());
		dc.addParam(new Structure().getType());
		dc.loadResult();
		
		
		dc.setSQL(INSERT_QUERY);
    	/**
    	 	inode
			name
			description
			structuretype
			system
			fixed
			velocity_var_name
			host
			folder
			mod_date 
    	 */
	
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_NAME);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION);
		dc.addParam(Structure.STRUCTURE_TYPE_PERSONA);
		dc.addParam(false);
		dc.addParam(true);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME);
		dc.addParam(Host.SYSTEM_HOST);
		dc.addParam(FolderAPI.SYSTEM_FOLDER);
		dc.addParam(new Date());
		dc.addParam(false);
		dc.loadResult();
		
		
		// these need to be fixed and identical in all installations
		final String[] defaultPersonaFieldInodes = {
		"606ac3af-63e5-4bd4-bfa1-c4c672bb8eb8", "0ea2bd92-4b2d-48a2-a394-77fd560b1fce", "6b25d960-034d-4030-b785-89cc01baaa3d",
				"07cfbc2c-47de-4c78-a411-176fe8bb24a5", "2dab7223-ebb5-411b-922f-611a30bc2a2b", "65e4e742-d87a-47ff-84ef-fde44e889e27", "f9fdd242-6fac-4d03-9fa3-b346d6995779" };
		
		Structure proxy = new Structure();
		proxy.setInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		List<Field> fields =  APILocator.getPersonaAPI().getBasePersonaFields(proxy);
		int i=0;
		for(Field f : fields){
			f.setInode(defaultPersonaFieldInodes[i++]);
			f.setSortOrder(i);
			FieldFactory.saveField(f, f.getInode());
		}
    }




	@Override
	public String getPostgresScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getMySQLScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getOracleScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getMSSQLScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getH2Script() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}
    	
    	
    

}
