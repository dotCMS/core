package com.dotcms.util;

import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public enum XStreamFactory {
    INSTANCE;

    private String[] WHITE_LIST = new String[]{
            "javax.**",
            "com.google.common.**",
            "com.dotmarketing.beans.Identifier",
            "com.dotmarketing.portlets.languagesmanager.model.Language",
            "com.dotmarketing.portlets.**.model.**",
            "com.dotcms.publisher.business.PublishAuditHistory",
            "com.dotmarketing.beans.Permission",
            "com.liferay.portal.model.**",
            "com.dotmarketing.beans.ContainerStructure",
            "com.dotmarketing.business.**",
            "java.util.**",
            " java.util.ArrayList"
    };

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
            final String encodingAsString = UtilMethods.isSet(encoding) ? encoding.toString() : null;
            xstream = new XStream(new DomDriver(encodingAsString));
            xstream.allowTypesByWildcard(WHITE_LIST);
            xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()));
            xstreams.put(encoding, xstream);
        }

        return xstream;

    }
}
