package com.dotcms.api.system.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.api.system.event.dao.SystemEventsDAO;
import com.dotcms.api.system.event.dto.SystemEventDTO;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 *   
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEventsFactory implements Serializable {

	private final SystemEventsDAO systemEventsDAO = new SystemEventsDAOImpl();
	private final SystemEventsAPI systemEventsAPI = new SystemEventsAPIImpl();

	private SystemEventsFactory() {
		// singleton
	}

	private static class SingletonHolder {
		private static final SystemEventsFactory INSTANCE = new SystemEventsFactory();
	}

	/**
	 * Get the instance.
	 * 
	 * @return SystemEventsFactory
	 */
	public static SystemEventsFactory getInstance() {

		return SystemEventsFactory.SingletonHolder.INSTANCE;
	}

	/**
	 * 
	 * @return
	 */
	public SystemEventsDAO getSystemEventsDAO() {
		return this.systemEventsDAO;
	}

	/**
	 * 
	 * @return
	 */
	public SystemEventsAPI getSystemEventsAPI() {
		return this.systemEventsAPI;
	}

	/**
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Jul 11, 2016
	 *
	 */
	private final class SystemEventsDAOImpl implements SystemEventsDAO {

		private final ConversionUtils conversionUtils = ConversionUtils.INSTANCE;
		
		@Override
		public void add(SystemEventDTO systemEvent) throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("INSERT INTO system_event (identifier, event_type, payload, created) VALUES (?, ?, ?, ?)");
			dc.addParam(systemEvent.getId());
			dc.addParam(systemEvent.getEventType());
			dc.addParam(systemEvent.getPayload());
			dc.addParam(systemEvent.getCreationDate());
			dc.loadResult();
		}

		@Override
		public Collection<SystemEventDTO> getEventsSince(long fromDate) throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event WHERE created >= ?");
			dc.addParam(fromDate);
			List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			/*return ConversionUtils.INSTANCE.convert(result, new Converter<Map<String, Object>, SystemEventDTO>() {

				@Override
				public SystemEventDTO convert(Map<String, Object> original) {
					return new SystemEventDTO((String) original.get("identifier"), (String) original.get("event"), (String) original
							.get("payload"), (Long) original.get("created"));
				}
				
			});*/
			return this.conversionUtils.convert(systemEvents, (Map<String, Object> record) -> {
				String id = (String) record.get("identifier");
				String eventType = (String) record.get("event_type");
				String payload = (String) record.get("payload");
				Long created = (Long) record.get("created");
				return new SystemEventDTO(id, eventType, payload, created);
			});
		}

		@Override
		public Collection<SystemEventDTO> getAll() throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("SELECT identifier, event_type, payload, created FROM system_event");
			List<Map<String, Object>> systemEvents = dc.loadObjectResults();
			return this.conversionUtils.convert(systemEvents, (Map<String, Object> record) -> {
				String id = (String) record.get("identifier");
				String eventType = (String) record.get("event_type");
				String payload = (String) record.get("payload");
				Long created = (Long) record.get("created");
				return new SystemEventDTO(id, eventType, payload, created);
			});
		}

		@Override
		public void deleteEvents(long toDate) throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event WHERE created <= ?");
			dc.addParam(toDate);
			dc.loadResult();
		}

		@Override
		public void deleteEvents(long fromDate, long toDate) throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event WHERE created >= ? AND created <= ?");
			dc.addParam(fromDate);
			dc.addParam(toDate);
			dc.loadResult();
		}

		@Override
		public void deleteAll() throws DotDataException {
			DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM system_event");
			dc.loadResult();
		}

	}

	/**
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
		public void push(SystemEvent systemEvent) throws DotDataException {
			if (!UtilMethods.isSet(systemEvent) || !UtilMethods.isSet(systemEvent.getId())) {
				String msg = "System Event object cannot be null.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.add(new SystemEventDTO(systemEvent.getId(), systemEvent.getEventType().name(),
						this.marshalUtils.marshal(systemEvent.getPayload()), systemEvent.getCreationDate().getTime()));
			} catch (DotDataException e) {
				String msg = "An error occurred when saving a system event with ID: [" + systemEvent.getId() + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public Collection<SystemEvent> getEventsSince(long createdDate) throws DotDataException {
			if (createdDate <= 0) {
				String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getEventsSince(createdDate);
				return this.conversionUtils.convert(result, (SystemEventDTO record) -> {
					String id = record.getId();
					SystemEventType eventType = SystemEventType.valueOf(record.getEventType());
					String payload = record.getPayload();
					Date created = new Date(record.getCreationDate());
					return new SystemEvent(id, eventType, payload, created);
				});
			} catch (DotDataException e) {
				String msg = "An error occurred when retreiving system events created since: [" + new Date(createdDate)
						+ "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public Collection<SystemEvent> getAll() throws DotDataException {
			try {
				List<SystemEventDTO> result = (List<SystemEventDTO>) this.systemEventsDAO.getAll();
				return this.conversionUtils.convert(result, (SystemEventDTO record) -> {
					String id = record.getId();
					SystemEventType eventType = SystemEventType.valueOf(record.getEventType());
					String payload = record.getPayload();
					Date created = new Date(record.getCreationDate());
					return new SystemEvent(id, eventType, payload, created);
				});
			} catch (DotDataException e) {
				String msg = "An error occurred when retreiving all system events.";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void deleteEvents(long toDate) throws DotDataException {
			if (toDate <= 0) {
				String msg = "System Event creation date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.deleteEvents(toDate);
			} catch (DotDataException e) {
				String msg = "An error occurred when deleting system events created up to: [" + new Date(toDate) + "]";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

		@Override
		public void deleteEvents(long fromDate, long toDate) throws DotDataException {
			if (fromDate <= 0) {
				String msg = "System Event 'from' date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			if (toDate <= 0) {
				String msg = "System Event 'to' date must be greater than zero.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			if (fromDate > toDate) {
				String msg = "System Event 'from' date cannot be greater than 'to' date.";
				Logger.error(this, msg);
				throw new IllegalArgumentException(msg);
			}
			try {
				this.systemEventsDAO.deleteEvents(fromDate, toDate);
			} catch (DotDataException e) {
				String msg = "An error occurred when deleting system events created from: [" + new Date(fromDate)
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
				String msg = "An error occurred when deleting all system events.";
				Logger.error(this, msg, e);
				throw new DotDataException(msg, e);
			}
		}

	}

}
