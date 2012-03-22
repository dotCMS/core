package com.dotmarketing.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

/**
 * This class alows user to run store procedures
 * @author Oswaldo
 *
 */
public class DotStoredProcedure {

	ArrayList<HashMap<String, Object>> paramList;
	ArrayList<Map<String, String>> results;
	String SQL;
	String procedureName;

	boolean gotResult;
	int cursor;
	int maxRows = 10000;
	int startRow = 0;
	
	private final String inputValueKey = "inputValue";
	private final String outputTypeKey = "outputType";
	public final int returnValue = 1;


	public DotStoredProcedure() {
		Logger.debug(this, "------------ DotStoredProcedure() --------------------");
	}

	@Deprecated
	public int getInt(String x) {
		x = x.toLowerCase(); 
		Logger.debug(this, "getInt: " + x);

		if (gotResult == false) {
			getResult();
		}

		try {
			return Integer.parseInt((String) ((HashMap) results.get(cursor)).get(x));
		} catch (Exception e) {
			Logger.debug(this, "getInt: " + e);
			throw new DotRuntimeException(e.toString());
		}
	}

	public void setMaxRows(int x) {
		maxRows = x;
	}

	public void setMaxRows(String x) {
		try {
			setMaxRows(Integer.parseInt(x));
		} catch (Exception e) {
			throw new DotRuntimeException(e.toString());
		}
	}

	public int getMaxRows() {
		return maxRows;
	}

	public int getNumRows() {
		return (results != null) ? results.size() : 0;
	}

	@Deprecated
	public Object getObject(String x) {

		Object obj = new Object();

		try {
			obj = getObject(Class.forName(x).newInstance());
			return obj;
		} catch (Exception e) {
			Logger.error(this, "Create class Exception" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	@Deprecated
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
				throw new DotRuntimeException(e.toString());
			}
		}

		return x;
	}

	@Deprecated
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

	@Deprecated
	public void getResult(String dataSource) {
		Connection conn = DbConnectionFactory.getConnection(dataSource);
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		gotResult = true;

		results = new ArrayList<Map<String, String>>();

		//build the SQL statement, looping through params
		try {
			executeQuery(conn, statement, rsmd, false);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}

		statement = null;
		rsmd = null;
		conn = null;
	}

	/**
	 * Executes SQL, Pulls a result set, sets the rs and rsmd variables.
	 */
	@Deprecated
	public void getResult() {
		Connection conn = DbConnectionFactory.getConnection();
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		gotResult = true;

		//build the SQL statement, looping through params
		try {
			executeQuery(conn, statement, rsmd, false);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}

		statement = null;
		rsmd = null;
		conn = null;
	}

	@Deprecated
	public void setSQL(String x) {
		cursor = 0;
		gotResult = false;
		paramList = new ArrayList<HashMap<String, Object>>();
		SQL = x;

		Logger.debug(this, "setSQL: " + x);
	}
	
	/*
	 * Prepare the procedure sql to be called.
	 * 
	 * @param procedurename String with the procedure name to be called.
	 * @param numParam int with the number of parameters needed for the stored procedure.
	 */
	public void setProcedure(String procedureName, int numParam) {
		cursor = 0;
		gotResult = false;
		paramList = new ArrayList<HashMap<String, Object>>();
		String params ="";
		
		while(numParam > 0){
			params = params+",?";
			numParam--;
		}
		
		SQL = "{call " + procedureName + " (" + params.substring(1) + ")}";

		Logger.debug(this, "setSQL: " + SQL);
	}
	
	/*
	 * Prepare the function sql to be called.
	 * 
	 * @param functionName String with the function name to be called.
	 * @param numParam int with the number of parameters needed for the stored procedure. The return value will be considered a parameter too. 
	 */
	public void setFunction(String functionName, int numParam) {
		cursor = 0;
		gotResult = true;
		paramList = new ArrayList<HashMap<String, Object>>();
		String params ="";
		
		if (0 < numParam)
			numParam--;
		while(numParam > 0){
			params = params+",?";
			numParam--;
		}
		
		SQL = "{? = call " + functionName + " (" + params.substring(1) + ")}";

		Logger.debug(this, "setSQL: " + SQL);
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
	@Deprecated
	public String getString(String x) {
		Logger.debug(this, "getString(String x)");
		x = x.toLowerCase();
		if (gotResult == false) {
			getResult();
		}

		try {
			return (String) ((HashMap) results.get(cursor)).get(x);
		} catch (Exception e) {
			throw new DotRuntimeException(e.toString());
		}
	}

	/**
	 * Returns the results.
	 * 
	 * @return ArrayList
	 */
	@Deprecated
	public List<Map<String,String>> getResults() {
		if (gotResult == false) {
			Connection conn = DbConnectionFactory.getConnection();
			CallableStatement statement = null;
			ResultSetMetaData rsmd = null;
			gotResult = true;

			//build the SQL statement, looping through params
			try {
				executeQuery(conn, statement, rsmd, true);
			} catch (Exception e) {
				Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
				throw new DotRuntimeException(e.toString());
			} finally {
				try { if (statement != null) statement.close(); } catch (Exception e) { }
			}

			statement = null;
			rsmd = null;
			conn = null;
		}
		return (results != null) ? results : new ArrayList<Map<String,String>>();
	}

	/**
	 * Returns the results.
	 * 
	 * @return ArrayList
	 */
	@Deprecated
	public List<Map<String,String>> getResults(String dataSource) {
		if (gotResult == false) {
			Connection conn = DbConnectionFactory.getConnection(dataSource);
			CallableStatement statement = null;
			ResultSetMetaData rsmd = null;
			gotResult = true;

			results = new ArrayList<Map<String, String>>();

			//build the SQL statement, looping through params
			try {
				executeQuery(conn, statement, rsmd, true);
			} catch (Exception e) {
				Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
				throw new DotRuntimeException(e.toString());
			} finally {
				try { if (statement != null) statement.close(); } catch (Exception e) { }
			}

			statement = null;
			rsmd = null;
			conn = null;
		}
		return (results != null) ? results : new ArrayList<Map<String,String>>();
	}
	/**
	 * Sets the startRow.
	 * 
	 * @param startRow
	 *            The startRow to set
	 */
	public void setStartRow(int startRow) {
		this.startRow = startRow;
	}

	public void setStartRow(String x) {
		try {
			setStartRow(Integer.parseInt(x));
		} catch (Exception e) {
			throw new DotRuntimeException(e.toString());
		}
	}

	/**
	 * Returns the startRow.
	 * 
	 * @return int
	 */
	public int getStartRow() {
		return startRow;
	}

	public void addObject(Object x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		paramList.add(paramList.size(), param);
	}

	public void addParam(Object x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a feature to the Param attribute of the dotConnect object
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(boolean x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (boolean): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		paramList.add(paramList.size(), param);
	}

	/**
	 * This method adds an <code>int</code> parameter to the prepared SQL
	 * statement.
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(int x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (int): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		paramList.add(paramList.size(), param);
	}

	/**
	 * This method adds a <code>String</code> parameter to the prepared SQL
	 * statement.
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(String x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (String): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a long parameter to the prepared SQL statement.
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(long x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (long): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a double parameter to the prepared SQL statement.
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(double x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (double): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a date parameter to the prepared SQL statement.
	 * 
	 * @param x
	 *            The feature to be added to the Param attribute
	 */
	public void addParam(java.util.Date x) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (date): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, new Timestamp(x.getTime()));
		paramList.add(paramList.size(), param);
	}
	
	public void registerOutParameter(int paramType) {
		Logger.debug(this, "db.registerOutParameter " + paramList.size() + " (type): " + paramType);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}
	
	public void addInOutObject(Object x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	public void addInOutParam(Object x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (Object): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a feature to the Param attribute of the dotConnect object and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addInOutParam(boolean x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (boolean): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * This method adds an <code>int</code> parameter to the prepared SQL statement and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addInOutParam(int x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (int): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * This method adds a <code>String</code> parameter to the prepared SQL statement and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addInOutParam(String x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (String): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x);
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a long parameter to the prepared SQL statement and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addParam(long x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (long): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a double parameter to the prepared SQL statement and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addParam(double x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (double): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, x + "");
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}

	/**
	 * Adds a date parameter to the prepared SQL statement and register the same attribute as an output
	 * 
	 * @param x	The feature to be added to the Param attribute
	 * @param paramType Output parameter type
	 */
	public void addParam(java.util.Date x, int paramType) {
		Logger.debug(this, "db.addParam " + paramList.size() + " (date): " + x);
		HashMap<String, Object> param = new HashMap<String, Object>();
		param.put(inputValueKey, new Timestamp(x.getTime()));
		param.put(outputTypeKey, paramType);
		paramList.add(paramList.size(), param);
	}
	
	@Deprecated
	private void executeQuery(Connection conn, CallableStatement statement, ResultSetMetaData rsmd, boolean getResultSet)throws SQLException{

		conn.clearWarnings();
		statement = conn.prepareCall(SQL);
		Logger.debug(this, "SQL = " + statement.toString());
		
		HashMap<String, Object> param;
		Object inputValue;
		Integer outputType;
		
		for (int i = 0; i < paramList.size(); i++) {
			param = paramList.get(i);
			inputValue = param.get(inputValueKey);
			if (inputValue != null)
				statement.setObject(i + 1, inputValue);
			outputType = (Integer) param.get(outputTypeKey);
			if (outputType != null)
				statement.registerOutParameter(i + 1, outputType);
		}
		
		ResultSet rs = null;
		if (getResultSet)
			rs = statement.executeQuery();
		else
			statement.execute();
		
		if (rs != null) {
			
			results = new ArrayList<Map<String,String>>();
			
			rsmd = rs.getMetaData();
			
			//move to the starter row
			for (int i = 0; i < startRow; i++) {
				rs.next();
			}
			;

			int i = 0;

			while (rs.next() && (i < maxRows)) {
				HashMap<String,String> vars = new HashMap<String,String>();

				for (int j = 1; j <= rsmd.getColumnCount(); j++) {
					String x = rsmd.getColumnName(j) + "";

					if ((rs.getString(x) == null) || rs.getString(x).equals("null")) {
						x=x.toLowerCase();
						vars.put(x, "");
					} else {
						x=x.toLowerCase();
						vars.put(x, rs.getString(x) + "");
					}
				}

				vars.put("rownumber", Integer.toString(i));
				vars.put("oddoreven", Integer.toString((i % 2)));
				results.add(vars);

				i++;
			}
		}
	}
	
	/**
	 * Execute the configure stored procedure with the default data source
	 */
	public void executeStoredProcedure() {
		Connection conn = DbConnectionFactory.getConnection();
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		
		try {
			executeStoredProcedure(conn, statement, rsmd, false);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}
		
		statement = null;
		rsmd = null;
		conn = null;
	}

	/**
	 * Execute the configure stored procedure with a specific data source
	 * 
	 * @param dataSource String
	 */
	public void executeStoredProcedure(String dataSource) {
		Connection conn = DbConnectionFactory.getConnection(dataSource);
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		
		try {
			executeStoredProcedure(conn, statement, rsmd, false);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}
		
		statement = null;
		rsmd = null;
		conn = null;
	}
	
	/**
	 * Returns the results of the configure stored procedure with the default data source.
	 * 
	 * @return HashMap<Integer, Object> The integer key will the parameter position declared in the stored procedure. The first parameter is 1. In the case of a function, the first parameter is the return value. 
	 */
	public HashMap<Integer, Object> getStoredProcedureResults() {
		HashMap<Integer, Object> result = null;
		Connection conn = DbConnectionFactory.getConnection();
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		
		try {
			 result = executeStoredProcedure(conn, statement, rsmd, true);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}
		
		statement = null;
		rsmd = null;
		conn = null;
		
		return result;
	}

	/**
	 * Returns the results of the configure stored procedure with a specific data source.
	 * 
	 * @param dataSource String
	 * @return HashMap<Integer, Object> The integer key will the parameter position declared in the stored procedure. The first parameter is 1. In the case of a function, the first parameter is the return value.
	 */
	public HashMap<Integer, Object> getStoredProcedureResults(String dataSource) {
		HashMap<Integer, Object> result = null;
		Connection conn = DbConnectionFactory.getConnection(dataSource);
		CallableStatement statement = null;
		ResultSetMetaData rsmd = null;
		
		results = new ArrayList<Map<String, String>>();
		
		try {
			result = executeStoredProcedure(conn, statement, rsmd, true);
		} catch (Exception e) {
			Logger.error(this, "getResult(): unable to execute query.  Bad SQL?:" + e, e);
			throw new DotRuntimeException(e.toString());
		} finally {
			try { if (statement != null) statement.close(); } catch (Exception e) { }
		}
		
		statement = null;
		rsmd = null;
		conn = null;
		
		return result;
	}
	
	private HashMap<Integer, Object> executeStoredProcedure(Connection conn, CallableStatement statement, ResultSetMetaData rsmd, boolean getResultSet) throws SQLException {
		HashMap<Integer, Object> result = new HashMap<Integer, Object>();
		
		conn.clearWarnings();
		statement = conn.prepareCall(SQL);
		Logger.debug(this, "SQL = " + statement.toString());
		
		HashMap<String, Object> param;
		Object inputValue;
		Integer outputType;
		
		for (int i = 0; i < paramList.size(); i++) {
			param = paramList.get(i);
			inputValue = param.get(inputValueKey);
			if (inputValue != null)
				statement.setObject(i + 1, inputValue);
			outputType = (Integer) param.get(outputTypeKey);
			if (outputType != null)
				statement.registerOutParameter(i + 1, outputType);
		}
		
		ResultSet rs = null;
		if (getResultSet)
			rs = statement.executeQuery();
		else
			statement.execute();
		
		if (rs != null) {
			try {
				rsmd = rs.getMetaData();
				ArrayList<Map<String,String>> results = convertResultSetToArrayList(rs);
				
				result.put(returnValue, results);
			} catch (Exception e) {
			}
		}
		
		if (getResultSet) {
			Object value;
			for (int i = 0; i < paramList.size(); i++) {
				param = paramList.get(i);
				outputType = (Integer) param.get(outputTypeKey);
				if (outputType != null) {
					value = statement.getObject(i + 1);
					
					if (value instanceof ResultSet) {
						result.put(i + 1, convertResultSetToArrayList((ResultSet) value));
					} else if (value instanceof java.sql.Date) {
						result.put(i + 1, new Date(((java.sql.Date) value).getTime()));
					} else {
						result.put(i + 1, value);
					}
				}
			}
		}
		
		return result;
	}
	
	private ArrayList<Map<String,String>> convertResultSetToArrayList(ResultSet rs) throws SQLException {

		ArrayList<Map<String,String>> results = new ArrayList<Map<String,String>>();
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		try {
			//move to the starter row
			for (int i = 0; i < startRow; i++) {
				if(!rs.next())
					break;
			};
	
			int i = 0;
	
			while (rs.next() && (i < maxRows)) {
				HashMap<String,String> vars = new HashMap<String,String>();
	
				for (int j = 1; j <= rsmd.getColumnCount(); j++) {
					String x = rsmd.getColumnName(j) + "";
	
					if ((rs.getString(x) == null) || rs.getString(x).equals("null")) {
						x=x.toLowerCase();
						vars.put(x, "");
					} else {
						x=x.toLowerCase();
						vars.put(x, rs.getString(x) + "");
					}
				}
	
				vars.put("rownumber", Integer.toString(i));
				vars.put("oddoreven", Integer.toString((i % 2)));
				results.add(vars);
	
				i++;
			}
		} catch (SQLException e) {
			
			//A patch for oracle empty result sets
			if(e.getErrorCode() == 1002)
				return results;
			throw e;
		}
		
		return results;
	}
}