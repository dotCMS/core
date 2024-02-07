package com.dotcms.util.xstream;

import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class XStreamHandler {

    private XStreamHandler(){

    }

    public static XStream newXStreamInstance(final String encoding) {
        final XStream xstream = new XStream(new DomDriver(encoding)) {
            //This is here to prevent unmapped properties from old versions from breaking thr conversion
            //https://stackoverflow.com/questions/5377380/how-to-make-xstream-skip-unmapped-tags-when-parsing-xml
            @Override
            protected MapperWrapper wrapMapper(final MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(final Class definedIn,
                            final String fieldName) {
                        if (definedIn == Object.class) {
                            Logger.warn(XStreamHandler.class, String.format(
                                    "unmapped property `%s` found ignored while importing bundle. ",
                                    fieldName));
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };

        xstream.allowTypesByWildcard(new String[] {
                "com.dotcms.**", "com.dotmarketing.**", "com.google.common.collect.**", "java.lang.**",
                "java.util.**", "java.sql.**", "com.thoughtworks.xstream.mapper.**"
        });

        xstream.addPermission(aClass -> {
            Logger.warn(aClass, aClass.getCanonicalName()
                    + " should be included in the xstream white list");
            return true;
        });

        return xstream;
    }

    /**
     * Custom unmapped properties safe XStream instance factory method
     *
     * @return
     */
    public static XStream newXStreamInstance() {
        return newXStreamInstance(null);
    }
}