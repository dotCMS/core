package com.dotcms.util;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProviderWrapper;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public enum XStreamFactory {
    INSTANCE;

    public static final int JAVA_BEAN_CONVERTER_PRIORITY = XStream.PRIORITY_VERY_LOW + 1;
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
            "sun.util.calendar.**",
            "com.dotmarketing.beans.Tree",
            "com.dotmarketing.tag.model.Tag",
            "com.dotmarketing.beans.MultiTree",
            "com.dotmarketing.tag.model.TagInode",
            "com.dotcms.publisher.business.EndpointDetail",
            "com.dotcms.publisher.pusher.wrapper.**",
            "com.dotcms.storage.model.Metadata",
            "com.dotmarketing.portlets.fileassets.business.FileAsset"
    };

    private Map<Charset, XStream> xstreams;

    XStreamFactory() {
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
            //xstream.registerConverter(new JavaBeanConverter(xstream.getMapper(), ContainerStructure.class), JAVA_BEAN_CONVERTER_PRIORITY);
            xstreams.put(encoding, xstream);
        }

        return xstream;
    }
}
