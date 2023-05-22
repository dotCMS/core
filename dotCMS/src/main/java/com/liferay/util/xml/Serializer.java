package com.liferay.util.xml;

import com.liferay.util.lang.ArrayWrapper;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class Serializer {

	public static Object[] readArray(String xml, Class<?> c) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(c);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return ((ArrayWrapper) jaxbUnmarshaller.unmarshal(new StringReader(xml))).getArray();
	}

	public static String writeArray(Object[] array) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(ArrayWrapper.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		StringWriter sw = new StringWriter();
		jaxbMarshaller.marshal(new ArrayWrapper(array), sw);
		return sw.toString();
	}

	public static List readList(String xml, Class<?> c) throws JAXBException {
		return List.of(readArray(xml, c));
	}

	public static String writeList(List list) throws JAXBException {
		return writeArray(list.toArray());
	}

	public static Object readObject(Class<?> c, String xml) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(c);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return jaxbUnmarshaller.unmarshal(new StringReader(xml));
	}

	public static String writeObject(Object obj) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		StringWriter sw = new StringWriter();
		jaxbMarshaller.marshal(obj, sw);
		return sw.toString();
	}
}
