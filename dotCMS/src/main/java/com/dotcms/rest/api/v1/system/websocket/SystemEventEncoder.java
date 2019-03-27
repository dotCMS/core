package com.dotcms.rest.api.v1.system.websocket;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;

/**
 * This is the base {@link Encoder} for the System Events end-point. This class
 * will transform every {@link SystemEvent} sent to the client into the
 * appropriate form, for example, a JSON representation.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jul 12, 2016
 */
public class SystemEventEncoder implements Encoder.Text<SystemEvent> {

	private final MarshalUtils marshalUtils = MarshalFactory.getInstance().getMarshalUtils();

	@Override
	public String encode(final SystemEvent object) throws EncodeException {
		return this.marshalUtils.marshal(object);
	}

	@Override
	public void init(final EndpointConfig config) {
		// Not required yet
	}

	@Override
	public void destroy() {
		// Not required yet
	}

} // E:O:F:SystemEventEncoder.
