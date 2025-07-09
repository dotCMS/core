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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * <a href="IntegerType.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class IntegerType implements UserType {

	public final static int DEFAULT_VALUE = 0;

	public final static int[] SQL_TYPES = new int[] {Types.INTEGER};

	@Override
	public Object deepCopy(Object obj) {
		return obj;
	}

	@Override
	public boolean equals(Object x, Object y) {
		if (x == y) {
			return true;
		}
		else if (x == null || y == null) {
			return false;
		}
		else {
			return x.equals(y);
		}
	}

	@Override
	public int hashCode(Object x) {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
		throws HibernateException, SQLException {

		int value = rs.getInt(names[0]);
		if (rs.wasNull()) {
			return Integer.valueOf(DEFAULT_VALUE);
		}
		return Integer.valueOf(value);
	}

	@Override
	public void nullSafeSet(PreparedStatement ps, Object obj, int index, SharedSessionContractImplementor session)
		throws HibernateException, SQLException {

		if (obj == null) {
			obj = Integer.valueOf(DEFAULT_VALUE);
		}

		ps.setInt(index, (Integer) obj);
	}

	@Override
	public Class returnedClass() {
		return Integer.class;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Object assemble(java.io.Serializable cached, Object owner) {
		return cached;
	}

	@Override
	public java.io.Serializable disassemble(Object value) {
		return (java.io.Serializable) value;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) {
		return original;
	}

}