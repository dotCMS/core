package com.dotmarketing.util;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.engine.SessionImplementor;
import com.dotcms.repackage.net.sf.hibernate.id.IdentifierGenerator;
import com.dotmarketing.business.APILocator;

public class UUIDGenerator implements IdentifierGenerator {

	public synchronized Serializable generate(SessionImplementor arg0, Object arg1)
			throws SQLException, HibernateException {
		return UUIDGenerator.generateUuid();

	}


	public static String generateUuid(){
	  return UUID.randomUUID().toString();
	}
	
	
	 public static String uuid(){
	    return UUID.randomUUID().toString();
	  }
	 
   public static String shorty(){
     return APILocator.getShortyAPI().shortify(UUID.randomUUID().toString());
   }
   
   
   public static String ulid() {
     return new ULID().nextULID();
     
   }
   
   
   
	 
}
