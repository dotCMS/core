/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.dao.hibernate;

import java.sql.Connection;
import java.util.Enumeration;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.MappingException;
import net.sf.hibernate.cfg.Environment;
import net.sf.hibernate.dialect.DB2Dialect;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.dialect.GenericDialect;
import net.sf.hibernate.dialect.HSQLDialect;
import net.sf.hibernate.dialect.MySQLDialect;
import net.sf.hibernate.dialect.OracleDialect;
import net.sf.hibernate.dialect.PostgreSQLDialect;
import net.sf.hibernate.dialect.SQLServerDialect;
import net.sf.hibernate.exception.SQLExceptionConverter;
import net.sf.hibernate.exception.ViolatedConstraintNameExtracter;
import net.sf.hibernate.sql.CaseFragment;
import net.sf.hibernate.sql.JoinFragment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.util.Logger;
import com.liferay.util.GetterUtil;
import com.liferay.util.dao.DataAccess;
import com.liferay.util.dao.DriverInfo;

/**
 * <a href="DynamicDialect.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class DynamicDialect extends Dialect {

	public DynamicDialect() {

		// Instantiate the proper dialect

		String datasource = GetterUtil.getString(
			Environment.getProperties().getProperty(Environment.DATASOURCE,
			"jdbc/dotCMSPool"));

		try {
			Connection con = DataAccess.getConnection(datasource);

			String url = con.getMetaData().getURL();

			Class dialectClass = null;

			if (url.startsWith(DriverInfo.DB2_URL)) {
				dialectClass = DB2Dialect.class;
			}
			else if (url.startsWith(DriverInfo.HYPERSONIC_URL)) {
				dialectClass = HSQLDialect.class;
			}
			else if (url.startsWith(DriverInfo.MYSQL_URL)) {
				dialectClass = MySQLDialect.class;
			}
			else if (url.startsWith(DriverInfo.ORACLE_URL)) {
				dialectClass = OracleDialect.class;
			}
			else if (url.startsWith(DriverInfo.POSTGRESQL_URL)) {
				dialectClass = PostgreSQLDialect.class;
			}
			else if (url.startsWith(DriverInfo.SQLSERVER_URL)) {
				dialectClass = SQLServerDialect.class;
			}

			if (dialectClass != null) {
				_log.debug("Class implementation " + dialectClass.getName());
			}
			else {
				_log.debug("Class implementation is null");
			}

			if (dialectClass != null) {
				_dialect = (Dialect)dialectClass.newInstance();
			}

			DataAccess.cleanUp(con);
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		if (_dialect == null) {
			_dialect = new GenericDialect();
		}

		// Synchorize default properties

		getDefaultProperties().clear();

		Enumeration enu = _dialect.getDefaultProperties().propertyNames();

		while (enu.hasMoreElements()) {
        	String key = (String)enu.nextElement();
        	String value = _dialect.getDefaultProperties().getProperty(key);

			getDefaultProperties().setProperty(key, value);
		}
	}

	public String appendIdentitySelectToInsert(String insertSQL) {
		return _dialect.appendIdentitySelectToInsert(insertSQL);
	}

	public boolean bindLimitParametersFirst() {
		return _dialect.bindLimitParametersFirst();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return _dialect.bindLimitParametersInReverseOrder();
	}

	public SQLExceptionConverter buildSQLExceptionConverter() {
		return _dialect.buildSQLExceptionConverter();
	}

	public char closeQuote() {
		return _dialect.closeQuote();
	}

	public CaseFragment createCaseFragment() {
		return _dialect.createCaseFragment();
	}

	public JoinFragment createOuterJoinFragment() {
		return _dialect.createOuterJoinFragment();
	}

	public boolean dropConstraints() {
		return _dialect.dropConstraints();
	}

	public String getAddColumnString() {
		return _dialect.getAddColumnString();
	}

	public String getAddForeignKeyConstraintString(
			String constraintName, String[] foreignKey, String referencedTable,
			String[] primaryKey) {
		return _dialect.getAddForeignKeyConstraintString(
			constraintName, foreignKey, referencedTable, primaryKey);
	}

	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return _dialect.getAddPrimaryKeyConstraintString(constraintName);
	}

	public String getCascadeConstraintsString() {
		return _dialect.getCascadeConstraintsString();
	}

	public String getCreateSequenceString(String sequenceName)
		throws MappingException {

		return _dialect.getCreateSequenceString(sequenceName);
	}

	public String getDropForeignKeyString() {
		return _dialect.getDropForeignKeyString();
	}

	public String getDropSequenceString(String sequenceName)
		throws MappingException {

		return _dialect.getDropSequenceString(sequenceName);
	}

	public String getIdentityColumnString() throws MappingException {
		return _dialect.getIdentityColumnString();
	}

	public String getIdentityInsertString() {
		return _dialect.getIdentityInsertString();
	}

	public String getIdentitySelectString() throws MappingException {
		return _dialect.getIdentitySelectString();
	}

	public String getLimitString(String querySelect, boolean hasOffset) {
		return _dialect.getLimitString(querySelect, hasOffset);
	}

	public String getLimitString(
		String querySelect, boolean hasOffset, int limit) {

		return _dialect.getLimitString(querySelect, hasOffset, limit);
	}

	public String getLowercaseFunction() {
		return _dialect.getLowercaseFunction();
	}

	public String getNoColumnsInsertString() {
		return _dialect.getNoColumnsInsertString();
	}

	public String getNullColumnString() {
		return _dialect.getNullColumnString();
	}

	public String getQuerySequencesString() {
		return _dialect.getQuerySequencesString();
	}

	public char getSchemaSeparator() {
		return _dialect.getSchemaSeparator();
	}

	public String getSequenceNextValString(String sequenceName)
		throws MappingException {

		return _dialect.getSequenceNextValString(sequenceName);
	}

	public String getTypeName(int code) throws HibernateException {
		return _dialect.getTypeName(code);
	}

	public String getTypeName(int code, int length) throws HibernateException {
		return _dialect.getTypeName(code, length);
	}

	public ViolatedConstraintNameExtracter
		getViolatedConstraintNameExtracter() {

		return _dialect.getViolatedConstraintNameExtracter();
	}

	public boolean hasAlterTable() {
		return _dialect.hasAlterTable();
	}

	public boolean hasDataTypeInIdentityColumn() {
		return _dialect.hasDataTypeInIdentityColumn();
	}

	public char openQuote() {
		return _dialect.openQuote();
	}

	public boolean qualifyIndexName() {
		return _dialect.qualifyIndexName();
	}

	/*public void registerColumnType(int code, int capacity, String name) {
		_dialect.registerColumnType(code, capacity, name);
	}

	public void registerColumnType(int code, String name) {
		_dialect.registerColumnType(code, name);
	}

	public void registerFunction(String name, SQLFunction function) {
		_dialect.registerFunction(name, function);
	}*/

	public boolean supportsCheck() {
		return _dialect.supportsCheck();
	}

	public boolean supportsForUpdate() {
		return _dialect.supportsForUpdate();
	}

	public boolean supportsForUpdateNowait() {
		return _dialect.supportsForUpdateNowait();
	}

	public boolean supportsForUpdateOf() {
		return _dialect.supportsForUpdateOf();
	}

	public boolean supportsIdentityColumns() {
		return _dialect.supportsIdentityColumns();
	}

	public boolean supportsIfExistsAfterTableName() {
		return _dialect.supportsIfExistsAfterTableName();
	}

	public boolean supportsIfExistsBeforeTableName()  {
		return _dialect.supportsIfExistsBeforeTableName();
	}

	public boolean supportsLimit() {
		return _dialect.supportsLimit();
	}

	public boolean supportsLimitOffset() {
		return _dialect.supportsLimitOffset();
	}

	public boolean supportsSequences() {
		return _dialect.supportsSequences();
	}

	public boolean supportsUnique() {
		return _dialect.supportsUnique();
	}

	public boolean supportsVariableLimit() {
		return _dialect.supportsVariableLimit();
	}

	public String toString() {
		if (_dialect != null) {
			return _dialect.toString();
		}
		else {
			return null;
		}
	}

	public boolean useMaxForLimit() {
		return _dialect.useMaxForLimit();
	}

	private static final Log _log = LogFactory.getLog(DynamicDialect.class);

	private Dialect _dialect;

}