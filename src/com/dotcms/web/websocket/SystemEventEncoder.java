package com.dotcms.web.websocket;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Encoder for SystemEvent's
 * @author jsanca
 */
public class SystemEventEncoder implements Encoder.Text<SystemEvent> {

    private final MarshalUtils marshalUtils =
            MarshalFactory.getInstance().getMarshalUtils();

    @Override
    public String encode(final SystemEvent object) throws EncodeException {

        return this.marshalUtils.marshal(object);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
} // E:O:F:SystemEventEncoder.
