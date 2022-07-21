package com.dotcms.cli.common;

/**
 * Common place for general utitliy methods
 */
public class CommandUtils {

    private static final String DOTCMS_CLIENT_SERVERS_PREFIX = "dotcms.client.servers.";
    private static final String DOTCMS_CLIENT_SERVERS_PROFILE = DOTCMS_CLIENT_SERVERS_PREFIX+"%s";

    /**
     * In our configuration files dotCMS instances are identified by unique property name like:
     * dotcms.client.servers.demo=https://demo.dotcms.com/api
     * While the configuration holder DotCmsClientConfig uses as key on the suffix.
     * Therefore, we need convert from the long reproresnetation to the short prefix back and forth
     * This method takes the prefix and prepends the fully qualified name to it.
     * @param suffix something like `demo` a short postfix to differentiate the instance name
     * @return the full instance config property name
     */
    public static String instanceName(final String suffix){
        return String.format(DOTCMS_CLIENT_SERVERS_PROFILE,suffix);
    }

    /**
     * In our configuration files dotCMS instances are identified by unique property name like:
     * dotcms.client.servers.demo=https://demo.dotcms.com/api
     * While the configuration holder DotCmsClientConfig uses as key on the suffix.
     * Therefore, we need convert from the long representation to the short prefix back and forth
     * This method takes the prefix and prepends the fully qualified name to it.
     * @param profileName something like `dotcms.client.servers.demo` the fully qualified property that identifies the instance name
     * @return prefix in this example should return `demo`
     */
    public static String instanceSuffix(final String profileName){
        return profileName.replace(DOTCMS_CLIENT_SERVERS_PREFIX,"");
    }

}
