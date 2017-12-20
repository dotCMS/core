package com.dotcms.rendering.velocity.services;

import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

final class StringifyObject {
    final String stringified;

    public StringifyObject(final Object o) {

        this.stringified = o.toString();
    }


    public StringifyObject(final String[] str) {
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
        this.stringified = sw.toString();
    }

    public StringifyObject(final Collection co) {
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
        this.stringified = sw.toString();
    }

    public StringifyObject(final Boolean o) {

        this.stringified = o.toString();
    }

    public StringifyObject(final String x) {
        StringWriter sw = new StringWriter();
        if (x.startsWith("$")) {
            this.stringified = x;
        } else {
            sw.append('"');
            sw.append(x.toString());
            sw.append('"');
            this.stringified = sw.toString();
        }
    }

    public StringifyObject(final Date x) {
        StringWriter sw = new StringWriter();
        String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(x);
        sw.append('"');
        sw.append(d);
        sw.append('"');
        this.stringified = sw.toString();
    }



    public String from() {
        return this.stringified ;
    }
}
