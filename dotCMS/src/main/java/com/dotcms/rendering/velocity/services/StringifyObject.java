package com.dotcms.rendering.velocity.services;

import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

final class StringifyObject {
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


    private String stringifyObject(final String[] str) {
        StringWriter sw = new StringWriter();
        sw.append('[');
        for (int i = 0; i < str.length; i++) {
            sw.append('"')
                .append(str[i])
                .append("\"");
            if (i != str.length - 1) {
                sw.append(",");
            }
        }
        sw.append(']');
        return  sw.toString();
    }

    private String  stringifyObject(final Collection co) {
        StringWriter sw = new StringWriter();

        sw.append('[');
        Iterator<Object> it = co.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            sw.append('"')
                .append(obj.toString())
                .append("\"");
            
            if(it.hasNext()) {
                sw.append(",");
            }
        }
        sw.append(']');
        return  sw.toString();
    }

    private String  stringifyObject(final Boolean o) {

        return ((Boolean)o).toString();
    }

    private String  stringifyObject(final String x) {
        StringWriter sw = new StringWriter();
        if (x.startsWith("$")) {
            return  sw.toString();
        } else {
            sw.append('"');
            sw.append(x.toString());
            sw.append('"');
            return  sw.toString();
        }
    }

    private String  stringifyObject(final Date x) {
        StringWriter sw = new StringWriter();
        String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(x);
        sw.append('"');
        sw.append(d);
        sw.append('"');
        return  sw.toString();
    }



    public String from() {
        return this.stringified ;
    }
}
