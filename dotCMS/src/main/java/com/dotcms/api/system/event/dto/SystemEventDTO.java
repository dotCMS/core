package com.dotcms.api.system.event.dto;

import java.io.Serializable;

import com.dotmarketing.util.UtilMethods;

/**
 * This class is the physical representation of a System Event in the database.
 * The data access layer interacts with this class to represent each row of the
 * {@code system_event} database table.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
@SuppressWarnings("serial")
public class SystemEventDTO implements Serializable {

	private final String id;
	private final String eventType;
	private final String payload;
	private final long creationDate;

	/**
	 * Creates a System Event object.
	 * 
	 * @param identifier
	 *            - The event ID.
	 * @param eventType
	 *            - The event type or category.
	 * @param payload
	 *            - The event message. This can be a simple String or a String
	 *            representation of an object.
	 * @param creationDate
	 *            - The event creation date.
	 */
	public SystemEventDTO(String id, String eventType, String payload, long creationDate) {
		this.id = id;
		this.eventType = eventType;
		this.payload = payload;
		this.creationDate = creationDate;
	}

	/**
	 * Returns the type or category of this event.
	 * 
	 * @return The event type.
	 */
	public String getEventType() {
		return eventType;
	}

	/**
	 * Returns the event payload represented as a {@code String}.
	 * 
	 * @return The {@code String} representation of the payload.
	 */
	public String getPayload() {
		return payload;
	}

	/**
	 * Returns the creation date of this event as {@code long} type.
	 * 
	 * @return The {@code long} representation of the creation date.
	 */
	public long getCreationDate() {
		return creationDate;
	}

	/**
	 * Returns the ID of the event.
	 * 
	 * @return The event ID.
	 */
	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == 0) ? 0 : (int) creationDate);
		result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SystemEventDTO other = (SystemEventDTO) obj;
		if (!UtilMethods.isSet(other.id)) {
			return false;
		}
		if (!this.id.equalsIgnoreCase(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SystemEventDTO [id=" + id + ", eventType=" + eventType + ", payload=" + payload + ", creationDate="
				+ creationDate + "]";
	}

}
