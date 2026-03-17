package com.dotmarketing.common.util;

import static com.liferay.util.StringPool.SPACE;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.SecurityThreatLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.sourceforge.squirrel_sql.fw.preferences.BaseQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mssql.prefs.MSSQLPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.mssql.tokenizer.MSSQLQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.mysql.tokenizer.MysqlQueryTokenizer;
import net.sourceforge.squirrel_sql.plugins.oracle.prefs.OraclePreferenceBean;
import net.sourceforge.squirrel_sql.plugins.oracle.tokenizer.OracleQueryTokenizer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;



/**
 * Utility class for sanitizing, tokenizing, and providing several common-use methods to create,
 * verify and transform SQL queries.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class SQLUtil {

	private static final SecurityLoggerServiceAPI securityLoggerServiceAPI =
			APILocator.getSecurityLogger();

	// When you need to send a sort but do not want to actually sort by anything
	public static final String DOT_NOT_SORT  = "dotnosort";
	public static final String ASC  = "asc";
	public static final String DESC  = "desc";
	public static final String _ASC  = " " + ASC ;
	public static final String _DESC  = " " + DESC;
	public static final String PARAMETER = "?";

	private static final String ORACLE_SQL_STATE_UNIQUE_CONSTRAINT = "23000";
	private static final String POSTGRE_SQL_STATE_UNIQUE_CONSTRAINT = "23505";

    private static final Set<String> EVIL_SQL_CONDITION_WORDS =  ImmutableSet.of( "insert", "delete", "update",
            "replace", "create", "drop", "alter", "truncate", "declare", "exec", "--", "procedure", "pg_", "lock",
            "unlock", "write", "engine", "mode", "set ", "sleep", ";");

    private static final Set<String> EVIL_SQL_PARAMETER_WORDS =  ImmutableSet.of( "select", "distinct", "like", "and", "or", "limit",
            "group", "order", "as ", "count", "where", "null", "not ");

	private static final Set<String> EVIL_SQL_WORDS =  new ImmutableSet.Builder<String>()
                                                                        .addAll( EVIL_SQL_CONDITION_WORDS )
                                                                        .addAll( EVIL_SQL_PARAMETER_WORDS )
                                                                        .build();

    // SECURITY: Pre-compiled patterns cache to prevent repeated Pattern.compile() calls
    private static final Map<String, Pattern> EVIL_WORD_PATTERNS = new HashMap<>();
    
    static {
        // Pre-compile patterns for all evil words to improve performance and prevent DoS
        for (String evilWord : EVIL_SQL_CONDITION_WORDS) {
            EVIL_WORD_PATTERNS.put(evilWord, createEvilWordPattern(evilWord));
        }
        for (String evilWord : EVIL_SQL_PARAMETER_WORDS) {
            EVIL_WORD_PATTERNS.put(evilWord, createEvilWordPattern(evilWord));
        }
    }

            /**
         * Creates appropriate regex pattern for evil word detection that matches the original boundary logic.
         * Must consider '-' and '_' as valid SQL characters (not boundaries) to maintain compatibility.
         */
        private static Pattern createEvilWordPattern(String evilWord) {
            if (evilWord.equals("--")) {
                // Special case for SQL comments - only match when not surrounded by valid SQL characters
                // This matches the original boundary logic for "--" 
                return Pattern.compile("(?i)(?<![a-zA-Z0-9_-])" + Pattern.quote(evilWord) + "(?![a-zA-Z0-9_-])");
            } else if (evilWord.equals(";")) {
                // Special case for semicolon - only match when not surrounded by valid SQL characters
                return Pattern.compile("(?i)(?<![a-zA-Z0-9_-])" + Pattern.quote(evilWord) + "(?![a-zA-Z0-9_-])");
            } else if (evilWord.endsWith(" ")) {
                // Words with trailing spaces - check for non-SQL-character before and space after
                String wordPart = evilWord.substring(0, evilWord.length() - 1);
                return Pattern.compile("(?i)(?<![a-zA-Z0-9_-])" + Pattern.quote(wordPart) + "\\s");
            } else {
                // Regular words - use negative lookbehind/lookahead for valid SQL characters
                // This matches the original isValidSQLCharacter logic (alphanumeric, -, _)
                return Pattern.compile("(?i)(?<![a-zA-Z0-9_-])" + Pattern.quote(evilWord) + "(?![a-zA-Z0-9_-])");
            }
        }

    // SECURITY: Rate limiting for security logging to prevent log flooding


    /**
     * SECURITY: Logs malicious SQL injection attempts securely for threat intelligence
     * while preventing information disclosure and log injection attacks.
     * 
     * @param suspiciousInput The potentially malicious input to log
     * @param detectedWord The specific evil word that was detected
     * @param sourceContext Additional context about the source (API endpoint, etc.)
     */
    private static void logSecurityThreat(final String suspiciousInput, final String detectedWord, final String sourceContext) {
        SecurityThreatLogger.logSQLInjectionAttempt(suspiciousInput, detectedWord, sourceContext);
    }



	private final static Set<String> ORDERBY_WHITELIST= ImmutableSet.of(
			"title","upper(title)","filename", "moddate", "tagname","pageUrl",
			"category_name","category_velocity_var_name","status","workflow_step.name","assigned_to",
			"mod_date","structuretype,upper(name)","upper(name)",
			"category_key", "page_url", "name", "velocity_var_name", "tree_order",
			"description","category_","sort_order","hostName", "keywords",
			"mod_date,upper(name)", "relation_type_value", "child_relation_name",
			"parent_relation_name","inode");

	/**
	 * SECURITY: Whitelist of allowed conditional column names for WHERE clauses
	 * Based on structure table columns and other legitimate database columns
	 * This prevents SQL injection in conditional statements
	 */
	private final static Set<String> CONDITIONAL_COLUMNS_WHITELIST = ImmutableSet.of(
			// Structure table columns
			"inode", "name", "description", "default_structure", "page_detail", "structuretype", 
			"system", "fixed", "velocity_var_name", "url_map_pattern", "host", "folder", 
			"expire_date_var", "publish_date_var", "mod_date", "icon", "sort_order", "marked_for_deletion",
			// Common additional columns
			"title", "upper(title)", "filename", "moddate", "tagname", "pageurl",
			"category_name", "category_velocity_var_name", "status", "assigned_to",
			"category_key", "page_url", "keywords", "upper(name)"
	);
	
	public static List<String> tokenize(String schema) {
		List<String> ret=new ArrayList<>();
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
				DbConnectionFactory.isMySql()){
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

		if(StringUtils.isBlank(parameter) || parameter.contains("null")){//first check if is null or empty, check if contains the word null (e.g null null)
			return StringPool.BLANK;
		}

		String testParam=parameter.replaceAll(_ASC, StringPool.BLANK)
				.replaceAll(_DESC, StringPool.BLANK)
				.replaceAll("-", StringPool.BLANK).toLowerCase();

		if(testParam.equals(StringPool.BLANK)){
			return testParam;
		}

		testParam = com.dotmarketing.util.StringUtils.convertCamelToSnake(testParam);
		testParam = translateSortBy(testParam);

		if (ORDERBY_WHITELIST.contains(testParam)) {
			if (parameter.contains("-")) {
				return "-" + testParam;
			} else if (parameter.contains(_DESC)) {
				return testParam + _DESC;
			} else {
				return testParam;
			}
		}

		Exception e = new DotStateException("Invalid or pernicious sql parameter passed in : " + parameter);
		Logger.error(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter, e);

		SecurityLogger.logDebug(SQLUtil.class, "Invalid or pernicious sql parameter passed in : " + parameter);
		return StringPool.BLANK;
	}

	/**
	 * Method to translate UI field into SQL columns naming
	 * @param parameter
	 * @return
	 */
	public  static String translateSortBy(String parameter){

		String result = "";
		result = parameter.replace("moddate","mod_date")
				.replace("categoryname","category_name")
				.replace("categoryvelocityvarname","category_velocity_var_name")
				.replace("categorykey","category_key")
				.replace("pageurl", "page_url")
				.replace("velocityvarname","velocity_var_name")
				.replace("sortorder","sort_order")
				.replace("hostname","hostName")
				.replace("relationtypevalue","relation_type_value")
				.replace("childrelationname","child_relation_name")
				.replace("parentrelationname","parent_relation_name");

			return  result;
	}

	/**
	 * Applies the sanitize to the parameter argument in order to avoid evil sql words for a PARAMETER.
	 * @param parameter SQL to filter.
	 * @return String with filtered SQL.
	 */
	public static String sanitizeParameter(String parameter){

        if(!UtilMethods.isSet(parameter)) { //check if is not null

            return StringPool.BLANK;
        }

        parameter = StringEscapeUtils.escapeSql( parameter );

	    return sanitizeSQL( parameter, EVIL_SQL_WORDS );
	} // sanitizeParameter.

    /**
     * Applies the sanitize to the parameter argument in order to avoid evil sql words for a CONDITION.
     * @param condition SQL to filter.
     * @return String with filtered SQL.
     */
    public static String sanitizeCondition(String condition){

        if(!UtilMethods.isSet(condition)) { //check if is not null

            return StringPool.BLANK;
        }

        return sanitizeSQL( condition, EVIL_SQL_CONDITION_WORDS );
    } // sanitizeCondition.

    /**
     * Util method to filter the parameter SQL with a list of EVIL WORDS not allowed.
     *
     * @param query SQL to filter.
     * @param evilWords words not allowed in the parameter SQL.
     * @return String with filtered SQL.
     */
    private static String sanitizeSQL( String query, final Set<String> evilWords) {

        final String parameterLowercase = query.toLowerCase();

        for(String evilWord : evilWords){

            // SECURITY: Use case-insensitive pattern matching to prevent bypass
            // Check for evil word patterns regardless of case variations
            Pattern patternToFind = EVIL_WORD_PATTERNS.get(evilWord);
            if (patternToFind != null && patternToFind.matcher(query).find()) {
                // SECURITY: Log attack details for threat intelligence (securely)
                logSecurityThreat(query, evilWord, "SQLUtil.sanitizeSQL:pattern-match");
                
                // SECURITY: Do not log user input to prevent information disclosure in standard logs
                final String message = "Invalid or pernicious sql parameter detected";
                Logger.error(SQLUtil.class, message, new DotStateException(message));
                securityLoggerServiceAPI.logInfo(SQLUtil.class, message);
                return StringPool.BLANK;
            }

            // Legacy boundary checking as backup (keep existing logic)
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

				// SECURITY: Log attack details for threat intelligence (securely)
				logSecurityThreat(query, evilWord, "SQLUtil.sanitizeSQL:boundary-check");
				
				// SECURITY: Do not log user input to prevent information disclosure in standard logs
				final String message = "Invalid or pernicious sql parameter detected";
				Logger.error(SQLUtil.class, message, new DotStateException(message));
				securityLoggerServiceAPI.logInfo(SQLUtil.class, message);

                return StringPool.BLANK;
            }
        }

        return query;
    }

    /**
	 * Determine if the character is a valid for sql
	 * @param c char
	 * @return boolean
	 */
	private static boolean isValidSQLCharacter (final char c) {

		return Character.isLetterOrDigit(c) || '-' == c || '_' == c;
	} // isValidSQLCharacter.

	/**
	 * Method to check if an exception is a Unique Constraint Exception
	 * It depends on the database engine. So far only Oracle and postgres has been implemented
	 * @param ex
	 * @return
	 */
	public static boolean isUniqueConstraintException (DotDataException ex) {
		if (ex != null && ex.getCause() instanceof SQLException) {
			final SQLException sqle =  (SQLException) ex.getCause();
			return (DbConnectionFactory.isOracle() && sqle.getSQLState().equals(ORACLE_SQL_STATE_UNIQUE_CONSTRAINT)) ||
					(DbConnectionFactory.isPostgres() && sqle.getSQLState().equals(POSTGRE_SQL_STATE_UNIQUE_CONSTRAINT));
		}
		return false;
	}

	/**
	 * Returns an immutable list of common evil SQL keywords that must not be included in SQL queries sent by custom
	 * code.
	 *
	 * @return The list of evil SQL keywords.
	 */
	public static Set<String> getEvilSqlConditionWords() {
		return EVIL_SQL_CONDITION_WORDS;
	}

	/**
	 * Returns the whitelist of allowed conditional column names for WHERE clauses.
	 * This prevents SQL injection in conditional statements by restricting columns to a safe set.
	 *
	 * @return The set of allowed conditional column names.
	 */
	public static Set<String> getConditionalColumnsWhitelist() {
		return CONDITIONAL_COLUMNS_WHITELIST;
	}

	/**
	 * Scans the SQL query passed down by the user/developer looking for evil SQL words, as only {@code SELECT} queries
	 * (data reading operations) are allowed.
	 *
	 * @param sqlQuery The SQL query.
	 *
	 * @return If the SQL query is safe, returns {@code true}. Otherwise, returns {@code false}.
	 */
	public static boolean containsEvilSqlWords(final String sqlQuery) {
		final String evilWord = getEvilSqlConditionWords().stream().filter(restrictedWord -> sqlQuery
				.contains(restrictedWord + " ")).findFirst().orElse(null);
		return UtilMethods.isSet(evilWord) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Takes the 'orderBy' and 'direction' parameters and returns a valid SQL order-by clause. If
	 * the 'orderBy' parameter is not set, the method will default to the 'mod_date' column in
	 * descending order instead.
	 *
	 * @param orderBy   The column name used to order the results by.
	 * @param direction The sort direction of the results.
	 *
	 * @return A valid SQL order-by clause.
	 */
	public static String getOrderByAndDirectionSql(final String orderBy, final OrderDirection direction) {
		final String ascOrder = OrderDirection.ASC.name().toLowerCase();
		final String descOrder = OrderDirection.DESC.name().toLowerCase();
		String orderByParam = UtilMethods.isSet(orderBy)
				? orderBy.trim().toLowerCase()
				: ContentTypeFactory.MOD_DATE_COLUMN + SPACE + descOrder;

		if (!orderByParam.endsWith(SPACE + ascOrder) && !orderByParam.endsWith(SPACE + descOrder)) {
			orderByParam = orderByParam + SPACE + (UtilMethods.isSet(direction)
					? direction.toString().toLowerCase()
					: ascOrder);
		}
		return orderByParam;
	}


}
