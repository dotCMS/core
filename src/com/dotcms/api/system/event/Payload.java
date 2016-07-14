package com.dotcms.api.system.event;

import java.io.Serializable;

/**
 * Just wrapper the payload with a String which is the
 * @author jsanca
 */
public class Payload implements Serializable {

    private final String type;
    private final Object data;


    public Payload(Object data) {
        this.type = data.getClass().getName();
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
} // E:O:F:Payload.
