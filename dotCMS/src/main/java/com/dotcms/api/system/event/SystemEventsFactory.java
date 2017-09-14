package com.dotcms.api.system.event;

import com.dotcms.api.system.event.dao.SystemEventsDAO;
import com.dotcms.api.system.event.dto.SystemEventDTO;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


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

	public static final String EVENTS_THREAD_POOL_SUBMITTER_NAME = "events";
	private final DotSubmitter dotSubmitter		  = DotConcurrentFactory.getInstance().getSubmitter(EVENTS_THREAD_POOL_SUBMITTER_NAME);
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
	protected SystemEventsDAO getSystemEventsDAO() {
		return this.systemEventsDAO;
	}

	/**
	 * Returns the Thread Pool for the async events
	 * It is public in case some external client needs to build the payload in an async task, could reuse the same Submitter.
	 * @return The {@link DotSubmitter} instance.
	 */
	public DotSubmitter getDotSubmitter() {
		return this.dotSubmitter;
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
		private final DotSubmitter dotSubmitter = getDotSubmitter();
		private SystemEventsDAO systemEventsDAO = getSystemEventsDAO();
		private MarshalUtils marshalUtils = MarshalFactory.getInstance().getMarshalUtils();

		/**
		 * Method that will save a given system event
		 *
		 * @param systemEvent         SystemEvent to save
		 * @param forceNewTransaction true when is required to create a new transaction, this value must be sent as true
		 *                            when this push is called inside a thread
		 * @throws DotDataException
		 */
		private void push(final SystemEvent systemEvent, boolean forceNewTransaction) throws DotDataException {
			if (!UtilMethods.isSet(systemEvent)) {
				final String msg = "System Event object cannot be null.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}

			boolean localTransaction = false;

			try {

				if ( forceNewTransaction ) {
					//Start a transaction
					HibernateUtil.startTransaction();
					localTransaction = true;
				} else {
					//Check for a transaction and start one if required
					localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
				}

				this.systemEventsDAO.add(new SystemEventDTO(systemEvent.getId(), systemEvent.getEventType().name(),
						this.marshalUtils.marshal(systemEvent.getPayload()), systemEvent.getCreationDate().getTime()));

				//Everything ok..., committing the transaction
				if ( localTransaction ) {
					HibernateUtil.commitTransaction();
				}
			} catch (Exception e) {

				try {
					//On error rolling back the changes
					if ( localTransaction ) {
						HibernateUtil.rollbackTransaction();
					}
				} catch (DotHibernateException hibernateException) {
					Logger.error(SystemEventsAPIImpl.class, hibernateException.getMessage(), hibernateException);
				}

				final String msg = "An error occurred when saving a system event with ID: [" + systemEvent.getId()
						+ "]: " + e.getMessage();
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			} finally {

				if (localTransaction) {

					DbConnectionFactory.closeSilently();
				}
			}
		}

		@Override
		public void push(final SystemEvent systemEvent) throws DotDataException {
			push(systemEvent, false);
		}

		@Override
		public void push(SystemEventType event, Payload payload) throws DotDataException {
			push( new SystemEvent(event, payload ) );
		}

		@Override
		public void pushAsync(final SystemEventType event, final Payload payload) throws DotDataException {

			// if by any reason the submitter couldn't be created, sends the message syn
			if (null == this.dotSubmitter) {
				Logger.debug(this, "Sending a message: " + event + "sync, it seems the dotSubmitter could not be created");
				this.push(event, payload);
			} else {
				this.dotSubmitter.execute(() -> {

					try {

						Logger.debug(this, "Sending an async message: " + event);
						push(new SystemEvent(event, payload), true);
					} catch (DotDataException e) {
						Logger.info(this, e.getMessage());
					}
				});
			}
		}

		@Override
		@CloseDBIfOpened
		public Collection<SystemEvent> getEventsSince(final long createdDate) throws DotDataException {
			if (createdDate <= 0) {
				final String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				final List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getEventsSince(createdDate);
				return this.conversionUtils.convert(result, this::convertSystemEventDTO);
			} catch (DotDataException e) {
				final String msg = "An error occurred when retreiving system events created since: ["
						+ new Date(createdDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		@CloseDBIfOpened
		public Collection<SystemEvent> getAll() throws DotDataException {
			try {
				final List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getAll();
				return this.conversionUtils.convert(result, this::convertSystemEventDTO);
			} catch (DotDataException e) {
				final String msg = "An error occurred when retreiving all system events.";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@WrapInTransaction
		@Override
		public void deleteEvents(final long toDate) throws DotDataException {
			if (toDate <= 0) {
				final String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}

			try {

				this.systemEventsDAO.deleteEvents(toDate);
			} catch (Exception e) {

				final String msg = "An error occurred when deleting system events created up to: [" + new Date(toDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@WrapInTransaction
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
			} catch (Exception e) {

				final String msg = "An error occurred when deleting system events created from: [" + new Date(fromDate)
						+ "] to: [" + new Date(toDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@WrapInTransaction
		@Override
		public void deleteAll() throws DotDataException {

			try {

				this.systemEventsDAO.deleteAll();
			} catch (Exception e) {

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
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event WHERE created >= ? order by created");
			dc.addParam(fromDate);
			final List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			return this.conversionUtils.convert(systemEvents, this::convertSystemEventRecord);
		}

		@Override
		public Collection<SystemEventDTO> getAll() throws DotDataException {
			final DotConnect dc = new DotConnect();
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event");
			final List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			return this.conversionUtils.convert(systemEvents, this::convertSystemEventRecord);
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
