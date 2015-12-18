package com.dotmarketing.startup.runonce;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to add the defualt persona content type
 * 
 * @author 
 * @version 1.0
 * @since 12-04-2015
 */
public class Task03510CreateDefaultPersona extends AbstractJDBCStartupTask {


    @Override
    public boolean forceRun() {
        return true;
    }

  


    /**
     * ALL of this logic and SQL is duped in the PersonaFactoryImpl
     * and could be called by:
     * APILocator.getPersonaAPI().createDefaultPersonaStructure();
     * But I did not want to "CALL an API in a startup task"
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
    	APILocator.getPersonaAPI().createDefaultPersonaStructure();
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
