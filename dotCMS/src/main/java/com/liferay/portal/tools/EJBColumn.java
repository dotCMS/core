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

import com.liferay.util.TextFormatter;
import com.liferay.util.Validator;

/**
 * <a href="EJBColumn.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class EJBColumn {

	public EJBColumn(String name) {
		this(name, null, null);
	}

	public EJBColumn(String name, String dbName, String type) {
		this(name, dbName, type, false, null, null, null);
	}

	public EJBColumn(String name, String dbName, String type, boolean primary,
					 String ejbName, String mappingKey, String mappingTable) {

		this(name, dbName, type, primary, ejbName, mappingKey, mappingTable,
			 true, true, false);
	}

	public EJBColumn(String name, String dbName, String type, boolean primary,
					 String ejbName, String mappingKey, String mappingTable,
					 boolean caseSensitive, boolean orderByAscending,
					 boolean checkArray) {

		_name = name;
		_dbName = dbName;
		_type = type;
		_primary = primary;
		_methodName = TextFormatter.format(name, TextFormatter.G);
		_ejbName = ejbName;
		_mappingKey = mappingKey;
		_mappingTable = mappingTable;
		_caseSensitive = caseSensitive;
		_orderByAscending = orderByAscending;
		_checkArray = checkArray;
	}

	public String getName() {
		return _name;
	}

	public String getDBName() {
		return _dbName;
	}

	public String getType() {
		return _type;
	}

	public boolean isPrimitiveType() {
		if (Character.isLowerCase(_type.charAt(0))) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isCollection() {
		if (_type.equals("Collection")) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isPrimary() {
		return _primary;
	}

	public String getMethodName() {
		return _methodName;
	}

	public String getEJBName() {
		return _ejbName;
	}

	public String getMappingKey() {
		return _mappingKey;
	}

	public String getMappingTable() {
		return _mappingTable;
	}

	public boolean isMappingOneToMany() {
		return Validator.isNotNull(_mappingKey);
	}

	public boolean isMappingManyToMany() {
		return Validator.isNotNull(_mappingTable);
	}

	public boolean isCaseSensitive() {
		return _caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		_caseSensitive = caseSensitive;
	}

	public boolean isOrderByAscending() {
		return _orderByAscending;
	}

	public void setOrderByAscending(boolean orderByAscending) {
		_orderByAscending = orderByAscending;
	}

	public boolean isCheckArray() {
		return _checkArray;
	}

	public boolean equals(Object obj) {
		EJBColumn col = (EJBColumn)obj;

		String name = col.getName();

		if (_name.equals(name)) {
			return true;
		}
		else {
			return false;
		}
	}

	private String _name;
	private String _dbName;
	private String _type;
	private boolean _primary;
	private String _methodName;
	private String _ejbName;
	private String _mappingKey;
	private String _mappingTable;
	private boolean _caseSensitive;
	private boolean _orderByAscending;
	private boolean _checkArray;

}