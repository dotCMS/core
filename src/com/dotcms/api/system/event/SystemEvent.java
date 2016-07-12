package com.dotcms.api.system.event;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

/**
 * Logic DTO
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEvent implements Serializable {

	private final String id;
	private final SystemEventType event;
	private final Object payload;
	private final Date creationDate;

	/**
	 * 
	 * @param event
	 * @param payload
	 */
	public SystemEvent(SystemEventType event, Object payload) {
		this(null, event, payload, null);
	}

	/**
	 * 
	 * @param event
	 * @param payload
	 * @param creationDate
	 */
	public SystemEvent(SystemEventType event, Object payload, Date creationDate) {
		this(null, event, payload, creationDate);
	}

	/**
	 * 
	 * @param identifier
	 * @param eventType
	 * @param payload
	 * @param creationDate
	 */
	public SystemEvent(String id, SystemEventType eventType, Object payload, Date creationDate) {
		if (!UtilMethods.isSet(eventType)) {
			throw new IllegalArgumentException("System Event type must be specified.");
		}
		if (!UtilMethods.isSet(payload)) {
			throw new IllegalArgumentException("System Event payload must be specified.");
		}
		this.id = !UtilMethods.isSet(id) ? UUIDGenerator.generateUuid() : id;
		this.event = eventType;
		this.payload = payload;
		this.creationDate = creationDate == null ? new Date() : creationDate;
	}

	/**
	 * 
	 * @return
	 */
	public SystemEventType getEventType() {
		return event;
	}

	/**
	 * 
	 * @return
	 */
	public Object getPayload() {
		return payload;
	}

	/**
	 * 
	 * @return
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((event == null) ? 0 : event.hashCode());
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
		SystemEvent other = (SystemEvent) obj;
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
		return "SystemEventDTO [id=" + id + ", event=" + event + ", payload=" + payload + ", creationDate=" + creationDate
				+ "]";
	}

}
