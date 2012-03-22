package org.quartz.impl.jdbcjobstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.dotmarketing.util.Logger;

import org.apache.commons.logging.Log;

public class DotMSSQLDelegate extends MSSQLDelegate{

	public DotMSSQLDelegate(Log log, String tablePrefix, String instanceId) {
		super(log, tablePrefix, instanceId);
	}
	protected Object getObjectFromBlob(ResultSet rs, String colName)
	throws ClassNotFoundException, IOException, SQLException
	{
	InputStream binaryInput = rs.getBinaryStream(colName);
	if (binaryInput == null || binaryInput.available()== 0)
	{
	return null;
	}

	Object obj = null;
	ObjectInputStream in =null;
	
	try
	{
	  in= new ObjectInputStream(binaryInput);
	  obj = in.readObject();
	}catch(Exception e){
		Logger.error(this, e.getMessage());
		e.printStackTrace();
	}
	finally
	{
	if(in!=null)	
	  in.close();
	}

	return obj;
	}
}
