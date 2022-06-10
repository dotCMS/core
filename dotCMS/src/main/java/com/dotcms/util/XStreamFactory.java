package com.dotcms.util;

import com.dotcms.enterprise.publishing.remote.handler.ContentHandler;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum XStreamFactory {
    INSTANCE;

    public enum XStreamType {
        DEFAULT((final String encodingAsString) -> new XStream(new DomDriver(encodingAsString))),
        NOT_BROKEN_UNMAP_PROPERTIES(XStreamFactory::createNotBrokenUnMappedProperties);

        private Function<String, XStream> builder;

        XStreamType(final Function<String, XStream> builder) {
            this.builder = builder;
        }
    }

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
            "com.dotcms.publisher.pusher.wrapper.PushContentWorkflowWrapper",
            "com.dotcms.storage.model.Metadata",
            "com.dotmarketing.portlets.fileassets.business.FileAsset",
            "com.dotcms.publisher.business.PublishQueueElement",
            "com.dotcms.enterprise.publishing.bundlers.URLMapWrapper",
            "com.dotcms.enterprise.publishing.bundlers.**"
    };

    private Map<XStreamType, Map<Charset, XStream>> xstreams;

    XStreamFactory() {
        xstreams = new HashMap<>();

        for (final XStreamType xStreamType : XStreamType.values()) {
            xstreams.put(xStreamType, new HashMap<>());
        }
    }

    public synchronized XStream getInstance(){
        return getInstance(null);
    }

    public synchronized XStream getInstance(final Charset encoding){
        return getInstance(encoding, XStreamType.DEFAULT);
    }

    public synchronized XStream getInstanceNotBrokenUnmappedVersion(){
        return getInstance(null, XStreamType.NOT_BROKEN_UNMAP_PROPERTIES);
    }

    public synchronized XStream getInstanceNotBrokenUnmappedVersion(final Charset encoding){
        return getInstance(encoding, XStreamType.NOT_BROKEN_UNMAP_PROPERTIES);
    }

    private XStream getInstance(final Charset encoding, final XStreamType xStreamType) {
        final Map<Charset, XStream> charsetXStreamMap = xstreams.get(xStreamType);
        XStream xstream = charsetXStreamMap.get(encoding);

        if (!UtilMethods.isSet(xstream)) {
            final String encodingAsString = UtilMethods.isSet(encoding) ? encoding.toString() : null;
            xstream = xStreamType.builder.apply(encodingAsString);
            xstream.allowTypesByWildcard(WHITE_LIST);
            charsetXStreamMap.put(encoding, xstream);
        }

        return xstream;
    }

    /**
     * Custom unmapped properties safe XStream instance factory method
     * @return
     */
    public static XStream createNotBrokenUnMappedProperties(final String encodingAsString){
        return new XStream(new DomDriver(encodingAsString)){
            //This is here to prevent unmapped properties from old versions from breaking thr conversion
            //https://stackoverflow.com/questions/5377380/how-to-make-xstream-skip-unmapped-tags-when-parsing-xml
            @Override
            protected MapperWrapper wrapMapper(final MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(final Class definedIn, final String fieldName) {
                        if (definedIn == Object.class) {
                            Logger.warn(ContentHandler.class,String.format("unmapped property `%s` found ignored while importing bundle. ",fieldName));
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
    }
}
