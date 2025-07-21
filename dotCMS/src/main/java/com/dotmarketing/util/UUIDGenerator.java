package com.dotmarketing.util;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import com.dotmarketing.business.APILocator;

public class UUIDGenerator implements IdentifierGenerator {

	public synchronized Serializable generate(SharedSessionContractImplementor arg0, Object arg1)
			throws HibernateException {
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
