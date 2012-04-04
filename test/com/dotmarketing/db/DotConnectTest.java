package com.dotmarketing.db;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.common.db.DotConnect;

/**
 * 
 * This is the first version of this test class and only contains a few of methods to be tested for dotConnect
 * @author davidtorresv
 * @version 1.8
 * @since 1.8
 *
 */
public class DotConnectTest extends ServletTestCase {

	public void testBitAND() {
		String result = DotConnect.bitAND("A", "B");
		String dbType = DbConnectionFactory.getDBType();
		if (DbConnectionFactory.ORACLE.equals(dbType)) {
			assertEquals(result, "BITAND(A,B)");
		} else {
			assertEquals(result, "(A & B)");
		}
	}

	public void testBitOR() {
		String result = DotConnect.bitOR("A", "B");
		String dbType = DbConnectionFactory.getDBType();
		if (DbConnectionFactory.ORACLE.equals(dbType)) {
			assertEquals(result, "BITOR(A,B)");
		} else {
			assertEquals(result, "(A | B)");
		}
	}
	

	public void testBitXOR() {
		String result = DotConnect.bitXOR("A", "B");
		String dbType = DbConnectionFactory.getDBType();
		if (DbConnectionFactory.ORACLE.equals(dbType)) {
			assertEquals(result, "BITXOR(A,B)");
		} else if(DbConnectionFactory.POSTGRESQL.equals(dbType)) {
			assertEquals(result, "(A # B)");
		} else if(DbConnectionFactory.MYSQL.equals(dbType) || DbConnectionFactory.MSSQL.equals(dbType)) {
			assertEquals(result, "(A ^ B)");
		}

	}
	

	public void testBitNOT() {
		String result = DotConnect.bitNOT("A");
		String dbType = DbConnectionFactory.getDBType();
		if (DbConnectionFactory.ORACLE.equals(dbType)) {
			assertEquals(result, "BITNOT(A)");
		} else {
			assertEquals(result, "(~A)");
		}
	}
}
