package com.dotmarketing.util;

import com.dotmarketing.common.db.DotConnect;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.engine.SessionImplementor;
import com.dotcms.repackage.net.sf.hibernate.id.IdentifierGenerator;

public class UUIDGenerator implements IdentifierGenerator {

	public synchronized Serializable generate(SessionImplementor arg0, Object arg1)
			throws SQLException, HibernateException {
		return UUIDGenerator.generateUuid();

	}

	public void insertInode(String inode, String owner, String type, String identifier) throws SQLException {
		String sql = "INSERT INTO INODE(INODE, OWNER, IDATE, TYPE, IDENTIFIER) VALUES (?,?,CURRENT TIMESTAMP,?,?)";
		DotConnect dot = new DotConnect();
		dot.setSQL(sql);
		dot.addParam(inode);
		dot.addParam(owner);
		dot.addParam(type);
		dot.addParam(identifier);
		dot.executeStatement(sql);

	}
	
	public static String generateUuid(){
	  return UUID.randomUUID().toString();
	}
	
}
