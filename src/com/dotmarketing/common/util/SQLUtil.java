package com.dotmarketing.common.util;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.preferences.BaseQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mssql.prefs.MSSQLPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.mssql.tokenizer.MSSQLQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.oracle.prefs.OraclePreferenceBean;
import net.sourceforge.squirrel_sql.plugins.oracle.tokenizer.OracleQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mysql.tokenizer.MysqlQueryTokenizer;



import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringUtil;

public class SQLUtil {

	public static List<String> tokenize(String schema) {
		List<String> ret=new ArrayList<String>();
		if (schema!=null) {
		String dbType = DbConnectionFactory.getDBType();
		QueryTokenizer tokenizer=new QueryTokenizer(";","--",true);
		QueryTokenizer extraTokenizer=null;
		if (dbType.equalsIgnoreCase(DbConnectionFactory.MSSQL)) {
			//";","--",true
			IQueryTokenizerPreferenceBean prefs = new MSSQLPreferenceBean();
			//prefs.setStatementSeparator("GO");
			extraTokenizer=new MSSQLQueryTokenizer(prefs);
		}else if(dbType.equalsIgnoreCase(DbConnectionFactory.ORACLE)){
			IQueryTokenizerPreferenceBean prefs = new OraclePreferenceBean();
			tokenizer=new OracleQueryTokenizer(prefs);
		}else if(dbType.equalsIgnoreCase(DbConnectionFactory.MYSQL)){
			IQueryTokenizerPreferenceBean prefs = new BaseQueryTokenizerPreferenceBean();
			prefs.setProcedureSeparator("#");
			tokenizer=new MysqlQueryTokenizer(prefs);
			
		}
		tokenizer.setScriptToTokenize(schema.toString());
		
		 while (tokenizer.hasQuery() )
            {
               String querySql = tokenizer.nextQuery();
               if (querySql != null)
               {
            	   if (extraTokenizer !=null) {
            		   extraTokenizer.setScriptToTokenize(querySql);
            		   if (extraTokenizer.hasQuery()) {
            			   while (extraTokenizer.hasQuery()) {
            				   String innerSql = extraTokenizer.nextQuery();
            				   if (innerSql!=null) {
            					   
            					   ret.add(innerSql);
            				   }
            		   		}
            		   } else {
            			   
	            		  ret.add(querySql);
            		   }
            	   } else {
            		   
            		   ret.add(querySql);
            	   }
               }
            }
		}
		 return ret;
		
	}

	/**
	 * Will take the passed in columns and concat them for you probably for primary dotCMS DB. 
	 * For SQLServer the all fields will be cast as a varchar(512)
	 * @param dbColumns The name of the columns to use in the DB concat
	 * @return
	 */
	public static String concat(String ... dbColumns) throws DotRuntimeException{
		if (dbColumns == null){
			throw new DotRuntimeException("the column list being concated are null");
		}
		StringBuilder bob = new StringBuilder();
		boolean first = true;
		for (String col : dbColumns) {
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				if(!first){
					bob.append(" + ");
				}
				bob.append("cast( " + col + " as varchar(512))");
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				if(first){
					bob.append("CONCAT(");
				}else{
					bob.append(",");
				}
				bob.append(col);
			}else{
				if(!first){
					bob.append(" || ");
				}
				bob.append(col);
			}
			first = false;
		}
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			bob.append(")");
		}
		return bob.toString();
	}
	//http://jira.dotmarketing.net/browse/DOTCMS-3689
	public static String addLimits(String query, long offSet, long limit) {
		StringBuffer queryString = new StringBuffer();
		int count = 0;
		if(query!=null){
		  count = StringUtil.count(query.toLowerCase(), "select");
		}
		if(!UtilMethods.isSet(query)|| !query.toLowerCase().trim().contains("select")|| query.contains("?")||count>1){
			return query;			
		}else{	
		     if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||
				DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			   query = query +" LIMIT "+limit+" OFFSET " +offSet;
			   queryString.append(query);
			
	         }else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
	        	 String str = "";	 
		    	   if(query.toLowerCase().startsWith("select")){
					  query = query.substring(6);
				   }
		    	   if(query.toLowerCase().contains("order by")){
		  			  str = query.substring(query.indexOf("order by"), query.length());
		  			  query = query.replace(str,"").trim();
		  		   }
		    	   query = " SELECT TOP "+limit+" * FROM (SELECT ROW_NUMBER() " 
		    		  	 + " OVER ("+str+") AS RowNumber,"+query+") temp " 
		    		  	 + " WHERE RowNumber >"+offSet;	
		    	   queryString.append(query);
	        }else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
	        	limit = limit + offSet;
	        	query = "select * from ( select temp.*, ROWNUM rnum from ( "+
	    	             query+" ) temp where ROWNUM <= "+limit+" ) where rnum > "+offSet;
	        	queryString.append(query);
	        }
		} 
  	  return queryString.toString();
	}	
}
