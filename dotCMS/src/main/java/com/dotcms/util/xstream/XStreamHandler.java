package com.dotcms.util.xstream;

import java.util.regex.Pattern;
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

        //Allow only the classes that are in the trusted list
        xstream.allowTypesByWildcard(TrustedListMatcher.patterns);

        xstream.addPermission(aClass -> {
            //If the class is not in the trusted list, log a warning
            if (!TrustedListMatcher.matches(aClass)) {
                Logger.warn(aClass, aClass.getCanonicalName()
                        + " should be included in the xstream trusted list");
            }
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

    public static class TrustedListMatcher {

        private TrustedListMatcher(){
            //Private constructor to avoid instantiation
        }

        static final String[] patterns = {
                "com.dotcms.**", "com.dotmarketing.**", "com.google.common.collect.**",
                "java.lang.**",
                "java.util.**", "java.sql.**", "com.thoughtworks.xstream.mapper.**"
        };

        private static final Pattern[] compiledPatterns = compilePatterns();

        private static Pattern[] compilePatterns() {
            final Pattern[] compiledPatterns = new Pattern[TrustedListMatcher.patterns.length];
            for (int i = 0; i < TrustedListMatcher.patterns.length; i++) {
                // Replace '**' with '.*' to match any subpackage or class
                final String regex = TrustedListMatcher.patterns[i].replace("**", ".*");
                compiledPatterns[i] = Pattern.compile(regex);
            }
            return compiledPatterns;
        }

        public static boolean matches(Class<?> clazz) {
            String className = clazz.getName();
            for (Pattern pattern : compiledPatterns) {
                if (pattern.matcher(className).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

}