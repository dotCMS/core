package com.dotcms.util.xstream;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ForbiddenClassException;

public class XStreamHandler {

    /**
     * Name of the configuration property that extends the trusted list with extra package
     * patterns. Named in the rejection message so whoever reads the log knows what to set.
     */
    static final String TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY = "xstream.trusted.list.extra.patterns";

    private XStreamHandler(){

    }

    /**
     * Builds an {@link XStream} instance that reads back only the types a bundle is expected to
     * carry, and that skips properties an older version wrote and this one no longer maps.
     *
     * <p>Callers may add aliases and converters to the instance they get. They must not call
     * {@code allowTypes*} on it: XStream evaluates the most recently registered permission
     * first, so a late addition would outrank the exclusions set up here.</p>
     *
     * @param encoding the character encoding of the documents this instance reads and writes,
     *                 or {@code null} for the platform default
     * @return a configured instance, not shared with any other caller
     */
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

                    @Override
                    public Class realClass(final String elementName) {
                        try {
                            return super.realClass(elementName);
                        } catch (final ForbiddenClassException e) {
                            Logger.warn(XStreamHandler.class, String.format(
                                    "`%s` is not in the xstream trusted list and was not read "
                                            + "while importing bundle. Add its package to `%s` "
                                            + "if the bundle is expected to carry it.",
                                    elementName, TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY));
                            throw e;
                        }
                    }
                };
            }
        };

        //Allow only the classes that are in the trusted list. XStream's constructor already
        //starts from NoTypePermission.NONE and adds back nulls, primitives, arrays, interfaces
        //and a short list of JDK types (String, Number, Enum, Class, Date, java.time, ...), so
        //only the packages a bundle carries on top of that need to be listed here.
        xstream.allowTypesByWildcard(TrustedListMatcher.allowedPatterns());

        //Registered AFTER the allow on purpose: addPermission() inserts at index 0, so the last
        //permission registered is the first one evaluated. Written the other way around, the
        //com.dotcms wildcard would answer first and the denied subpackage would never be reached.
        xstream.denyTypesByWildcard(TrustedListMatcher.deniedPatterns);

        return xstream;
    }

    /**
     * Builds an instance with the platform default encoding. See
     * {@link #newXStreamInstance(String)} for what the instance will and will not read.
     *
     * @return a configured instance, not shared with any other caller
     */
    public static XStream newXStreamInstance() {
        return newXStreamInstance(null);
    }

    /**
     * Returns the configured extension to the trusted list exactly as it reads in the
     * configuration. Callers that keep an instance around compare this value to the one their
     * instance was built from, and rebuild when it differs.
     *
     * @return the property's current value, or {@code null} when nothing is configured
     */
    public static String trustedListFingerprint() {
        return Config.getStringProperty(TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY, null);
    }

    /**
     * An {@link XStream} instance kept for reuse that is replaced when the trusted-list
     * configuration changes.
     *
     * <p>Building an instance is not free, so the paths that read many documents hold on to one.
     * Holding it for the life of the JVM, though, would mean a configured extension to the
     * trusted list only took effect after a restart — which is the one moment it is needed, with
     * an installation's publishing already failing. This keeps the instance and the
     * configuration it was built from together, and builds a new one when they diverge.</p>
     */
    public static final class Cached {

        private final String encoding;
        private final Consumer<XStream> customizer;
        private volatile Holder holder;

        private record Holder(String fingerprint, XStream xstream) {}

        /**
         * @param encoding the encoding to build instances with, or {@code null} for the default
         * @param customizer applied to every instance this holder builds, for the aliases and
         *                   converters the caller needs
         */
        public Cached(final String encoding, final Consumer<XStream> customizer) {
            this.encoding = encoding;
            this.customizer = customizer;
        }

        /**
         * The instance to use now, rebuilt first if the configuration changed since the last
         * call.
         *
         * @return a configured instance, shared by every caller of this holder
         */
        public XStream get() {
            final String fingerprint = trustedListFingerprint();
            final Holder current = holder;
            if (current != null && Objects.equals(fingerprint, current.fingerprint())) {
                return current.xstream();
            }
            final XStream xstream = newXStreamInstance(encoding);
            if (customizer != null) {
                customizer.accept(xstream);
            }
            final Holder rebuilt = new Holder(fingerprint, xstream);
            holder = rebuilt;
            return rebuilt.xstream();
        }
    }

    /**
     * The package patterns a bundle's types are checked against. Holds the lists only — the
     * matching itself is XStream's, through {@code allowTypesByWildcard} and
     * {@code denyTypesByWildcard}, so there is one implementation of the wildcard rules rather
     * than a second one here that could answer differently.
     */
    public static class TrustedListMatcher {

        private TrustedListMatcher(){
            //Private constructor to avoid instantiation
        }

        /**
         * Packages whose types a push-publishing bundle legitimately carries.
         *
         * <p>{@code com.liferay.portal.model} is here because the user wrapper carries a
         * {@link com.liferay.portal.model.User}. {@code java.lang} is deliberately absent: the
         * types a bundle needs from it — text, numbers, enums and {@link Class} — are already
         * covered by XStream's own defaults, so listing the package would only add the ones a
         * bundle never carries.</p>
         */
        static final String[] patterns = {
                "com.dotcms.**", "com.dotmarketing.**", "com.liferay.portal.model.**",
                "com.google.common.collect.**",
                "java.util.**", "java.sql.**"
        };

        /**
         * Packages that sit inside a trusted one but are not trusted themselves.
         *
         * <p>{@code com.dotcms.repackage} holds the shaded third-party libraries — a little over
         * a hundred of them — which a bundle has no reason to carry. They are nested inside the
         * {@code com.dotcms} namespace that bundles do use, so they have to be taken back out
         * rather than simply left off the list above.</p>
         */
        static final String[] deniedPatterns = {
                "com.dotcms.repackage.**"
        };

        /**
         * The patterns a new {@link XStream} instance should trust: the ones shipped with
         * dotCMS plus whatever the installation added through configuration.
         *
         * @return every trusted pattern, shipped ones first
         */
        static String[] allowedPatterns() {
            final String[] extra = currentExtraPatterns().wildcards();
            if (extra.length == 0) {
                return patterns.clone();
            }
            final String[] combined = Arrays.copyOf(patterns, patterns.length + extra.length);
            System.arraycopy(extra, 0, combined, patterns.length, extra.length);
            return combined;
        }

        /**
         * The configured extension, parsed and compiled.
         *
         * @param rawValue the property's value exactly as configured, kept so the parsed form
         *                 can be reused until it changes
         * @param wildcards the individual patterns the value was split into
         */
        private record ExtraPatterns(String rawValue, String[] wildcards) {}

        private static volatile ExtraPatterns extraPatterns =
                new ExtraPatterns(null, new String[0]);

        /**
         * Reads the configured extension, re-parsing it only when the property has changed
         * since the last read. Re-reading on every call is what lets an installation widen the
         * trusted list without a restart; caching on the raw value is what keeps that from
         * costing a recompile each time.
         *
         * @return the current extension
         */
        private static ExtraPatterns currentExtraPatterns() {
            final String rawValue =
                    Config.getStringProperty(TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY, null);
            final ExtraPatterns current = extraPatterns;
            if (Objects.equals(rawValue, current.rawValue())) {
                return current;
            }
            final String[] wildcards = UtilMethods.isSet(rawValue)
                    ? Arrays.stream(rawValue.split(","))
                            .map(String::trim)
                            .filter(pattern -> !pattern.isEmpty())
                            .toArray(String[]::new)
                    : new String[0];
            final ExtraPatterns rebuilt = new ExtraPatterns(rawValue, wildcards);
            extraPatterns = rebuilt;
            return rebuilt;
        }
    }

}
