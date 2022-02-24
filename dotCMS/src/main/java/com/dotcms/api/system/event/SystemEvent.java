package com.dotcms.api.system.event;

import com.dotmarketing.util.UtilMethods;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;

/**
 * This class is the logical representation of a System Event in dotCMS. An
 * object of this nature can have several meanings, but the main goal is to
 * represent an event generated as a result of the execution of a process or
 * routine either by dotCMS or custom plugins.
 * <p>
 * As a System Event can be the result of a process or routine. Such a result or
 * additional information can be stored as part of the payload, which can be,
 * for example, a Java object represented as JSON.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEvent implements Serializable {

	private final String id;
	private final SystemEventType event;
	private final Payload payload;
	private final Date creationDate;
	private final String serverId;

	/**
	 * Creates a System Event. The creation date of the event is automatically
	 * set to the current date and time.
	 * 
	 * @param event
	 *            - The {@link SystemEventType} set for this event.
	 * @param payload
	 *            - The information containing the details of this event.
	 */
	public SystemEvent(SystemEventType event, Payload payload) {
		this(null, event, payload, null, null);
	}

	/**
	 * Creates a System Event.
	 * 
	 * @param eventType
	 *            - The {@link SystemEventType} set for this event.
	 * @param payload
	 *            - The information containing the details of this event.
	 * @param creationDate
	 *            - The creation date for this event.
	 */
	public SystemEvent(SystemEventType eventType, Payload payload, Date creationDate) {
		this(null, eventType, payload, creationDate, null);
	}

	/**
	 * Creates a System Event.
	 * 
	 * @param id
	 *            - The event ID. If a new event is being created, please use
	 *            {@link #SystemEvent(SystemEventType, Object, Date)} to let
	 *            dotCMS generate an appropriate ID.
	 * @param event
	 *            - The {@link SystemEventType} set for this event.
	 * @param payload
	 *            - The information containing the details of this event.
	 * @param creationDate
	 *            - The creation date for this event.
	 * @throws IllegalArgumentException
	 *             If the system event type or the payload object are not
	 *             specified.
	 */
	@JsonCreator
	public SystemEvent(
			@JsonProperty("id") final String id,
			@JsonProperty("event") final SystemEventType event,
			@JsonProperty("payload") final Payload payload,
			@JsonProperty("creationDate") final Date creationDate,
			@JsonProperty("serverId") final String serverId) {

		if (!UtilMethods.isSet(event)) {
			throw new IllegalArgumentException("System Event type must be specified.");
		}
		if (!UtilMethods.isSet(payload)) {
			throw new IllegalArgumentException("System Event payload must be specified.");
		}
		this.id = id;
		this.event = event;
		this.payload = payload;
		this.creationDate = creationDate == null ? new Date() : creationDate;
		this.serverId  = serverId;
	}

	/**
	 * Returns the type or category of this event.
	 * 
	 * @return The event type.
	 */
	@JsonProperty("event")
	public SystemEventType getEventType() {
		return event;
	}

	/**
	 * Returns the event payload object.
	 * 
	 * @return The payload object.
	 */
	@JsonProperty("payload")
	public Payload getPayload() {
		return payload;
	}

	/**
	 * Returns the creation date of this event.
	 * 
	 * @return The creation date.
	 */
	@JsonProperty("creationDate")
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Returns the ID of the event.
	 * 
	 * @return The event ID.
	 */
	@JsonProperty("id")
	public String getId() {
		return id;
	}

	/**
	 * Get the Server id where the event was created
	 * @return String
	 */
	@JsonProperty("serverId")
	public String getServerId() {
		return serverId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
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
			return this.event == other.event;
		}
		if (!this.id.equalsIgnoreCase(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SystemEvent [id=" + id + ", event=" + event + ", payload=" + payload + ", creationDate=" + creationDate
				+ "]";
	}
}
