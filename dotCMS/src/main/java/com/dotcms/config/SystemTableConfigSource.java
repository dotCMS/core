package com.dotcms.config;

import com.dotcms.business.SystemTable;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Encapsulates the configuration based on the system table
 * This is a dynamic configuration and can be changed at runtime
 * @author jsanca
 */
public class SystemTableConfigSource {

    // there are some keys that can not be configured on the system table, all which includes concurrent are not
    private static final Set<String> SKIP_KEYS_LOWER_SET = Set.of("concurrent", "scheduler");

    /**
     * Keys that are provisioning settings: they are read from the environment or the properties
     * file only, and a value stored for them in the system table has no effect. They decide which
     * addresses the server may contact on behalf of a user-supplied URL and which files it may
     * download, so they must not be changeable from inside the application. Matched as whole
     * keys, without regard to letter case, with or without the {@code DOT_} prefix.
     */
    public static final Set<String> PROVISIONING_KEYS = Set.of(
            "REMOTE_CALL_SUBNET_BLACKLIST",
            "REMOTE_CALL_SUBNET_ALLOWLIST",
            "ALLOW_ACCESS_TO_PRIVATE_SUBNETS",
            "REMOTE_CALL_ALLOW_REDIRECTS",
            "REMOTE_CALL_ALLOW_IPV6",
            "TEMP_RESOURCE_BY_URL_ADMIN_ONLY",
            "TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS");

    private static final String[] DEFAULT_BLACKLISTED_PROPS = {"DOTCMS_CLUSTER_ID", "DOTCMS_CLUSTER_SALT"};
    /** The blacklist built for the last value read of {@code SYSTEM_TABLE_BLACKLISTED_KEYS}, rebuilt when it changes. */
    private static final AtomicReference<Tuple2<List<String>, WhiteBlackList>> REFUSED_KEYS = new AtomicReference<>();
    /** Refuses every key while the configured blacklist pattern does not compile. */
    private static final WhiteBlackList REFUSE_ALL = new WhiteBlackList.Builder().addBlackPatterns(".*").build();

    private static final String ENV_PREFIX = "DOT_";
    private final Lazy<SystemTable> systemTable = Lazy.of(()->APILocator.getSystemAPI().getSystemTable());

    public SystemTableConfigSource() {
        Logger.info(this.getClass(), "Creating SystemTableConfigSource");
    }

    //@Override
    public int getOrdinal() {
        return 900;
    }

    //@Override
    public Set<String> getPropertyNames() {

        return this.systemTable.get().all().keySet();
    }

    // @Override
    public String getValue(final String propertyName) {

        Optional<String> valueOpt = Optional.empty();
        if (null != propertyName && this.isAllowed(propertyName)) {
            valueOpt = this.systemTable.get().get(propertyName);
        }
        return valueOpt.orElse(null);
    }

    private boolean isAllowed(final String propertyName) {
        return isServedFromSystemTable(propertyName);
    }

    /**
     * Tells whether a configuration key is one of the {@link #PROVISIONING_KEYS}. The key is
     * matched whole, without regard to letter case or surrounding spaces, with or without the
     * {@code DOT_} prefix.
     *
     * @param propertyName the key as configuration code or a REST request names it
     * @return {@code true} when the key is a provisioning key
     */
    public static boolean isProvisioningKey(final String propertyName) {
        if (!UtilMethods.isSet(propertyName)) {
            return false;
        }
        final String upper = propertyName.trim().toUpperCase(Locale.ROOT);
        final String bare = upper.startsWith(ENV_PREFIX) ? upper.substring(ENV_PREFIX.length()) : upper;
        return PROVISIONING_KEYS.contains(bare);
    }

    /**
     * Tells whether a configuration key may take its value from the system table. Keys in
     * {@link #PROVISIONING_KEYS} never do, and neither do keys that contain {@code concurrent}
     * or {@code scheduler}; every other key does.
     *
     * @param propertyName the key as configuration code asks for it, with or without the
     *                     {@code DOT_} prefix
     * @return {@code true} when a system table value for the key is used
     */
    public static boolean isServedFromSystemTable(final String propertyName) {
        if (!UtilMethods.isSet(propertyName) || isProvisioningKey(propertyName)) {
            return false;
        }
        final String lowerPropertyName = propertyName.toLowerCase(Locale.ROOT);
        return SKIP_KEYS_LOWER_SET.stream().noneMatch(lowerPropertyName::contains);
    }

    /**
     * Builds the system table access rules from the caller's allowlist and the configured blacklist.
     * An invalid blacklist is rejected instead of allowing access.
     */
    public static WhiteBlackList tableAccessRules(final String[] allowedPatterns) {
        return new WhiteBlackList.Builder().addWhitePatterns(allowedPatterns)
                .addBlackPatterns(blacklistedKeys()).build();
    }

    /**
     * Returns a check for keys blocked by the system table access rules:
     * {@code DOTCMS_CLUSTER_ID}, {@code DOTCMS_CLUSTER_SALT} and the patterns in {@code SYSTEM_TABLE_BLACKLISTED_KEYS}.
     * The blacklist is read when this method is called, so callers that check many keys in a row read it once. Callers
     * use it to hide those keys in configuration and audit output. If the blacklist has an invalid pattern,
     * every key is treated as refused and a warning is logged.
     *
     * @return a predicate that is {@code true} for a key on the blacklist
     */
    public static Predicate<String> refusedKeys() {
        final WhiteBlackList refused = refusedKeyList();
        return key -> UtilMethods.isSet(key) && !refused.isAllowed(key);
    }

    /**
     * Returns the blacklist for the current value of {@code SYSTEM_TABLE_BLACKLISTED_KEYS}, built again only when that
     * value changes.
     */
    private static WhiteBlackList refusedKeyList() {
        final List<String> patterns = List.of(blacklistedKeys());
        Tuple2<List<String>, WhiteBlackList> refused = REFUSED_KEYS.get();
        if (refused == null || !refused._1().equals(patterns)) {
            final WhiteBlackList list = Try.of(() -> new WhiteBlackList.Builder().addBlackPatterns(patterns).build())
                    .onFailure(e -> Logger.warn(SystemTableConfigSource.class, "SYSTEM_TABLE_BLACKLISTED_KEYS holds an "
                            + "invalid pattern, every key is treated as blacklisted: " + e.getMessage()))
                    .getOrElse(REFUSE_ALL);
            refused = Tuple.of(patterns, list);
            REFUSED_KEYS.set(refused);
        }
        return refused._2();
    }

    /**
     * Returns the blacklisted key patterns: {@code SYSTEM_TABLE_BLACKLISTED_KEYS} plus the default ones.
     */
    private static String[] blacklistedKeys() {
        return CollectionsUtils.concat(Config.getStringArrayProperty(
                "SYSTEM_TABLE_BLACKLISTED_KEYS", new String[]{}), DEFAULT_BLACKLISTED_PROPS);
    }

    public String put(final String propertyName, final String value) {

        this.systemTable.get().set(propertyName, value);
        return value;
    }

    public void remove(final String propertyName) {
        this.systemTable.get().delete(propertyName);
    }

    public void clear() {
        this.systemTable.get().all().keySet()
                .forEach(key -> this.systemTable.get().delete(key));
    }

    //@Override
    public String getName() {
        return SystemTableConfigSource.class.getSimpleName();
    }

}
