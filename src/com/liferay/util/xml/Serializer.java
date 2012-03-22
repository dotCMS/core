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

package com.liferay.util.xml;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.exolab.castor.xml.CastorException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;

import com.dotmarketing.util.Logger;
import com.liferay.util.ListUtil;
import com.liferay.util.lang.ArrayWrapper;

/**
 * <a href="Serializer.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class Serializer {

	public static Object[] readArray(String xml) throws CastorException {
		Reader reader = new StringReader(xml);

		ArrayWrapper aw;
		try {
			aw = (ArrayWrapper)Unmarshaller.unmarshal(ArrayWrapper.class, reader);
		} catch (MarshalException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		} catch (ValidationException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		}
		return aw.getArray();
	}

	public static String writeArray(Object[] array) throws CastorException {
		ArrayWrapper aw = new ArrayWrapper();
		aw.setArray(array);

		Writer writer = new StringWriter();
		try {
			Marshaller.marshal(aw, writer);
		} catch (MarshalException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		} catch (ValidationException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		}

		return writer.toString();
	}

	public static List readList(String xml) throws CastorException {
		List list = ListUtil.fromArray(readArray(xml));

		return list;
	}

	public static String writeList(List list) throws CastorException {
		Object[] array = list.toArray();

		return writeArray(array);
	}

	public static Object readObject(Class c, String xml)
		throws CastorException {

		Reader reader = new StringReader(xml);

		Object obj = null;
		try {
			obj = (Object)Unmarshaller.unmarshal(c, reader);
		} catch (MarshalException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		} catch (ValidationException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		}

		return obj;
	}

	public static String writeObject(Object obj) throws CastorException {
		Writer writer = new StringWriter();

		try {
			Marshaller.marshal(obj, writer);
		} catch (MarshalException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		} catch (ValidationException e) {
			Logger.error(Serializer.class, e.getMessage(), e);
			throw new CastorException(e.getMessage());
		}

		return writer.toString();
	}

}