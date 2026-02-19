package com.dotcms.rendering.velocity.services;


import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * Utility class to convert objects to their string representation for Velocity context.
 * Uses ThreadLocal buffers to minimize object allocation during page rendering.
 */
final class StringifyObject {

    /**
     * ThreadLocal StringBuilder pool to avoid allocating new StringWriter/StringBuilder
     * for each stringify operation. This significantly reduces GC pressure during
     * page rendering where many objects need to be stringified.
     */
    private static final ThreadLocal<StringBuilder> BUFFER =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    /**
     * ThreadLocal SimpleDateFormat to avoid creating expensive formatter objects.
     * SimpleDateFormat is not thread-safe, so ThreadLocal is required.
     */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    final String stringified;

    public StringifyObject(final Object o) {

        if(o instanceof String[]) {
            this.stringified = stringifyObject((String[]) o);
        }
        else if(o instanceof Collection) {
            this.stringified = stringifyObject((Collection) o);
        }
        else if(o instanceof Boolean) {
            
            this.stringified = stringifyObject((Boolean) o);
        }
        else if(o instanceof Date) {
            
            this.stringified = stringifyObject((Date) o);
        }
        else if(o instanceof String) {
            
            this.stringified = stringifyObject((String) o);
        }
        else {
            this.stringified = o.toString();
        }
    }


    /**
     * Gets the ThreadLocal StringBuilder, resetting it for reuse.
     * @return A clean StringBuilder ready for use
     */
    private static StringBuilder getBuffer() {
        final StringBuilder sb = BUFFER.get();
        sb.setLength(0);  // Reset without reallocation
        return sb;
    }

    private String stringifyObject(final String[] str) {
        final StringBuilder sb = getBuffer();
        sb.append('[');
        for (int i = 0; i < str.length; i++) {
            sb.append('"')
                .append(str[i])
                .append('"');
            if (i != str.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String stringifyObject(final Collection<?> co) {
        final StringBuilder sb = getBuffer();
        sb.append('[');
        final Iterator<?> it = co.iterator();
        while (it.hasNext()) {
            final Object obj = it.next();
            sb.append('"')
                .append(obj.toString())
                .append('"');
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String stringifyObject(final Boolean o) {
        return o.toString();
    }

    private String stringifyObject(final String x) {
        final StringBuilder sb = getBuffer();
        sb.append('"');
        sb.append(x.replace("\"", "`"));
        sb.append('"');
        return sb.toString();
    }

    private String stringifyObject(final Date x) {
        final StringBuilder sb = getBuffer();
        final String d = DATE_FORMAT.get().format(x);
        sb.append('"');
        sb.append(d);
        sb.append('"');
        return sb.toString();
    }



    public String from() {
        return this.stringified ;
    }
}
