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

package com.liferay.portal.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.StringUtil;

/**
 * <a href="DBBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.27 $
 *
 */
public class DBBuilder {

	public static void main(String[] args) {
		new DBBuilder();
	}

	public DBBuilder() {
		try {
			_buildSQL("portal");
			_buildSQL("update-1.7.5-1.8.0");
			_buildSQL("update-1.8.0-1.9.0");
			_buildSQL("update-1.9.1-1.9.5");
			_buildSQL("update-1.9.5-2.0.0");
			_buildSQL("update-2.0.3-2.1.0");
			_buildSQL("update-2.1.1-2.2.0");
			_buildSQL("update-2.2.1-2.2.5");
			_buildSQL("update-3.1.0-3.2.0");
			_buildCreate();
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private void _buildCreate() throws IOException {

		// DB2

		File file = new File("../sql/create/create-db2.sql");

		StringBuffer sb = new StringBuffer();

		sb.append("drop database lportal\n");
		sb.append("create database lportal\n");
		sb.append("connect to lportal\n");
		sb.append(_readSQL("../sql/portal/portal-db2.sql", _DB2[0], "\n"));

		/*sb.append(
			StringUtil.replace(
				FileUtil.read("../sql/indexes.sql"),
				new String[] {"\n", ";"},
				new String[] {"", "\n"}));*/

		FileUtil.write(file, sb.toString());

		// Firebird

		file = new File("../sql/create/create-firebird.sql");

		sb = new StringBuffer();

		sb.append("create database 'lportal.gdb' page_size 8192 user 'sysdba' password 'masterkey';\n");
		sb.append("connect 'lportal.gdb' user 'sysdba' password 'masterkey';\n");
		sb.append(
			_readSQL("../sql/portal/portal-firebird.sql", _FIREBIRD[0], ";\n"));
		sb.append("commit;\n");

		FileUtil.write(file, sb.toString());

		// MySQL

		file = new File("../sql/create/create-mysql.sql");

		sb = new StringBuffer();

		sb.append("drop database lportal;\n");
		sb.append("create database lportal;\n");
		sb.append("use lportal;\n");
		sb.append("\n");
		sb.append(FileUtil.read("../sql/portal/portal-mysql.sql")).append("\n");
		sb.append("\n");
		sb.append(FileUtil.read("../sql/indexes.sql")).append("\n");
		sb.append("\n");
		sb.append("commit;");

		FileUtil.write(file, sb.toString());

		// Oracle

		file = new File("../sql/create/create-oracle.sql");

		sb = new StringBuffer();

		sb.append("drop user &1 cascade;\n");
		sb.append("create user &1 identified by &2;\n");
		sb.append("grant connect,resource to &1;\n");
		sb.append("connect &1/&2;\n");
		sb.append("\n");
		sb.append(
			FileUtil.read("../sql/portal/portal-oracle.sql")).append("\n");
		sb.append("\n");
		sb.append(FileUtil.read("../sql/indexes.sql")).append("\n");
		sb.append("\n");
		sb.append("commit;\n");
		sb.append("\n");
		sb.append("quit");

		FileUtil.write(file, sb.toString());

		// SQL Server

		file = new File("../sql/create/create-sql-server.sql");

		sb = new StringBuffer();

		sb.append("drop database lportal;\n");
		sb.append("create database lportal;\n");
		sb.append("\n");
		sb.append("go\n");
		sb.append("\n");
		sb.append("use lportal;\n");
		sb.append("\n");
		sb.append(
			StringUtil.replace(
				FileUtil.read("../sql/portal/portal-sql-server.sql"),
				new String[] {"\\\\", "\\'", "\\\"", "\\n", "\\r"},
				new String[] {"\\", "''", "\"", "\n", "\r"}));
		sb.append("\n");
		sb.append(FileUtil.read("../sql/indexes.sql")).append("\n");
		sb.append("\n");
		sb.append("go");

		FileUtil.write(file, sb.toString());
	}

	private void _buildSQL(String fileName) throws IOException {
		File file = new File("../sql/" + fileName + ".sql");

		if (!file.exists()) {
			return;
		}

		// Template

		String template = FileUtil.read(file);

		// DB2

		String db2 = StringUtil.replace(template, _TEMPLATE, _DB2);

		db2 = _removeLongInserts(db2);
		db2 = _removeNull(db2);

		FileUtil.write("../sql/" + fileName + "/" + fileName + "-db2.sql", db2);

		// Firebird

		String firebird = StringUtil.replace(template, _TEMPLATE, _FIREBIRD);

		firebird = _removeLongInserts(firebird);
		firebird = _removeNull(firebird);
		firebird = StringUtil.replace(firebird, "varchar(100)", "varchar(60)");

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-firebird.sql", firebird);

		// Hypersonic

		String hypersonic = StringUtil.replace(
			template, _TEMPLATE, _HYPERSONIC);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-hypersonic.sql",
			hypersonic);

		// InterBase

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-interbase.sql", firebird);

		// JDataStore

		String jDataStore = StringUtil.replace(
			template, _TEMPLATE, _JDATASTORE);

		jDataStore = _removeLongInserts(jDataStore);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-jdatastore.sql",
			jDataStore);

		// MySQL

		String mysql = StringUtil.replace(template, _TEMPLATE, _MYSQL);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-mysql.sql", mysql);

		// Oracle

		String oracle = StringUtil.replace(template, _TEMPLATE, _ORACLE);

		oracle = _removeLongInserts(oracle);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-oracle.sql", oracle);

		// PostgreSQL

		String postgresql = StringUtil.replace(
			template, _TEMPLATE, _POSTGRESQL);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-postgresql.sql",
			postgresql);

		// SAP

		String sap = StringUtil.replace(template, _TEMPLATE, _SAP);

		FileUtil.write("../sql/" + fileName + "/" + fileName + "-sap.sql", sap);

		// SQL Server

		String sqlServer = StringUtil.replace(template, _TEMPLATE, _SQL_SERVER);

		FileUtil.write(
			"../sql/" + fileName + "/" + fileName + "-sql-server.sql",
			sqlServer);
	}

	private String _readSQL(String fileName, String comments, String eol)
		throws IOException {

		BufferedReader br = new BufferedReader(
			new FileReader(new File(fileName)));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null) {
			if (!line.startsWith(comments)) {
				line = StringUtil.replace(
					line,
					new String[] {"\n", "\t"},
					new String[] {"", ""});

				if (line.endsWith(";")) {
					sb.append(line.substring(0, line.length() - 1));
					sb.append(eol);
				}
				else {
					sb.append(line);
				}
			}
		}

		br.close();

		return sb.toString();
	}

	private String _removeLongInserts(String data) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(data));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("insert into Image") &&
				!line.startsWith("insert into JournalArticle") &&
				!line.startsWith("insert into JournalStructure") &&
				!line.startsWith("insert into JournalTemplate") &&
				!line.startsWith("insert into ShoppingItem")) {

				sb.append(line);
				sb.append("\n");
			}
		}

		br.close();

		return sb.toString();
	}

	private String _removeNull(String content) {
		content = StringUtil.replace(content, " not null", " not_null");
		content = StringUtil.replace(content, " null", "");
		content = StringUtil.replace(content, " not_null", " not null");

		return content;
	}

	private static String[] _TEMPLATE = {
		"##", "TRUE", "FALSE",
		"'01/01/1970'", "CURRENT_TIMESTAMP",
		"BOOLEAN", "DATE", "DOUBLE", "INTEGER", "STRING", "TEXT", "VARCHAR"};

	private static String[] _DB2 = {
		"--", "1", "0",
		"'1970-01-01-00.00.00.000000'", "current timestamp",
		"char(1)", "timestamp", "double", "integer", "long varchar",
		"long varchar", "varchar"};

	private static String[] _FIREBIRD = {
		"--", "1", "0",
		"'01/01/1970'", "current_timestamp",
		"smallint", "timestamp", "double precision", "integer", "varchar(4000)",
		"blob", "varchar"};

	private static String[] _HYPERSONIC = {
		"//", "true", "false",
		"'1970-01-01'", "now()",
		"bit", "timestamp", "double", "int", "longvarchar", "longvarchar",
		"varchar"};

	private static String[] _JDATASTORE = {
		"--", "TRUE", "FALSE",
		"'1970-01-01'", "current_timestamp",
		"boolean", "date", "double", "integer", "long varchar", "long varchar",
		"varchar"};

	private static String[] _MYSQL = {
		"##", "1", "0",
		"'1970-01-01'", "now()",
		"tinyint", "datetime", "double", "integer", "longtext", "longtext",
		"varchar"};

	private static String[] _ORACLE = {
		"--", "1", "0",
		"to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS')", "sysdate",
		"number(1, 0)", "date", "number(30,20)", "number(30,0)",
		"varchar2(4000)", "long varchar", "varchar2"};

	private static String[] _POSTGRESQL = {
		"--", "t", "f",
		"'01/01/1970'", "current_timestamp",
		"bool", "timestamp", "double precision", "integer", "text", "text",
		"varchar"};

	private static String[] _SAP = {
		"##", "TRUE", "FALSE",
		"'1970-01-01 00:00:00.000000'", "timestamp",
		"boolean", "timestamp", "float", "int", "long", "long", "varchar"};

	private static String[] _SQL_SERVER = {
		"--", "1", "0",
		"'19700101'", "GetDate()",
		"bit", "datetime", "float", "int", "varchar(1000)", "text", "varchar"};

}