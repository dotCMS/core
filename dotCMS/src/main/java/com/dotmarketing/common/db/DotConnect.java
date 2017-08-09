package com.dotmarketing.common.db;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import com.dotcms.repackage.org.apache.commons.collections.map.LRUMap;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import static com.dotcms.util.CloseUtils.*;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import static com.dotcms.util.CollectionsUtils.*;

/**
 * Description of the Class
 * 
 * @author will
 * @created February 25, 2002 Modified May 8, 2003 Hashtables converted to
 *          HashMaps results is now an arraylist
 */
public class DotConnect {
    
	private static Map<Connection, Map<String, PreparedStatement>> stmts = new LRUMap(200);
	
	ArrayList<Object> paramList;

    ArrayList<Object> results;
    
    List<Map<String, Object>> objectResults;

    String SQL;

    boolean gotResult;

    int cursor;

    int maxRows = -1;

    int startRow = 0;
    
    boolean forceQuery=false;

    private static Map<Class, StatementObjectSetter> statementSetterHandlerMap =
            map(DotTimezonedTimestamp.class, DotConnect::setTimestampWithTimezone);

    public DotConnect() {
        Logger.debug(this, "------------ DotConnect() --------------------");
    }
    
    public void setForceQuery(boolean force) {
        forceQuery=force;
    }

    public int getInt(String x){
    	x = x.toLowerCase(); 
        Logger.debug(this, "getInt: " + x);

        if (gotResult == false) {
            getResult();
        }

        try {
            return Integer.parseInt((String) ((HashMap) results.get(cursor)).get(x));
        } catch (Exception e) {
            Logger.debug(this, "getInt: " + e);
            throw new DotRuntimeException(e.toString(),e);
        }
    }
    public int loadInt(String x) throws DotDataException{
    	x = x.toLowerCase(); 
        try {
            return (Integer) loadObjectResults().get(0).get(x);
        } catch (Exception e) {
            Logger.debug(this, "loadInt: " + e);
            throw new DotDataException(e.toString(),e);
        }
    }
    public DotConnect setMaxRows(int x) {
        maxRows = x;
        return this;
    }

    public DotConnect setMaxRows(String x) {
        try {
            setMaxRows(Integer.parseInt(x));
        } catch (Exception e) {
            throw new DotRuntimeException(e.toString(),e);
        }
        return this;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getNumRows() {
        return (results != null) ? results.size() : 0;
    }

    public Object getObject(String x) {
    	
        Object obj = new Object();

        try {
            obj = getObject(Class.forName(x).newInstance());
            return obj;
        } catch (Exception e) {
            Logger.error(this, "Create class Exception" + e, e);
            throw new DotRuntimeException(e.toString(),e);
        }
    }

    public Object getObject(Object x) {
        Logger.debug(this, "getObject(Object " + x.toString() + ")");

        //if we don't have a result set, get one
        if (gotResult == false) {
            getResult();
        }

        if (getNumRows() == 0) {
            return x;
        }

        HashMap o = (HashMap) results.get(cursor);

        for (Iterator e = o.entrySet().iterator(); e.hasNext();) {
            try {
                String colName = (String) e.next();
                java.lang.StringBuffer setter = new java.lang.StringBuffer();
                setter.append("set");

                if (colName.toString().length() > 1) {
                    java.util.StringTokenizer keySpliter = new java.util.StringTokenizer(colName, "_");

                    // Split on Underscore
                    while (keySpliter.hasMoreTokens()) {
                        String partKey = keySpliter.nextToken();
                        setter.append(partKey.substring(0, 1).toUpperCase() + partKey.substring(1));
                    }
                }

                Class[] params = { new String().getClass() };
                Object[] paramsObj = new Object[1];
                paramsObj[0] = (o.get(colName) == null) ? null : o.get(colName).toString();

                x.getClass().getMethod(setter.toString(), params).invoke(x, paramsObj);
            } catch (Exception ex) {
                Logger.error(this, "db.getObject: " + ex, ex);
                throw new DotRuntimeException(ex.toString(),ex);
            }
        }

        return x;
    }

    public Object[] getObjectArray(String ObjName) {
        //if we don't have a result set, get one
        if (gotResult == false) {
            getResult();
        }

        ArrayList<Object> bar = new ArrayList<Object>();

        for (cursor = 0; cursor < getNumRows(); cursor++) {
            bar.add(getObject(ObjName));
        }

        return bar.toArray();
    }

    public void getResult(String dataSource) {
	    gotResult = true;
	
	    //build the SQL statement, looping through params
	    try {
	    	executeQuery(dataSource);
	    } catch (Exception e) {
	    	Logger.error(this, "getResult(): unable to execute query.  Bad SQL? : " + (SQL != null ? SQL + " " : "") + (paramList != null ? paramList.toString() + " " : "") + e.getMessage(), e);
	        throw new DotRuntimeException(e.toString(),e);
	    }
	
	}
    
    public void getResult(Connection conn) {
	    gotResult = true;
	
	    //build the SQL statement, looping through params
	    try {
	    	executeQuery(conn);
	    } catch (Exception e) {
	    	Logger.error(this, "getResult(): unable to execute query.  Bad SQL? : " + (SQL != null ? SQL + " " : "") + (paramList != null ? paramList.toString() + " " : "") + e.getMessage(), e);
	        throw new DotRuntimeException(e.toString(),e);
	    }
	
	}
    
    public void loadResult(Connection conn) throws DotDataException{
	    gotResult = true;
	
	    //build the SQL statement, looping through params
	    try {
	    	executeQuery(conn);
	    } catch (Exception e) {
	    	throw new DotDataException(e.getMessage(),e);
	    }
	
	}
    
    public boolean executeStatement(String sql) throws SQLException{
    	Connection conn = DbConnectionFactory.getConnection();
    	Statement stmt = conn.createStatement();
    	Logger.info(this, "Executing " + sql);
    	boolean ret=stmt.execute(sql);
    	stmt.close();
    	return ret;
    }

    public boolean executeStatement(String sql, Connection con) throws SQLException{
    	Statement stmt = con.createStatement();
    	Logger.info(this, "Executing " + sql);
    	boolean ret=stmt.execute(sql);
    	Logger.info(this, "Finished Executing " + sql);
    	stmt.close();
    	return ret;
    }
    
    public void loadResult() throws DotDataException{
        gotResult = true;
        //build the SQL statement, looping through params
        try {
			executeQuery();
		} catch (Exception e) {
			throw new DotDataException(e.getMessage() + toString(),e);
		}
    }
    
	/**
     * @deprecated - Use loadResult instead as it throws the exception and doesn't swallow it 
     * Executes SQL, Pulls a result set, sets the rs and rsmd variables.
     */
    public void getResult() {
        gotResult = true;


        //build the SQL statement, looping through params
        try {
        	executeQuery();
        } catch (Exception e) {
            Logger.error(this, "getResult(): unable to execute query.  Bad SQL? : " + (SQL != null ? SQL + " " : "") + (paramList != null ? paramList.toString() + " " : "") + e.getMessage(), e);
            throw new DotRuntimeException(e.toString(),e);
        }
        

    }

    public DotConnect setSQL(String x) {
        cursor = 0;
        gotResult = false;
        paramList = new ArrayList<Object>();
        SQL = x;
        startRow = 0;
        maxRows = -1;
        
        Logger.debug(this, "setSQL: " + x);
        return this;
    }
    
    public DotConnect setSQL(String x, int limit) {
        if(DbConnectionFactory.isMsSql()){
        	x = x.trim();
            if(x.startsWith("select distinct"))
                setSQL(x.replaceFirst("select distinct", "select distinct top "+limit+" "));
            else
                setSQL(x.replaceFirst("select", "select top "+limit+" "));
        }
        else if(DbConnectionFactory.isOracle()) {
            setSQL("select * from ("+x+") where rownum<="+limit);
        }
        else {
            setSQL(x+" limit "+limit);
        }
        return this;
    }

    public String getSQL() {
        return SQL;
    }

    /**
     * Returns a single result as a String.
     * 
     * @param x
     *            Description of Parameter
     * @return The string value
     */
    public String getString(String x) {
        Logger.debug(this, "getString(String x)");
        x = x.toLowerCase();
        if (gotResult == false) {
            getResult();
        }

        try {
            if(results == null || results.isEmpty()) {
                return null;
            }

            return (String) ((HashMap) results.get(cursor)).get(x);
        } catch (Exception e) {
            throw new DotRuntimeException(e.toString(),e);
        }
    }

    //this is a specialize getUniqueID for MSSQL
    public static synchronized int getUniqueID(String type) {
        String SELECT_AUTONUM = "SELECT NextAutoNum as idx from webAutoNum where Code = ?";
        String UPDATE_AUTONUM = "UPDATE webAutoNum set NextAutoNum = ? where Code = ?";

        int x = 0;
        DotConnect db = new DotConnect();
        db.setSQL(SELECT_AUTONUM);
        db.addParam(type);
        x = db.getInt("idx");

        db.setSQL(UPDATE_AUTONUM);
        db.addParam(x + 1);
        db.addParam(type);
        db.getResult();

        try {
            return Integer.parseInt(Integer.toString(x) + "9");
        } catch (Exception e) {
            Logger.error(DotConnect.class, "ERROR: GET NOAH/WEB AUTONUMBER FAILED:" + e, e);
            throw new DotRuntimeException(e.toString(),e);
        }
    }

    /**
     * Returns the results.
     * @return ArrayList
     */
    public ArrayList loadResults(Connection conn) throws DotDataException{
        if (gotResult == false) {
            loadResult(conn);
        }
        return (results != null) ? results : new ArrayList();
    }
    
    /**
     * Returns the results.
     * @return ArrayList
     */
    public ArrayList loadResults() throws DotDataException{
        if (gotResult == false) {
            loadResult();
        }
        return (results != null) ? results : new ArrayList();
    }
    
    /**
     * Returns the results.
     * @deprecated - loadResults as it doesn't swallow the excpetion. 
     * @return ArrayList
     */
    public ArrayList getResults()throws DotDataException {
        if (gotResult == false) {
            getResult();
        }
        return (results != null) ? results : new ArrayList();
    }

	/**
     * Returns the results.
     * 
     * @return ArrayList
     */
    public ArrayList getResults(String dataSource)throws DotDataException {
        if (gotResult == false) {
            getResult(dataSource);
        }
        return (results != null) ? results : new ArrayList();
    }
    /**
     * Sets the startRow.
     * 
     * @param startRow
     *            The startRow to set
     */
    public DotConnect setStartRow(int startRow) {
        this.startRow = startRow;
        return this;
    }

    public DotConnect setStartRow(String x) {
        try {
            setStartRow(Integer.parseInt(x));
        } catch (Exception e) {
            throw new DotRuntimeException(e.toString(),e);
        }
        return this;
    }

    /**
     * Returns the startRow.
     * 
     * @return int
     */
    public int getStartRow() {
        return startRow;
    }

    public DotConnect addObject(Object x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    public DotConnect addParam(Object x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    /**
     * Adds a feature to the Param attribute of the dotConnect object
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(boolean x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (boolean): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    /**
     * This method adds an <code>int</code> parameter to the prepared SQL
     * statement.
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(int x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (int): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    /**
     * This method adds a <code>String</code> parameter to the prepared SQL
     * statement.
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(String x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (String): " + x);
        paramList.add(paramList.size(), x);
        return this;
        
    }

    /**
     * Adds a long parameter to the prepared SQL statement.
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(long x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (long): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    /**
     * Adds a double parameter to the prepared SQL statement.
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(double x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (double): " + x);
        paramList.add(paramList.size(), x);
        return this;
    }

    /**
     * Adds a date parameter to the prepared SQL statement.
     * 
     * @param x
     *            The feature to be added to the Param attribute
     */
    public DotConnect addParam(java.util.Date x) {
        Logger.debug(this, "db.addParam " + paramList.size() + " (date): " + x);
        paramList.add(paramList.size(), x!=null ? new Timestamp(x.getTime()) : x);
        return this;
    }

    private void executeQuery() throws SQLException{
        Connection conn = DbConnectionFactory.getConnection();
        executeQuery(conn);
    }
    
    private void executeQuery(String dataSource) throws SQLException{
    	Connection conn = DbConnectionFactory.getConnection(dataSource);
        executeQuery(conn);
    }

    /*
     * Depending on the query, this method will call the execute (for update operations: DELETE, UPDATE, INSERT)
     * or executeQuery for Select operations.
     * In addition will see if the configuration want's to use cache prepared statements
     * Finally wrap the results on this.objectResults abd this.results.
     */
    private void executeQuery(Connection conn) throws SQLException{
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        results = new ArrayList<Object>();
        objectResults= new ArrayList <Map<String, Object>>();
        Statement stmt = null;
        //perform some query optimizations
        String starter = SQL.substring(0, 10);
        PreparedStatement statement = null;
        boolean cachePreparedStatement=Config.getBooleanProperty("CACHE_PREPARED_STATEMENTS", false);
	    boolean logSQL = "true".equals(System.getProperty("hibernate.show_sql"));
	    if(logSQL){
	    	
	    	Logger.info(this.getClass(), SQL + " params:" + paramList);
	    }
        try {
	        conn.clearWarnings();
	        long before = System.nanoTime();
	        long beforeMetadata = 0;
	        long afterMetadata = 0;
	        long beforeQueryExecution = 0;
	        long afterQueryExecution = 0;
	        long beforePreparation = 0;
	        long afterPreparation = 0;
	        if(SQL.contains("?")){ // if it is a prepared statement
	        	if (cachePreparedStatement) { // if want's to use the cache map
	        		Map<String,PreparedStatement> m = null;
		        	Connection rconn = conn.getMetaData().getConnection();
		        	m = stmts.get(rconn);
		        	if(m!=null){ // if it is in the cache.
		        		if (starter.toLowerCase().trim().indexOf("call")!=-1) { // if it is a stored procedure
		        		   statement = (CallableStatement)m.get(SQL);
		        		}else{
		        			statement = m.get(SQL);
		        		}
		        	}else{ // if it is not in the cache, put it there.
		        		m = new HashMap<String, PreparedStatement>();
		        		synchronized (stmts) {
		        			stmts.put(rconn, m);
		        		}
		        	}
		        	if(statement == null){ // if couldn't create it
		        		beforePreparation = System.nanoTime();
		        		if (starter.toLowerCase().trim().indexOf("call")!=-1) { // if it is a stored procedure
		        		  statement = (CallableStatement)conn.prepareCall(SQL);
		        		}else{
		        		  statement = rconn.prepareStatement(SQL);
		        		}
		        		m.put(SQL, statement);
		        		afterPreparation = System.nanoTime();
		        	}
		        	
	        	} else {
	        		beforePreparation = System.nanoTime();
	        		if (starter.toLowerCase().trim().indexOf("call")!=-1) {
	        			statement = (CallableStatement)conn.prepareCall(SQL);
	        		}else{
		        	  statement = conn.prepareStatement(SQL);
	        		}
	        		afterPreparation = System.nanoTime();
	        	}

	        	//statement.setMaxRows(maxRows);
		        Logger.debug(this, "SQL = " + statement.toString());
		        for (int i = 0; i < paramList.size(); i++) {
		            Object param = paramList.get(i);
		            if(param!=null && statementSetterHandlerMap.containsKey(param.getClass())) {
		                statementSetterHandlerMap.get(param.getClass()).execute(statement, i+1, param);
                    } else {
                        statement.setObject(i + 1, paramList.get(i));
                    }
		        }
				if (!starter.toLowerCase().trim().contains("select")) { // if it is NOT a read operation
		        	beforeQueryExecution = System.nanoTime();
		            statement.execute();
		            rs = statement.getResultSet();
		            afterQueryExecution = System.nanoTime();
		            if(rs!=null){
		            	beforeMetadata = afterQueryExecution;
		            	rsmd = rs.getMetaData();
		            	afterMetadata = System.nanoTime();
		            }
		        } else { // if it is a SELECT
		        	beforeQueryExecution = System.nanoTime();
		            rs = statement.executeQuery();
		            afterQueryExecution = System.nanoTime();
		            beforeMetadata = afterQueryExecution;
		            rsmd = rs.getMetaData();
		            afterMetadata = System.nanoTime();
		        }
	        }else{
	        	beforePreparation = System.nanoTime();
	        	stmt = conn.createStatement();
	        	afterPreparation = System.nanoTime();
	        	
	        	if (!starter.toLowerCase().trim().contains("select") && !forceQuery) {
		        	beforeQueryExecution = System.nanoTime(); // todo: shouldn't get the resultset and metadata?
		            stmt.execute(SQL);
		            afterQueryExecution = System.nanoTime();
		        } else {
		        	beforeQueryExecution = System.nanoTime();
		            rs = stmt.executeQuery(SQL);
		            afterQueryExecution = System.nanoTime();
		            beforeMetadata = afterQueryExecution;
		            rsmd = rs.getMetaData();
		            afterMetadata = System.nanoTime();
		        }
	        }

			long after = System.nanoTime();
			if((float)((after - before) / 1000000F) > 1000F) {
				Logger.debug(this, "Somewhat slow query, " +
						"total time: " + ((float)(after - before) / 1000000F) +
						"ms, query preparation time: " + ((float)(afterPreparation - beforePreparation) / 1000000F) + 
						"ms, query execution time: " + ((float)(afterQueryExecution - beforeQueryExecution) / 1000000F) + 
						"ms, metadata time: " + ((float)(afterMetadata - beforeMetadata) / 1000000F) + 
						"ms, SQL: " + SQL + ", parameters: " + paramList.toString());
			}
				if((float)((after - before) / 1000000F) > 3000F) {
					Logger.warn(this, "Somewhat slow query, " +
							"total time: " + ((float)(after - before) / 1000000F) +
							"ms, query preparation time: " + ((float)(afterPreparation - beforePreparation) / 1000000F) + 
							"ms, query execution time: " + ((float)(afterQueryExecution - beforeQueryExecution) / 1000000F) + 
							"ms, metadata time: " + ((float)(afterMetadata - beforeMetadata) / 1000000F) + 
							"ms, SQL: " + SQL + ", parameters: " + paramList.toString());
			}
			
	        if (rs != null) {
	            //move to the starter row
	            for (int i = 0; i < startRow; i++) {
	                rs.next();
	            }
	
	            int i = 0;
	
	            while (rs.next() && (maxRows <= 0 || i < maxRows)) {
	                HashMap<String,String> vars = new HashMap<String,String>();
	                HashMap<String,Object> objvars=new HashMap<String,Object>();
	
	                for (int j = 1; j <= rsmd.getColumnCount(); j++) {
	                    String x = rsmd.getColumnLabel(j) + "";
	                    try{
		                    if ((rs.getString(x) == null) || rs.getString(x).equals("null")) {
		                    	x=x.toLowerCase();
		                    	vars.put(x, "");
		                    	
		                    	if(rs.getObject(x) instanceof java.sql.Clob){
		                    		objvars.put(x, rs.getString(x));
		                    	}else if( DbConnectionFactory.isOracle() && rs.getObject(x) instanceof oracle.sql.TIMESTAMP){
		                    		objvars.put(x,new Date(((oracle.sql.TIMESTAMP) rs.getObject(x)).timeValue().getTime()));
		                    	}
		                    	else{
		                    		objvars.put(x, rs.getObject(x));
		                    	}
		                    } else {
		                    	x=x.toLowerCase();
		                        vars.put(x, rs.getString(x) + "");
		                        
		                    	if(rs.getObject(x) instanceof java.sql.Clob){
		                    		objvars.put(x, rs.getString(x));
		                    	}else if( DbConnectionFactory.isOracle() && rs.getObject(x) instanceof oracle.sql.TIMESTAMP){
		                    		objvars.put(x,new Date(((oracle.sql.TIMESTAMP) rs.getObject(x)).timestampValue().getTime()));
		                    	}
		                    	else{
		                    		objvars.put(x, rs.getObject(x));
		                    	}
		                        
		                        
		                        //objvars.put(x, rs.getObject(x));
		                    }
	                    }catch (SQLException e) {
							Logger.error(this, "This is usually caused by bad data in the db Setting RS column value to empty: " + e.getMessage(), e);
							x=x.toLowerCase();
							vars.put(x, "");
							objvars.put(x,null);
						}
	                }
	                vars.put("rownumber", Integer.toString(i));
	                objvars.put("rownumber", i);
	                vars.put("oddoreven", Integer.toString((i % 2)));
	                objvars.put("oddoreven", (i % 2));
	                results.add(vars);
	                objectResults.add(objvars);
	                i++;
	            }
	        }
        } finally {
        	try { if (rs != null) rs.close(); } catch (Exception e) { }
        	try { if (stmt != null) stmt.close(); } catch (Exception e) { }
        	try { if ( (!cachePreparedStatement) && (statement!= null) ) statement.close(); } catch (Exception e) { }
        }

    }
    
    
    /**
     * Returns the correct Concatenation SQL syntax for a particular RDBMS
     * @param elements
     * @return result
     */
    
    
    public static String concat(String[] elements) {
        
        StringBuffer result = new StringBuffer(); 
        int size = elements.length;

        if (DbConnectionFactory.isMySql()) {
           result.append("CONCAT(");
             for(int i = 0; i < size; i++ ) {
            result.append(elements[i]);
              if( i + 1 < size ) result.append(",");
           }
           result.append(")");
           } else if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle() || DbConnectionFactory.isH2()) {
               for(int i = 0; i < size; i++ ) {
                 result.append(elements[i]);
               if( i + 1 < size ) result.append(" || ");
               }
           } else if (DbConnectionFactory.isMsSql()) {
             for(int i = 0; i < size; i++ ) {
              result.append(elements[i]);
               if( i + 1 < size ) result.append(" + ");
          }
         }

        return result.toString();
       }
    
    
    /**
     * Returns the correct bit AND operation syntax for a particular RDBMS
     * @param elements
     * @return result
     */
    
    
    public static String bitAND(String op1, String op2) {

		StringBuilder result = new StringBuilder();

		if (DbConnectionFactory.isOracle()) {
			result.append("BITAND(");
			result.append(op1);
			result.append(",");
			result.append(op2);
			result.append(")");
		} else {
			result.append("(");
			result.append(op1);
			result.append(" & ");
			result.append(op2);
			result.append(")");
		}

		return result.toString();
	}
    
    
    /**
     * Returns the correct bit OR operation syntax for a particular RDBMS
     * @param elements
     * @return result
     */
    
    
    public static String bitOR(String op1, String op2) {

		StringBuilder result = new StringBuilder();

		if (DbConnectionFactory.isOracle()) {
			result.append("(");
			result.append(op1);
			result.append(" + ");
			result.append(op2);
			result.append( " - " );
			result.append("BITAND(");
			result.append(op1);
			result.append(",");
			result.append(op2);
			result.append(")");
			result.append(")");
		} else {
			result.append("(");
			result.append(op1);
			result.append(" | ");
			result.append(op2);
			result.append(")");
		}

		return result.toString();
	}
    
    /**
     * Returns the correct bit XOR operation syntax for a particular RDBMS
     * @param elements
     * @return result
     */
    
    
    public static String bitXOR(String op1, String op2) {

		StringBuilder result = new StringBuilder();

		if (DbConnectionFactory.isOracle()) {
			result.append("BITXOR(");
			result.append(op1);
			result.append(",");
			result.append(op2);
			result.append(")");
		} else if(DbConnectionFactory.isPostgres()) {
			result.append("(");
			result.append(op1);
			result.append(" # ");
			result.append(op2);
			result.append(")");
		} else {
			result.append("(");
			result.append(op1);
			result.append(" ^ ");
			result.append(op2);
			result.append(")");
		}

		return result.toString();
	}
    
    /**
     * Returns the correct bit XOR operation syntax for a particular RDBMS
     * @param elements
     * @return result
     */
    
    
    public static String bitNOT(String op1) {

		StringBuilder result = new StringBuilder();

		if (DbConnectionFactory.isOracle()) {
			result.append("BITNOT(");
			result.append(op1);
			result.append(")");
		} else  {
			result.append("(~");
			result.append(op1);
			result.append(")");
		}

		return result.toString();
	}
    

    /**
     * Returns the object results.
     * @return ArrayList
     */
    public List<Map<String, Object>> loadObjectResults(Connection conn) throws DotDataException{
        if (gotResult == false) {
            loadResult(conn);
        }
        return (objectResults != null) ? objectResults : new ArrayList <Map<String, Object>>();
    }
    
    /**
     * Returns the object results.
     * @return ArrayList
     */
    public List<Map<String, Object>> loadObjectResults() throws DotDataException{
        if (gotResult == false) {
            loadResult();
        }
        return (objectResults != null) ? objectResults : new ArrayList <Map<String, Object>>();
    }
    
    /**
     * Returns the object results.
     * @deprecated - loadResults as it doesn't swallow the excpetion. 
     * @return ArrayList
     */
    public List<Map<String, Object>> getObjectResults()throws DotDataException {
        if (gotResult == false) {
            getResult();
        }
        return (objectResults != null) ? objectResults : new ArrayList <Map<String, Object>>();
    }

	/**
     * Returns the object results.
     * 
     * @return ArrayList
     */
    public List<Map<String, Object>> getObjectResults(String dataSource)throws DotDataException {
        if (gotResult == false) {
            getResult(dataSource);
        }
        return (objectResults != null) ? objectResults : new ArrayList <Map<String, Object>>();
    }

	/**
	 * Returns the number of records that exist in the specified table. This
	 * method is useful given that different databases return the count value as
	 * different Java objects (e.g., {@code BigDecimal} for Oracle, and
	 * {@code Long} for other databases).
	 * 
	 * @param tableName
	 *            - The name of the database table.
	 * @return The number of records in the specified table.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
 	public Long getRecordCount(String tableName) throws DotDataException {
		return getRecordCount(tableName, "");
	}
 	public Long getRecordCount(String tableName, String whereClause) throws DotDataException {
 		Long recordCount = 0L;
		setSQL("SELECT COUNT(*) AS count FROM " + tableName +" "+ whereClause);
		if (DbConnectionFactory.isOracle()) {
			BigDecimal result = (BigDecimal) loadObjectResults().get(0).get("count");
			recordCount = new Long(result.toPlainString());
		} else if (DbConnectionFactory.isMsSql()) {
			Integer result = (Integer) loadObjectResults().get(0).get("count");
			recordCount = new Long(result.toString());
		} else {
			recordCount = (Long) loadObjectResults().get(0).get("count");
		}
		return recordCount;
	}

    /**
     * It will create a String like (?,?,?) depending on the number of Parameters.
     *
     * @param numberParameters how many '?' you want.
     * @return
     */
    public static String createParametersPlaceholder(int numberParameters){
        String parameterPlaceholders = "";

        if(numberParameters > 0){
            parameterPlaceholders = StringUtils.repeat(",?", numberParameters).substring(1);
        }
        return parameterPlaceholders;
    }


	@Override
	public String toString() {
		try{
			return asJson().toString(2);
		} catch (JSONException e) {
			return super.toString();
		}
	}
    
    private JSONObject asJson() throws JSONException{
		JSONObject json = new JSONObject();

			json.append("SQL", getSQL());

			if(paramList!=null){
				for(int i=0;i<paramList.size();i++){
					json.accumulate("params", paramList.get(i));
				}
			}
			json.append("maxRows", getMaxRows());
			json.append("offest", getStartRow());
			
			return json;

    }
    

    /**
     * Executes in batch transaction, with N copies of the "sql". How many times will be added to the batch will depend on the "listOfParams" size.
     *
     * Will returns an array with the integer result of how many rows were affected for each row updated.
     *
     * Pre: all {@link Params}.size() should be the same on the "listOfParams",
     * the first param in the list will be taken as a default to set statement parameter over the rest of the
     * params in the collection.
     *
     * @param sql {@link String}
     * @param listOfParams {@link Collection} of {@link Params}
     * @return int []
     */
    public int [] executeBatch (final String sql, final Collection<Params> listOfParams) throws DotDataException {

        int [] results = null;
        boolean previousAutocommit = true;
        Connection conn = null;
        PreparedStatement preparedStatement = null;

        try {

            conn = DbConnectionFactory.getConnection();
            preparedStatement = conn.prepareStatement(sql);

            previousAutocommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            if (null != listOfParams) {

                for (Params params: listOfParams) {

                    if (null != params) {

                        this.setParams (preparedStatement, params);
                        preparedStatement.addBatch();
                    }
                }

                results = preparedStatement.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {

            try {

                conn.rollback();
            } catch (SQLException e1) {

                if (Logger.isErrorEnabled(this.getClass())) {

                    Logger.error(this.getClass(), e.getMessage(), e);
                }

                throw new DotDataException("SQL Error doing a batch and couldn't rollback", e);
            }

            throw new DotDataException("SQL Error doing a batch", e);
        } finally {

            try {

                conn.setAutoCommit(previousAutocommit);
            } catch (SQLException e) {

                if (Logger.isErrorEnabled(this.getClass())) {

                    Logger.error(this.getClass(), e.getMessage(), e);
                }
            }

            closeQuietly(preparedStatement, conn);
        }

        return results;
    } // executeBatch.

    private void setParams(final PreparedStatement preparedStatement,
                           final Params params) throws SQLException {

        for (int i = 0; i < params.size(); ++i) {

            preparedStatement.setObject(i+1, params.get(i));
        }
    }

    private void setParams(final PreparedStatement preparedStatement,
                           final Object... params) throws SQLException {

        for (int i = 0; i < params.length; ++i) {

            preparedStatement.setObject(i+1, params[i]);
        }
    }

    /**
     * Executes an update operation for a preparedStatement, returns the number of affected rows.
     * If the connection is get from a transaction context, will used it. Otherwise will create and handle an atomic transaction.
     * @param preparedStatement String
     * @param parameters Object array of parameters for the preparedStatement (if it does not have any, can be null). Not any checking of them
     * @return int rows affected
     * @throws DotDataException
     */
    public int executeUpdate (final String preparedStatement,
                              final Object... parameters) throws DotDataException {

        final boolean isNewTransaction = DbConnectionFactory.startTransactionIfNeeded();
        Connection connection = null;
        int rowsAffected = 0;

        try {

            connection   =  DbConnectionFactory.getConnection();
            rowsAffected =  this.executeUpdate(connection,
                    preparedStatement, parameters);

            if (isNewTransaction) {
                connection.commit();
            }
        } catch (DotDataException e) {

            if (isNewTransaction) {
                this.rollback(connection);
            }
           throw e;
        } catch (Exception e) {

            Logger.error(DotConnect.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        } finally {

            if (isNewTransaction) {
                closeQuietly(connection);
            }
        }

        return rowsAffected;
    } // executeUpdate.

    protected void rollback(final Connection connection) throws DotDataException {

        if (null != connection) {

            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        }
    } // rollback.

    /**
     * Executes an update operation for a preparedStatement
     * @param connection {@link Connection}
     * @param preparedStatementString String
     * @param parameters Object array of parameters for the preparedStatement (if it does not have any, can be null). Not any checking of them
     * @return int rows affected
     * @throws DotDataException
     */
    public int executeUpdate (final Connection connection, final String preparedStatementString,
                              final Object... parameters) throws DotDataException {


        PreparedStatement preparedStatement = null;
        int rowsAffected = 0;

        try {

            preparedStatement = connection.prepareStatement(preparedStatementString);
            this.setParams(preparedStatement, parameters);
            rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e) {

            Logger.error(DotConnect.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        } finally {

            closeQuietly(preparedStatement);
        }

        return rowsAffected;
    } // executeUpdate.

    private static void setTimestampWithTimezone(PreparedStatement statement, int parameterIndex, Object timestamp) {

        final DotTimezonedTimestamp dotTimezonedTimestamp = (DotTimezonedTimestamp) timestamp;
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(dotTimezonedTimestamp.getTimezone()));

        try {
            statement.setTimestamp(parameterIndex, dotTimezonedTimestamp.getTimestamp(), calendar);
        } catch(SQLException e) {
            Logger.error(DotConnect.class, "Error setting Timestamp to PreparedStatement. " +
                    "Parameter Index " + parameterIndex + "; Timestamp: " + timestamp, e);
        }
    }

}