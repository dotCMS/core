package com.dotcms.api.system.event;

import java.io.Serializable;

/**
 * This class wraps the payload of a System Event, specifying its type (i.e.,
 * the Java class that the payload represents) so that the
 * marshalling/unmarshalling process can be performed without issues.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 13, 2016
 */
@SuppressWarnings("serial")
public class Payload implements Serializable {

	private final String type;
	private final Object data;

	/**
	 * Creates a payload object.
	 * 
	 * @param data
	 *            - Any Java object that represents the payload.
	 */
	public Payload(Object data) {
		this.type = data.getClass().getName();
		this.data = data;
	}

	/**
	 * Returns the type (fully qualified name) of the Java class representing
	 * the payload of the System Event.
	 * 
	 * @return The fully qualified name of the payload class.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the System Event payload.
	 * 
	 * @return The payload.
	 */
	public Object getData() {
		return data;
	}

} // E:O:F:Payload.
