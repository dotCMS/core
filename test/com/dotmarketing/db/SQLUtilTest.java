package com.dotmarketing.db;

import java.sql.SQLException;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;


public class SQLUtilTest extends ServletTestCase {

	public final void testAddLimits() {
		    String dbType = DbConnectionFactory.getDBType();
		    String query = "Select * from contentlet order by inode";
		    long offset = 5;
		    long limit = 10;
		    String mainQuery = SQLUtil.addLimits(query, offset, limit);
		    String expectedQuery = "";
		    if(DbConnectionFactory.POSTGRESQL.equals(dbType) || DbConnectionFactory.MYSQL.equals(dbType) ){
				expectedQuery = query +" LIMIT "+limit+" OFFSET " +offset;
		    }else if(DbConnectionFactory.ORACLE.equals(dbType)){
		    	limit = limit + offset;
		    	expectedQuery = "select * from ( select temp.*, ROWNUM rnum from ( "+
	             query+" ) temp where ROWNUM <= "+limit+" ) where rnum > "+offset;
		    }else if(DbConnectionFactory.MSSQL.equals(dbType)){
		    	String str = "";	 
		    	if(query.toLowerCase().startsWith("select")){
					  query = query.substring(6);
				}
		    	if(query.toLowerCase().contains("order by")){
		  		  str = query.substring(query.indexOf("order by"), query.length());
		  		  query = query.replace(str,"").trim();
		  		}
		    	expectedQuery = " SELECT TOP "+limit+" * FROM (SELECT ROW_NUMBER() " 
   		  	 				  + " OVER ("+str+") AS RowNumber,"+query+") temp " 
   		  	 				  + " WHERE RowNumber >"+offset;	
		    }
		    assertEquals("Checking Query Statement:", expectedQuery, mainQuery);
	}
	 public void testQueryExecution(){
		 String query = "Select * from contentlet order by inode";
		 long offset = 5;
		 long limit = 10;
		 String mainQuery = SQLUtil.addLimits(query, offset, limit);
		 DotConnect dc = new DotConnect();
		 boolean val = false;
		 try {
		       val = dc.executeStatement(mainQuery);	    	
			} catch (SQLException e) {
				e.printStackTrace();
			}
		 assertEquals("Checking Execution of Query:", true, val);
	}
}
