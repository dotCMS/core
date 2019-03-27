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

import java.util.List;

import com.liferay.util.TextFormatter;

/**
 * <a href="EJBEntity.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.19 $
 *
 */
public class EJBEntity {

	public static EJBColumn getColumn(String name, List columnList) {
		int pos = columnList.indexOf(new EJBColumn(name));

		return (EJBColumn)columnList.get(pos);
	}

	public EJBEntity(String name) {
		this(null, null, null, name, null, false, null, null, null, null, null,
			 null, null);
	}

	public EJBEntity(String packagePath, String portletName,
					 String portletShortName, String name, String table,
					 boolean localManager, String persistenceClass, List pkList,
					 List collectionList, List columnList, EJBOrder order,
					 List finderList, List referenceList) {

		_packagePath = packagePath;
		_portletName = portletName;
		_portletShortName = portletShortName;
		_name = name;
		_table = table;
		_localManager = localManager;
		_persistenceClass = persistenceClass;
		_pkList = pkList;
		_collectionList = collectionList;
		_columnList = columnList;
		_order = order;
		_finderList = finderList;
		_referenceList = referenceList;
	}

	public String getPackagePath() {
		return _packagePath;
	}

	public String getPortletName() {
		return _portletName;
	}

	public String getPortletShortName() {
		return _portletShortName;
	}

	public String getName() {
		return _name;
	}

	public String getNames() {
		return TextFormatter.formatPlural(new String(_name));
	}

	public String getVarName() {
		return TextFormatter.format(_name, TextFormatter.I);
	}

	public String getVarNames() {
		return TextFormatter.formatPlural(new String(getVarName()));
	}

	public String getTable() {
		return _table;
	}

	public boolean hasLocalManager() {
		return _localManager;
	}

	public String getPersistenceClass() {
		return _persistenceClass;
	}

	public String getPKClassName() {
		if (hasCompoundPK()) {
			return _name + "PK";
		}
		else {
			EJBColumn col = (EJBColumn)_pkList.get(0);

			return col.getType();
		}
	}

	public String getPKVarName() {
		if (hasCompoundPK()) {
			return getVarName() + "PK";
		}
		else {
			EJBColumn col = (EJBColumn)_pkList.get(0);

			return col.getName();
		}
	}

	public boolean hasCompoundPK() {
		if (_pkList.size() > 1) {
			return true;
		}
		else {
			return false;
		}
	}

	public List getPKList() {
		return _pkList;
	}

	public List getCollectionList() {
		return _collectionList;
	}

	public List getColumnList() {
		return _columnList;
	}

	public boolean hasColumns() {
		if ((_columnList == null) || (_columnList.size() == 0)) {
			return false;
		}
		else {
			return true;
		}
	}

	public EJBOrder getOrder() {
		return _order;
	}

	public boolean isOrdered() {
		if (_order != null) {
			return true;
		}
		else {
			return false;
		}
	}

	public List getFinderList() {
		return _finderList;
	}

	public List getReferenceList() {
		return _referenceList;
	}

	public EJBColumn getColumn(String name) {
		return getColumn(name, _columnList);
	}

	public EJBColumn getColumnByMappingTable(String mappingTable) {
		for (int i = 0; i < _columnList.size(); i++) {
			EJBColumn col = (EJBColumn)_columnList.get(i);

			if (col.getMappingTable() != null &&
				col.getMappingTable().equals(mappingTable)) {

				return col;
			}
		}

		return null;
	}

	public boolean equals(Object obj) {
		EJBEntity entity = (EJBEntity)obj;

		String name = entity.getName();

		if (_name.equals(name)) {
			return true;
		}
		else {
			return false;
		}
	}

	private String _packagePath;
	private String _portletName;
	private String _portletShortName;
	private String _name;
	private String _table;
	private boolean _localManager;
	private String _persistenceClass;
	private List _pkList;
	private List _collectionList;
	private List _columnList;
	private EJBOrder _order;
	private List _finderList;
	private List _referenceList;

}