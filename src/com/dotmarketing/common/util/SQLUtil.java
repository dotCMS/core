package com.dotmarketing.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;

import net.sourceforge.squirrel_sql.fw.preferences.BaseQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mssql.prefs.MSSQLPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.mssql.tokenizer.MSSQLQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mysql.tokenizer.MysqlQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.oracle.prefs.OraclePreferenceBean;
import net.sourceforge.squirrel_sql.plugins.oracle.tokenizer.OracleQueryTokenizer;

public class SQLUtil {
    
    private static SecurityLoggerServiceAPI securityLoggerServiceAPI =
            APILocator.getSecurityLogger(); 

	// this is only for unit test, must be always on true.
	private static boolean enableLog = true;

	@VisibleForTesting
	protected static void enableLog (final boolean enable) {

		enableLog = enable;
	}

	private static final Set<String> EVIL_SQL_CONDITION_WORDS =  ImmutableSet.of( "insert", "delete", "update",
	        "replace", "create", "drop", "alter", "truncate", "declare", "exec", "--", "procedure", "pg_", "lock",
	        "unlock", "write", "engine", "mode", "set ", "sleep", ";");

	private static final Set<String> EVIL_SQL_PARAMETER_WORDS =  ImmutableSet.of( "select", "distinct", "like", "and", "or", "limit",
	        "group", "order", "as ", "count", "where", "null", "not ");

	private static final Set<String> EVIL_SQL_WORDS =  new ImmutableSet.Builder<String>()
	        .addAll( EVIL_SQL_CONDITION_WORDS )
	        .addAll( EVIL_SQL_PARAMETER_WORDS )
	        .build();

	private final static Set<String> ORDERBY_WHITELIST= ImmutableSet.of(
			"title","filename", "moddate", "tagname","pageUrl",
			"category_name","category_velocity_var_name",
			"mod_date","structuretype,upper(name)","upper(name)",
			"category_key", "page_url","name","velocity_var_name",
			"description","category_","sort_order","hostName", "keywords",
			"mod_date,upper(name)");
	
	public static List<String> tokenize(String schema) {
		List<String> ret=new ArrayList<String>();
		if (schema!=null) {
		QueryTokenizer tokenizer=new QueryTokenizer(";","--",true);
		QueryTokenizer extraTokenizer=null;
		if (DbConnectionFactory.isMsSql()) {
			//";","--",true
			IQueryTokenizerPreferenceBean prefs = new MSSQLPreferenceBean();
			//prefs.setStatementSeparator("GO");
			extraTokenizer=new MSSQLQueryTokenizer(prefs);
		}else if(DbConnectionFactory.isOracle()){
			IQueryTokenizerPreferenceBean prefs = new OraclePreferenceBean();
			tokenizer=new OracleQueryTokenizer(prefs);
		}else if(DbConnectionFactory.isMySql()){
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
			if(DbConnectionFactory.isMsSql()){
				if(!first){
					bob.append(" + ");
				}
				bob.append("cast( " ).append( col ).append( " as varchar(512))");
			}else if(DbConnectionFactory.isMySql()){
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
		if(DbConnectionFactory.isMySql()){
			bob.append(")");
		}
		return bob.toString();
	}

	/**
	 * Appends the required SQL code to the existing query to limit the number
	 * of results returned by such a query. You can also specify the offset if
	 * paginated results are required. This method will handle all the
	 * database-specific details related to keywords.
	 * 
	 * @param query
	 *            - The SQL query that will be executed.
	 * @param offSet
	 *            - The number of rows to start reading from.
	 * @param limit
	 *            - The maximum number of rows to return.
	 * @return The database-specific SQL statement that will add a row limit
	 *         and/or offset to the results.
	 */
	public static String addLimits(String query, long offSet, long limit) {
		if ( offSet == 0 && limit == -1 ) {
			//Nothing to do...
			return query;
		}
		StringBuffer queryString = new StringBuffer(); 
		int count = 0;
		if(query!=null){
			query = query.toLowerCase();
		  count = StringUtil.count(query, "select");
		}
		if(!UtilMethods.isSet(query)|| !query.trim().contains("select")|| count>1){
			return query;
		}else{
		     if(DbConnectionFactory.isPostgres()||
				DbConnectionFactory.isMySql() || DbConnectionFactory.isH2()){
			   query = query +" LIMIT "+limit+" OFFSET " +offSet;
			   queryString.append(query);

	         }else if(DbConnectionFactory.isMsSql()){
	        	 String str = "";
		    	   if(query.startsWith("select")){
					  query = query.substring(6);
				   }
		    	   if(query.contains("order by")){
		  			  str = query.substring(query.indexOf("order by"), query.length());
		  			  query = query.replace(str,"").trim();
		  		   }
		    	   query = " SELECT TOP "+limit+" * FROM (SELECT ROW_NUMBER() "
		    		  	 + " OVER ("+str+") AS RowNumber,"+query+") temp "
		    		  	 + " WHERE RowNumber >"+offSet;
		    	   queryString.append(query);
	        }else if(DbConnectionFactory.isOracle()){
	        	limit = limit + offSet;
	        	query = "select * from ( select temp.*, ROWNUM rnum from ( "+
	    	             query+" ) temp where ROWNUM <= "+limit+" ) where rnum > "+offSet;
	        	queryString.append(query);
	        }
		}
  	  return queryString.toString();
	}
	
	/**
	 * Method to sanitize order by SQL injection
	 * @param parameter
	 * @return
	 */
	public static String sanitizeSortBy(String parameter){



		
		if(!UtilMethods.isSet(parameter)){//check if is not null
			return "";
		}


		String testParam=parameter.replaceAll(" asc", "").replaceAll(" desc", "").replaceAll("-", "").toLowerCase();
		if(ORDERBY_WHITELIST.contains(testParam)){
			return parameter;
		}

		Exception e = new DotStateException("Invalid or pernicious sql parameter passed in : " + parameter);
		Logger.error(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter, e);

		SecurityLogger.logDebug(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter);
		return "";
	}

	/**
	 * Applies the sanitize to the parameter argument in order to avoid evil sql words for a PARAMETER.
	 * @param parameter SQL to filter.
	 * @return String with filtered SQL.
	 */
    public static String sanitizeParameter(String parameter){

        return sanitizeSQL( parameter, EVIL_SQL_WORDS );
    } // sanitizeParameter.

    /**
     * Applies the sanitize to the parameter argument in order to avoid evil sql words for a CONDITION.
     * @param condition SQL to filter.
     * @return String with filtered SQL.
     */
    public static String sanitizeCondition(String condition){

        return sanitizeSQL( condition, EVIL_SQL_CONDITION_WORDS );
    } // sanitizeCondition.

    /**
     * Util method to filter the parameter SQL with a list of EVIL WORDS not allowed.
     *
     * @param parameter SQL to filter.
     * @param evilWords words not allowed in the parameter SQL.
     * @return String with filtered SQL.
     */
    public static String sanitizeSQL( String parameter, final Set<String> evilWords ) {
        if(!UtilMethods.isSet(parameter)) { //check if is not null

            return StringPool.BLANK;
        }

        parameter = StringEscapeUtils.escapeSql(parameter);

        final String parameterLowercase = parameter.toLowerCase();

        for(String evilWord : evilWords){

            final int index = parameterLowercase.indexOf(evilWord);

            //check if the order by requested have any other command
            if(index != -1  &&
                    (
                            (index  == 0 // if the evilWord is at the begin of the parameterLowercase AND
                                    || !isValidSQLCharacter(parameterLowercase.charAt(index - 1)) // there is not alphanumeric before parameterLowercase is invalid
                            )  &&
                                    (index + evilWord.length() == parameterLowercase.length() // if the evilWord is at the end of the parameterLowercase is invalid
                                            || !isValidSQLCharacter(parameterLowercase.charAt(index + evilWord.length()))  // if there is not alphanumeric next is invalid
                                    )
                    )) {

                Exception e = new DotStateException("Invalid or pernicious sql parameter passed in : " + parameter);
                Logger.error(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter, e);
                securityLoggerServiceAPI.logInfo(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter);

                return StringPool.BLANK;
            }
        }

        return parameter;
    }

	/**
	 * Determine if the character is a valid for sql
	 * @param c char
	 * @return boolean
     */
	private static boolean isValidSQLCharacter (final char c) {

		return Character.isLetterOrDigit(c) || '-' == c || '_' == c;
	} // isValidSQLCharacter.
}
