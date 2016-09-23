package com.dotcms.api.system.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.api.system.event.dao.SystemEventsDAO;
import com.dotcms.api.system.event.dto.SystemEventDTO;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This singleton class provides access to the {@link SystemEventsAPI} class.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
@SuppressWarnings("serial")
public class SystemEventsFactory implements Serializable {

	private final SystemEventsDAO systemEventsDAO = new SystemEventsDAOImpl();
	private final SystemEventsAPI systemEventsAPI = new SystemEventsAPIImpl();

	/**
	 * Private constructor for singleton creation.
	 */
	private SystemEventsFactory() {

	}

	/**
	 * Singleton holder using initialization on demand
	 */
	private static class SingletonHolder {
		private static final SystemEventsFactory INSTANCE = new SystemEventsFactory();
	}

	/**
	 * Returns a single instance of this factory.
	 * 
	 * @return A unique {@link SystemEventsFactory} instance.
	 */
	public static SystemEventsFactory getInstance() {
		return SystemEventsFactory.SingletonHolder.INSTANCE;
	}

	/**
	 * Returns the data access object that will interact with information in the
	 * database.
	 * 
	 * @return The {@link SystemEventsDAO} instance.
	 */
	private SystemEventsDAO getSystemEventsDAO() {
		return this.systemEventsDAO;
	}

	/**
	 * Returns a singleton instance of the System Events API.
	 * 
	 * @return The {@link SystemEventsAPI} instance.
	 */
	public SystemEventsAPI getSystemEventsAPI() {
		return this.systemEventsAPI;
	}

	/**
	 * The concrete implementation of the {@link SystemEventsAPI} class.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Jul 11, 2016
	 *
	 */
	private final class SystemEventsAPIImpl implements SystemEventsAPI {

		private final ConversionUtils conversionUtils = ConversionUtils.INSTANCE;

		private SystemEventsDAO systemEventsDAO = getSystemEventsDAO();
		private MarshalUtils marshalUtils = MarshalFactory.getInstance().getMarshalUtils();

		@Override
		public void push(final SystemEvent systemEvent) throws DotDataException {
			if (!UtilMethods.isSet(systemEvent)) {
				final String msg = "System Event object cannot be null.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.add(new SystemEventDTO(systemEvent.getId(), systemEvent.getEventType().name(),
						this.marshalUtils.marshal(systemEvent.getPayload()), systemEvent.getCreationDate().getTime()));
			} catch (DotDataException e) {
				final String msg = "An error occurred when saving a system event with ID: [" + systemEvent.getId() + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void push(SystemEventType event, Payload payload) throws DotDataException {
			push( new SystemEvent(event, payload ) );
		}

		@Override
		public Collection<SystemEvent> getEventsSince(final long createdDate) throws DotDataException {
			if (createdDate <= 0) {
				final String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				final List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getEventsSince(createdDate);
				return this.conversionUtils.convert(result, (SystemEventDTO record) -> {
					return convertSystemEventDTO(record);
				});
			} catch (DotDataException e) {
				final String msg = "An error occurred when retreiving system events created since: ["
						+ new Date(createdDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public Collection<SystemEvent> getAll() throws DotDataException {
			try {
				final List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getAll();
				return this.conversionUtils.convert(result, (SystemEventDTO record) -> {
					return convertSystemEventDTO(record);
				});
			} catch (DotDataException e) {
				final String msg = "An error occurred when retreiving all system events.";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void deleteEvents(final long toDate) throws DotDataException {
			if (toDate <= 0) {
				final String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.deleteEvents(toDate);
			} catch (DotDataException e) {
				final String msg = "An error occurred when deleting system events created up to: [" + new Date(toDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void deleteEvents(final long fromDate, final long toDate) throws DotDataException {
			if (fromDate <= 0) {
				final String msg = "System Event 'from' date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			if (toDate <= 0) {
				final String msg = "System Event 'to' date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			if (fromDate > toDate) {
				final String msg = "System Event 'from' date cannot be greater than 'to' date.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.deleteEvents(fromDate, toDate);
			} catch (DotDataException e) {
				final String msg = "An error occurred when deleting system events created from: [" + new Date(fromDate)
						+ "] to: [" + new Date(toDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void deleteAll() throws DotDataException {
			try {
				this.systemEventsDAO.deleteAll();
			} catch (DotDataException e) {
				final String msg = "An error occurred when deleting all system events.";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		/**
		 * Converts the physical representation of a System Event (i.e., the
		 * information as stored in the database) to the logical representation.
		 * 
		 * @param record
		 *            - The {@link SystemEventDTO} object.
		 * @return The {@link Notification} object.
		 */
		private SystemEvent convertSystemEventDTO(SystemEventDTO record) {
			final String id = record.getId();
			final SystemEventType eventType = SystemEventType.valueOf(record.getEventType());
			final String payloadStr = record.getPayload();
			final Payload payload = marshalUtils.unmarshal(payloadStr, Payload.class);
			final Date created = new Date(record.getCreationDate());
			return new SystemEvent(id, eventType, payload, created);
		}

	}

	/**
	 * The concrete implementation of the {@link SystemEventsDAO} class.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Jul 11, 2016
	 *
	 */
	private final class SystemEventsDAOImpl implements SystemEventsDAO {

		private final ConversionUtils conversionUtils = ConversionUtils.INSTANCE;

		@Override
		public void add(final SystemEventDTO systemEvent) throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("INSERT INTO system_event (identifier, event_type, payload, created) VALUES (?, ?, ?, ?)");
			final String id = (!UtilMethods.isSet(systemEvent.getId())) ? UUIDGenerator.generateUuid() : systemEvent.getId();
			dc.addParam(id);
			dc.addParam(systemEvent.getEventType());
			dc.addParam(systemEvent.getPayload());
			dc.addParam(systemEvent.getCreationDate());
			dc.loadResult();
		}

		@Override
		public Collection<SystemEventDTO> getEventsSince(final long fromDate) throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event WHERE created >= ?");
			dc.addParam(fromDate);
			final List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			return this.conversionUtils.convert(systemEvents, (Map<String, Object> record) -> {
				return convertSystemEventRecord(record);
			});
		}

		@Override
		public Collection<SystemEventDTO> getAll() throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event");
			final List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			return this.conversionUtils.convert(systemEvents, (Map<String, Object> record) -> {
				return convertSystemEventRecord(record);
			});
		}

		@Override
		public void deleteEvents(final long toDate) throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event WHERE created <= ?");
			dc.addParam(toDate);
			dc.loadResult();
		}

		@Override
		public void deleteEvents(long fromDate, long toDate) throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event WHERE created >= ? AND created <= ?");
			dc.addParam(fromDate);
			dc.addParam(toDate);
			dc.loadResult();
		}

		@Override
		public void deleteAll() throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event");
			dc.loadResult();
		}

		/**
		 * Converts the raw data of the table row into the physical
		 * representation.
		 * 
		 * @param record
		 *            - The database record.
		 * @return The {@link SystemEventDTO} object.
		 */
		private SystemEventDTO convertSystemEventRecord(Map<String, Object> record) {
			final String id = (String) record.get("identifier");
			final String eventType = (String) record.get("event_type");
			final String payload = (String) record.get("payload");
			Long created = 0L;
			if (DbConnectionFactory.isOracle()) {
				BigDecimal result = (BigDecimal) record.get("created");
				created = new Long(result.toPlainString());
			} else {
				created = (Long) record.get("created");
			}
			return new SystemEventDTO(id, eventType, payload, created);
		}

	}

}
