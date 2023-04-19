/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.cache.provider;

import com.dotmarketing.business.cache.provider.CacheProviderStats;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that will register and handle the different {@link com.dotmarketing.business.cache.provider.CacheProvider} implementations
 * in dotCMS, in order to defined custom CacheProviders is required an Enterprise
 * License, without it only the default CacheProviders can be use ({@link com.dotmarketing.business.cache.provider.guava.GuavaCache}
 * <br/>
 * <br/>
 * With a valid Enterprise License the CacheProviders to use can be specified using properties specifying the chain for a specific cache
 * region <strong>cache.mycacheregionexample.chain</strong> in the <strong>dotmarketing-config.properties</strong> file,
 * with those properties you can specify the list of classes to use as CacheProviders for each region, that list will also define the order of execution of those providers.
 * <p/>
 * In order to define a default chain to use for cache regions that are not specified in the <strong>dotmarketing-config.properties</strong>
 * the property <strong>cache.default.chain</strong> must be used.
 * <p/>
 * <strong>Examples:</strong>
 * <ul>
 * <li>cache.default.chain=com.dotmarketing.business.cache.provider.guava.TestCacheProvider,com.dotmarketing.business.cache.provider.guava.GuavaCache,com.dotmarketing.business.cache.provider.h2.H2CacheLoader</li>
 * <li>cache.velocitymemoryonlycache.chain=com.dotmarketing.business.cache.provider.guava.GuavaCache</li>
 * <li>cache.velocityuservtlcache.chain=com.dotmarketing.business.cache.provider.redis.RedisProvider,com.dotmarketing.business.cache.provider.h2.H2CacheLoader</li>
 * </ul>
 *
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public interface CacheProviderAPI {

    public static final String LIVE_CACHE_PREFIX = "livecache";
    public static final String WORKING_CACHE_PREFIX = "workingcache";
    public static final String DEFAULT_CACHE = "default";

    /**
     * Registers this service to the OSGI Context, in order to be use for OSGI plugins
     */
    void registerBundleService ();

    /**
     * Initializes all the registered Cache Providers
     *
     * @throws Exception
     */
    void init () throws Exception;

    /**
     * Determines whether all Cache Providers are distributed
     */
    boolean isDistributed();

    /**
     * Determines whether all Cache Providers registered for the given region are distributed
     *
     * @param group
     */
    boolean isGroupDistributed ( String group );

    /**
     * Adds the given content to the given region and for a given key to
     * all the registered Cache Providers
     *
     * @param group
     * @param key
     * @param content
     */
    void put ( String group, String key, final Object content );

    /**
     * Searches and return the content in a given region and with a given key, this
     * method will try to search for the content in all the registered Cache Providers
     * but as soon as a record is found will stop the search and return that record.
     *
     * @param group
     * @param key
     * @return
     */
    Object get ( String group, String key );

    /**
     * Invalidates the given region in all the registered Cache Providers
     * Depending on value for ignoreDistributed flag, it won't invalidates on distributed Cache Providers
     * @param group
     * @param ignoreDistributed
     */
    void remove ( String group, boolean ignoreDistributed );

    /**
     * Invalidates a given key for a given region in all the registered Cache Providers
     * Depending on value for ignoreDistributed flag, it won't invalidates on distributed Cache Providers
     * @param group
     * @param key
     * @param ignoreDistributed
     */
    void remove ( String group, String key, boolean ignoreDistributed );

    /**
     * Invalidates all the regions in all the registered Cache Providers
     * Depending on value for ignoreDistributed flag, it won't invalidates on distributed Cache Providers
     * @param ignoreDistributed
     */
    void removeAll (boolean ignoreDistributed);

    /**
     * Returns all the regions found in all the registered Cache Providers
     *
     * @return
     */
    Set<String> getGroups ();

    /**
     * Returns stat data found in all the registered Cache Providers
     *
     * @return
     */
    List<CacheProviderStats> getStats ();

    /**
     * Shutdowns all the registered Cache Providers
     */
    void shutdown ();

}