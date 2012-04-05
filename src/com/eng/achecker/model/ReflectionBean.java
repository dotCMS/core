package com.eng.achecker.model;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflectionBean {
	
	public ReflectionBean() {}
	
	public ReflectionBean(Map<String, Object> init) throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if ( init == null )
			return;
		BeanInfo info = Introspector.getBeanInfo(this.getClass());
		for ( PropertyDescriptor property : info.getPropertyDescriptors()) {
			Object value = init.get(property.getName());
			if ( value != null ) {
				Method write = property.getWriteMethod();
				if ( write != null )
					write.invoke(this, value);
			}
		}
	}

}
