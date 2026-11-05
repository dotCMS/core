package com.dotcms.util.xstream;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.extended.JavaClassConverter;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.thoughtworks.xstream.security.NoPermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;

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
                            throw refusal(e);
                        }
                    }
                };
            }

            @Override
            public Object unmarshal(final HierarchicalStreamReader reader, final Object root,
                    final DataHolder dataHolder) {
                try {
                    return super.unmarshal(reader, root, dataHolder);
                } catch (final TrustedListException e) {
                    //Recorded here rather than where the refusal is raised, because XStream
                    //caches a refusal by element name: the second document naming the same type
                    //is turned away without the mapper below being entered again. An instance
                    //is held for the life of the JVM, so recording at the point of refusal
                    //would leave every import after the first one with no trace at all.
                    e.record();
                    throw e;
                }
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
        //Built by hand rather than through denyTypesByWildcard() for the flag: a wildcard skips
        //anonymous types by default, which keeps the allow list conservative but would leave a
        //type such as com.dotcms.repackage.x.Foo$1 outside both sides and therefore back on
        //XStream's own defaults, where any map, collection, number, throwable or member passes.
        xstream.addPermission(new NoPermission(
                new WildcardTypePermission(true, TrustedListMatcher.deniedPatterns)));

        //A document names a type in two ways: as an element name, and as the text of a
        //<java-class> element, which is how a Class held in a field is written. XStream builds
        //the converter for the second one on a bare DefaultMapper, so that path never reaches
        //the permissions at all. Replacing it puts both ways under the same trusted list.
        xstream.registerConverter(
                new TrustedListClassConverter(xstream.getClassLoaderReference(),
                        xstream.getMapper()), XStream.PRIORITY_NORMAL);

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
     * Reads a {@code <java-class>} reference through the instance's own mapper, so the type it
     * names is checked against the trusted list like any other.
     *
     * <p>Only reading is changed. The inherited {@code toString} keeps writing through the
     * plain mapper this is constructed with, exactly as XStream's own converter does, so a
     * bundle written by this version names its types the same way an older version would and
     * stays readable by one.</p>
     */
    private static final class TrustedListClassConverter extends JavaClassConverter {

        private final Mapper secured;

        /**
         * @param classLoaderReference the instance's class loader, used for writing
         * @param secured the instance's full mapper chain, used for reading
         */
        private TrustedListClassConverter(final ClassLoaderReference classLoaderReference,
                final Mapper secured) {
            super(classLoaderReference);
            this.secured = secured;
        }

        @Override
        public Object fromString(final String str) {
            try {
                return secured.realClass(str);
            } catch (final CannotResolveClassException e) {
                throw new ConversionException("Cannot load java class " + str, e.getCause());
            }
        }
    }

    /**
     * Turns XStream's bare refusal into one an operator can act on, and records it where a
     * refusal belongs.
     *
     * <p>XStream's own exception carries the refused type's name and nothing else, so an
     * administrator looking at the failed bundle in the Push Publishing portlet — which shows
     * the exception's message — would learn which type stopped the import but not what to do
     * about it. The message built here names the property that would let the type through.</p>
     *
     * <p>Building the refusal does not record it. That happens once the read it belongs to has
     * actually failed, in {@link TrustedListException#record()}, for the caching reason set out
     * there.</p>
     *
     * @param cause XStream's refusal, whose message is the refused type's name
     * @return the refusal to throw in its place
     */
    private static TrustedListException refusal(final ForbiddenClassException cause) {
        return new TrustedListException(String.format(
                "`%s` is not in the xstream trusted list and was not read while importing "
                        + "bundle. Add its package to `%s` if the bundle is expected to carry "
                        + "it.",
                cause.getMessage(), TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY));
    }

    /**
     * The refusal raised when a bundle names a type the trusted list does not cover.
     *
     * <p>It extends XStream's own refusal on purpose. XStream treats that type specially in two
     * places — its caching mapper remembers a refusal so the same element is not resolved twice,
     * and its unmarshaller re-throws it untouched instead of wrapping it in a conversion error
     * — and both behaviours are the ones we want, the second being what lets the message reach
     * the caller intact. The first is also why recording lives in {@link #record()} rather than
     * in the constructor. Only the message differs, and it has to be supplied here because
     * {@link ForbiddenClassException} builds its own from a {@link Class}.</p>
     */
    public static final class TrustedListException extends ForbiddenClassException {

        private static final long serialVersionUID = 20261103L;

        private final String message;

        private TrustedListException(final String message) {
            super((Class<?>) null);
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        /**
         * Writes this refusal to the application log and to the security log.
         *
         * <p>Called when a read fails, not when the refusal is built, and deliberately so.
         * XStream caches a refusal by element name and re-throws the same instance for every
         * later document naming that type, without consulting the mapper again; since a
         * serializer is kept for the life of the JVM, recording at the point of refusal would
         * mean one entry for the first failed import and none for any that followed.</p>
         *
         * <p>The line goes to the security log as well as the application log. A type outside
         * the trusted list arriving in a bundle is either an installation that needs the
         * property set or a bundle carrying something it should not, and the second case
         * belongs in the log that is collected and reviewed as a security trail.</p>
         */
        private void record() {
            Logger.warn(XStreamHandler.class, message);
            SecurityLogger.logWarn(XStreamHandler.class, message);
        }
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
     * matching itself is XStream's, through {@code allowTypesByWildcard} for the first list and
     * a {@code WildcardTypePermission} wrapped in a {@code NoPermission} for the second, so
     * there is one implementation of the wildcard rules rather than a second one here that
     * could answer differently.
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
