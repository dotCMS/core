package org.apache.felix.framework;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * OSGi exception.
 * @author jsanca
 */
public class OsgiException extends DotRuntimeException {

    private static final long serialVersionUID = 1L;

    public OsgiException(String message) {
        super(message);
    }

    public OsgiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OsgiException(Throwable cause) {
        super(cause);
    }
}
