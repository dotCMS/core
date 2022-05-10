package com.dotcms.util;

import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public enum XStreamFactory {
    INSTANCE;

    private Map<Charset, XStream> xstreams;

    private XStreamFactory() {
        xstreams = new HashMap<>();
    }

    public synchronized XStream getInstance(){
        return getInstance(null);
    }

    public synchronized XStream getInstance(final Charset encoding){

        XStream xstream = xstreams.get(encoding);

        if (!UtilMethods.isSet(xstream)) {
            xstream = new XStream(new DomDriver(null));
            xstreams.put(encoding, xstream);
        }

        return xstream;

    }
}
