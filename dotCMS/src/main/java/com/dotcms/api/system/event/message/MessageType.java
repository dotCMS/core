package com.dotcms.api.system.event.message;

import com.dotcms.api.system.event.message.builder.SystemMessage;

/**
 * it's the types value allow for {@link SystemMessage}
 */
public enum MessageType {
    SIMPLE_MESSAGE,  // just raw text message
    CONFIRMATION_MESSAGE, // could be a RAW or RICH message but includes a callback on case confirmation (yes) and optional callback for no.
}
