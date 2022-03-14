package com.dotcms.api.system.event;

import com.dotcms.util.jackson.PayloadDeserializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.Objects;

/**
 * This class wraps the payload of a System Event, specifying its type (i.e.,
 * the Java class that the payload represents) so that the
 * marshalling/unmarshalling process can be performed without issues.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 13, 2016
 */
@JsonInclude(Include.NON_NULL)
@JsonDeserialize(using = PayloadDeserializer.class)
public class Payload implements Serializable {

	private final Object data;
	private final Visibility visibility;
	private final String type;

    private final Object visibilityValue; // this could be anything: an user id, role uid or permission, or event a meta object with several things.
	private final String visibilityType;
	/**
	 * Creates a payload object.
	 *
	 * @param data {@link Object}
	 *            - Any Java object that represents the payload.
	 */
	public Payload(final Object data) {

		this(data, Visibility.GLOBAL, null);
	}
	
	/**
	 * Creates a payload object without data. This method generates an empty 
	 * object payload using an empty Void generic class object. This allows 
	 * to send events that doesn't requires any data input on their payload.
	 *
	 */
	public Payload() {

		this(new Void(), Visibility.GLOBAL, null);
	}
	
	/**
	 * Creates a payload object without data, but allowing to set the visility 
	 * and visibility. This method generates an empty object payload using an
	 * empty Void generic class object. This allows to send events that doesn't 
	 * requires any data input on their payload.
	 * 
	 * @param visibility {@link Visibility}
	 * 			  - If the event should be apply just for a specific user, role or global
	 * @param visibilityValue {@link String}
	 * 			  - Depending of the visibility type, this could be an userId or roleId, for global just keep it null.
	 */
	public Payload(final Visibility visibility,
				   final Object visibilityValue) {
		this(new Void(), visibility, visibilityValue);
	}

	/**
	 * Creates a payload object.
	 *
	 * @param data         {@link Object}
	 *                     - Any Java object that represents the payload.
	 * @param visibility   {@link Visibility}
	 *                     - If the event should be apply just for a specific user, role or global
	 * @param visibilityValue {@link String}
	 *                     - Depending of the visibility type, this could be an userId or roleId, for global just keep it null.
	 */
	@JsonCreator
	public Payload(@JsonProperty("data") final Object data,
			@JsonProperty("visibility") final Visibility visibility,
			@JsonProperty("visibilityValue") final Object visibilityValue) {
		this.data = data;
		this.visibility = visibility;
		this.visibilityValue = visibilityValue;
		final Object rawData = getRawData();
		this.type = rawData != null ? rawData.getClass().getName() : null;
		this.visibilityType = visibilityValue != null ? visibilityValue.getClass().getName() : null;
	}

	/**
	 * Returns the System Event payload.
	 * 
	 * @return The payload.
	 */
	public Object getData() {

		Object data = this.data;

		if (data instanceof DataWrapper){
			data = DataWrapper.class.cast(data).getData();
		}

		return data;
	}

	/**
	 * Payload Type getter
	 * @return
	 */
	public String getType() {
		return type;
	}

	@JsonIgnore
	public Object getRawData() {
		return this.data;
	}

	/**
	 * Returns the Visibility for this event
	 *
	 * @return Visibility
     */
	public Visibility getVisibility() {
		return visibility;
	}

	/**
	 * Returns the visibility value
	 * @return Object
	 */
	public Object getVisibilityValue() {
		return visibilityValue;
	}

	/**
	 * Returns the visibility type
	 * @return String
	 */
	public String getVisibilityType() {
		return visibilityType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Payload payload = (Payload) o;

		return Objects.equals(data, payload.data);

	}

	@Override
	public int hashCode() {
		return data != null ? data.hashCode() : 0;
	}
} // E:O:F:Payload.
