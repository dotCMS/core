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

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.caffine.CaffineCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.CacheOSGIService;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public class CacheProviderAPIImpl implements CacheProviderAPI, CacheOSGIService {

    private static final String CACHE_POOL_DEFAULT_CHAIN = "cache.default.chain";

    private static String defaultProvidersNoLicense;//Default providers with not ee lincense
    private static String defaultProviders;//Default providers with license

    private final Map<String, List<String>> configuredChainsPerRegion;//List of registered providers per region
    private final Map<String, CacheProvider> providersClasses;//List of ALL the registered providers
    private final Map<String, CacheProvider> defaultProvidersNoLicenseClasses;//List of the default registered providers

    private boolean isLicenseInitialized = false;

    public CacheProviderAPIImpl () {

        configuredChainsPerRegion = new ConcurrentHashMap<>();
        providersClasses = new ConcurrentHashMap<>();
        defaultProvidersNoLicenseClasses = new ConcurrentHashMap<>();

        /*
         This class stores in memory the initialized providers in order to reduce the performance impact as much as possible.
         Keep in mind this class is heavily used!.
         */

        //Reading the default providers properties
        defaultProvidersNoLicense = CaffineCache.class.getCanonicalName();
        defaultProviders = Config.getStringProperty(CACHE_POOL_DEFAULT_CHAIN, defaultProvidersNoLicense);

        //Lets try to find out what Providers we want to use
        Iterator<String> keysIterator = Config.subset("cache");
        while ( keysIterator.hasNext() ) {

            //Check if it is a region chain property and read the property
            String propertyKey = keysIterator.next();
            if ( propertyKey.endsWith(".chain") ) {

                String poolChainClassNames = Config.getStringProperty("cache." + propertyKey, "");

                //Now iterate each CacheProvider on this region
                String[] chainArray = poolChainClassNames.split(",");
                for ( String providerClassName : chainArray ) {

                    providerClassName = providerClassName.trim();

                    //First lets search if we already instantiated this provider
                    if ( !providersClasses.containsKey(providerClassName) ) {

                        try {

                            CacheProvider cacheProviderInstance;
                            try {
                                //Create a new instance of the provider
                                cacheProviderInstance = getInstanceFor(providerClassName);
                            } catch ( ClassNotFoundException e ) {
                                Logger.error(this, "Unable to get the class reference for the CacheProvider  [" + providerClassName + "].", e);
                                //Lets continue with the next given CacheProvider implementation
                                continue;
                            }

                            //Add the new provider instance to the list for registered providers
                            providersClasses.put(providerClassName, cacheProviderInstance);

                        } catch ( InstantiationException | IllegalAccessException e ) {
                            Logger.error(this, "Error instantiating CacheProvider [" + providerClassName + "].", e);
                            //Lets continue with the next given CacheProvider implementation
                        }
                    }

                }
            }

        }

        /*
        Get the list of default CacheProviders when no license is set, we do this for easy and quick access
         */
        String[] chainArray = defaultProvidersNoLicense.split(",");
        for ( String providerClassName : chainArray ) {

            providerClassName = providerClassName.trim();

            CacheProvider cacheProviderInstance = null;

            //Search in the already saved Providers
            if ( providersClasses.containsKey(providerClassName) ) {
                cacheProviderInstance = providersClasses.get(providerClassName);
            } else {
                try {
                    //Create a new instance of the provider
                    cacheProviderInstance = getInstanceFor(providerClassName);
                } catch ( Exception e ) {
                    Logger.error(this, "Unable to get the class reference for the CacheProvider  [" + providerClassName + "].", e);
                    //Lets continue with the next given CacheProvider implementation
                }
            }

            if ( cacheProviderInstance != null ) {
                //Now lets fill in the default CacheProviders list for easy access
                defaultProvidersNoLicenseClasses.put(providerClassName, cacheProviderInstance);
            } else {
                Logger.error(this, "CacheProvider not found [" + providerClassName + "].");
            }
        }

        //Registering this service to the OSGI context
        registerBundleService();
    }

    /**
     * Creates an instance of a given class name
     *
     * @param providerClassName
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    private CacheProvider getInstanceFor ( String providerClassName ) throws IllegalAccessException, InstantiationException, ClassNotFoundException {

        //Look for the provider class
        Class<CacheProvider> providerClass = (Class<CacheProvider>) Class.forName(providerClassName);
        //Create a new instance of the provider
        return providerClass.newInstance();
    }

    /**
     * Verifies if the server have a valid Enterprise License
     *
     * @return
     */
    private Boolean isValidLicense () {

        if ( !isLicenseInitialized ) {

             /*
             Validate if we can get use the LicenseUtil, the CacheLocator and CacheProviders
             are one of the first elements to be created, using the LicenseUtil here on a clean install
             can throw errors as the DB could not be even been loaded or a server id file could not be created
             and we don't want to stop the execution here for those expected cases.
             */
            String serverId = APILocator.getServerAPI().readServerId();
            if ( serverId == null ) {
                //We can continue, probably a first start
                Logger.debug(this, "Unable to get License level [server id is null].");
                return false;
            }

            //We can finally call directly the LicenseUtil.getLevel() method, the cluster_server was finally created!
            isLicenseInitialized = true;
        }

        if ( LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ) {
            return true;
        }

        return false;
    }

    /**
     * Return all the registered CacheProviders, there are cases when is required to iterate over all the Providers, like on
     * a flush or a shutdown
     *
     * @return
     */
    private List<CacheProvider> getAllProviders () {

        //If no license return the default providers
        if ( !isValidLicense() ) {
            return new ArrayList<>(defaultProvidersNoLicenseClasses.values());
        }

        //Or else return the complete list of providers
        return new ArrayList<>(providersClasses.values());
    }

    /**
     * Returns the list of CacheProviders to use for a given group, this class stores in
     * memory the initialized providers in order to impact as much as possible performance, keep in mind this class
     * is heavily used!.
     *
     * @param group
     * @return
     */
    private List<CacheProvider> getProvidersForRegion ( String group ) {

        //The case is very simple here, no license no chance to modify any region chain
        if ( !isValidLicense() ) {
            return new ArrayList<>(defaultProvidersNoLicenseClasses.values());
        }

        List<CacheProvider> providersForRegion = new ArrayList<>();
        List<String> poolChainClassNames;

        //First lets check if we already searched for this group in the past
        if ( configuredChainsPerRegion.containsKey(group) ) {
            poolChainClassNames = configuredChainsPerRegion.get(group);
        } else {

            //Read from the properties the cache chain to use for this region, if nothing found the default chain will be used
            String poolChainClassNamesValue = Config.getStringProperty("cache." + group + ".chain", defaultProviders);
            poolChainClassNames = Arrays.asList(poolChainClassNamesValue.split(","));

            //Added to the map, we want to avoid extra validations and checks as much as we can
            configuredChainsPerRegion.put(group, poolChainClassNames);
        }

        //Now iterate for each requested CacheProvider
        for ( String providerClassName : poolChainClassNames ) {

            CacheProvider cacheProviderInstance = null;

            //First lets search if we already requested this provider in the past
            if ( providersClasses.containsKey(providerClassName) ) {
                cacheProviderInstance = providersClasses.get(providerClassName);
            }

            if ( cacheProviderInstance != null ) {
                if ( !providersForRegion.contains(cacheProviderInstance) ) {

                    try {
                        /*
                        Check if the Provider was not already initialized, if not is probably because
                        the user didn't had a license and now have it (so he is using more providers now), or had a license and he removed it
                         */
                        if ( !cacheProviderInstance.isInitialized() ) {
                            cacheProviderInstance.init();
                        }

                        //Add to the list of providers to return
                        providersForRegion.add(cacheProviderInstance);

                    } catch ( Exception e ) {
                        //On Error we don't want to stop the execution of the other providers
                        Logger.error(this.getClass(), "Error initializing CacheProvider [" + cacheProviderInstance.getName() + "].", e);
                    }
                }
            } else {
                Logger.error(this, "CacheProvider not found [" + providerClassName + "].");
            }
        }

        return providersForRegion;
    }

    /**
     * Determines whether all Cache Providers are distributed
     */
    public boolean isDistributed() {
        return determineDistributed( getAllProviders() );
    }

    /**
     * Determines whether all Cache Providers registered for the given region are distributed
     *
     * @param group
     */
    public boolean isGroupDistributed( String group ) {
        return determineDistributed( getProvidersForRegion(group) );
    }

    private boolean determineDistributed(List<CacheProvider> providers) {

    	for(CacheProvider provider : providers) {
        	if (!provider.isDistributed()) {
        		return false;
        	}
        }

        return true;    	
    }

    /**
     * Registers this CacheOSGIService to the OSGI Context, in order to be use for OSGI plugins
     */
    public void registerBundleService () {

    	if (System.getProperty(WebKeys.OSGI_ENABLED) != null ) {
            //Register the CacheOSGIService
           // BundleContext context = HostActivator.instance().getBundleContext();
           // Hashtable<String, String> props = new Hashtable<>();
           // context.registerService(CacheOSGIService.class.getName(), this, props);
        }

    }

    @Override
    public void addCacheProvider ( String cacheRegion, Class<CacheProvider> cacheProviderClass ) throws Exception {
        throw new DotStateException("Not Implemented");
    }

    @Override
    public void removeCacheProvider ( Class<CacheProvider> cacheProviderClass ) {
        throw new DotStateException("Not Implemented");
    }

    @Override
    public void init () throws Exception {

        //Getting the list of all the cache providers
        List<CacheProvider> providers = getAllProviders();

        for ( CacheProvider provider : providers ) {

            try {
                provider.init();
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error initializing CacheProvider [" + provider.getName() + "].", e);
            }
        }
    }

    @Override
    public void put ( String group, String key, Object content ) {

        //Getting the list of cache providers to use for the given region
        List<CacheProvider> providersToUse = getProvidersForRegion(group);

        for ( CacheProvider provider : providersToUse ) {

            try {
                provider.put(group, key, content);
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error adding record to CacheProvider [" + provider.getName() + "]: group [" + group + "] - key [" + key + "].", e);
            }
        }
    }

    @Override
    public Object get ( String group, String key ) {

        //Getting the list of cache providers to use for the given region
        List<CacheProvider> providersToUse = getProvidersForRegion(group);
        
        // we shouldn't instantiate the providersAlreadyChecked List until we need to
        List<CacheProvider> providersAlreadyChecked = null;
        Object foundObject = null;
        for ( CacheProvider provider : providersToUse ) {
            try {
                foundObject = provider.get(group, key);
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error getting record from CacheProvider [" + provider.getName() + "]: group [" + group + "] - key [" + key + "].", e);
            }

            if ( foundObject != null ) {
            	if(providersAlreadyChecked != null){
            		for(CacheProvider p : providersAlreadyChecked) {
            			try {
                            p.put(group, key, foundObject);
                        } catch ( Exception e ) {
                            //On Error we don't want to stop the execution of the other providers
                            Logger.error(this.getClass(), "Error adding record to CacheProvider [" + provider.getName() + "]: group [" + group + "] - key [" + key + "].", e);
                        }
            		}
            	}
                //We already found something, we don't need to continue
                break;
            }else{
            	if(providersAlreadyChecked==null){
            		providersAlreadyChecked = new ArrayList<>(providersToUse.size());
            	}
            	providersAlreadyChecked.add(provider);
            }
        }

        return foundObject;
    }

    @Override
    public void remove ( String group, String key, boolean ignoreDistributed ) {

        //Getting the list of cache providers to use for the given region
        List<CacheProvider> providersToUse = Lists.reverse(getProvidersForRegion(group));

        for ( CacheProvider provider : providersToUse ) {

            try {
            	if (!(ignoreDistributed && provider.isDistributed())){
                    provider.remove(group, key);
            	}
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error removing record from CacheProvider [" + provider.getName() + "]: group [" + group + "] - key [" + key + "].", e);
            }
        }
    }

    @Override
    public void remove ( String group, boolean ignoreDistributed ) {

        //Getting the list of cache providers to use for the given region
        List<CacheProvider> providersToUse = Lists.reverse(getProvidersForRegion(group));

        for ( CacheProvider provider : providersToUse ) {

            try {
            	if (!(ignoreDistributed && provider.isDistributed())){
            		provider.remove(group);
            	}
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error removing group from CacheProvider [" + provider.getName() + "]: group [" + group + "].", e);
            }
        }
    }

    @Override
    public void removeAll (boolean ignoreDistributed) {

        //Getting the list of all the cache providers
        List<CacheProvider> providers = Lists.reverse(getAllProviders());

        for ( CacheProvider provider : providers ) {

            try {
            	if (!(ignoreDistributed && provider.isDistributed())){
            		provider.removeAll();
            	}
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error flushing CacheProvider [" + provider.getName() + "].", e);
            }
        }
    }

    @Override
    public Set<String> getGroups () {

        //Getting the list of all the cache providers
        List<CacheProvider> providers = getAllProviders();

        Set<String> groups = new HashSet<>();

        for ( CacheProvider provider : providers ) {

            try {
                Set<String> foundGroups = provider.getGroups();

                if ( foundGroups != null ) {
                    groups.addAll(foundGroups);
                }
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error retrieving groups from CacheProvider [" + provider.getName() + "].", e);
            }
        }

        return groups;
    }

    @Override
    public List<CacheProviderStats> getStats () {

        //Getting the list of all the cache providers
        List<CacheProvider> providers = getAllProviders();
        List<CacheProviderStats> ret = new ArrayList<>();
        for ( CacheProvider provider : providers ) {

            try {
                CacheProviderStats providerStat = provider.getStats();
                if(providerStat != null){
                    ret.add(providerStat);
                }

            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error calculating stats from CacheProvider [" + provider.getName() + "].", e);
            }
        }

        return ret;
    }

    @Override
    public void shutdown () {

        //Getting the list of all the cache providers
        Set<CacheProvider> providers = new HashSet<>(providersClasses.values());
        providers.addAll(defaultProvidersNoLicenseClasses.values());

        for ( CacheProvider provider : providers ) {

            try {
                if ( provider.isInitialized() ) {
                    provider.shutdown();
                }
            } catch ( Exception e ) {
                //On Error we don't want to stop the execution of the other providers
                Logger.error(this.getClass(), "Error shutting down CacheProvider [" + provider.getName() + "].", e);
            }
        }
    }

    /**
     * Internal comparator class used to order stat results (getStats())
     */
    public class CacheComparator implements Comparator<Map<String, Object>> {

        public int compare ( Map<String, Object> o1, Map<String, Object> o2 ) {

            if ( o1 == null && o2 != null ) return 1;
            if ( o1 != null && o2 == null ) return -1;
            if ( o1 == null && o2 == null ) return 0;

            String group1 = (String) o1.get("region");
            String group2 = (String) o2.get("region");

            if ( !UtilMethods.isSet(group1) && !UtilMethods.isSet(group2) ) {
                return 0;
            } else if ( UtilMethods.isSet(group1) && !UtilMethods.isSet(group2) ) {
                return -1;
            } else if ( !UtilMethods.isSet(group1) && UtilMethods.isSet(group2) ) {
                return 1;
            } else if ( group1.equals(group2) ) {
                return 0;
            } else if ( group1.startsWith(WORKING_CACHE_PREFIX) && group2.startsWith(LIVE_CACHE_PREFIX) ) {
                return 1;
            } else if ( group1.startsWith(LIVE_CACHE_PREFIX) && group2.startsWith(WORKING_CACHE_PREFIX) ) {
                return -1;
            } else if ( !group1.startsWith(LIVE_CACHE_PREFIX) && !group1.startsWith(WORKING_CACHE_PREFIX) && (group2.startsWith(LIVE_CACHE_PREFIX) || group2.startsWith(WORKING_CACHE_PREFIX)) ) {
                return -1;
            } else if ( (group1.startsWith(LIVE_CACHE_PREFIX) || group1.startsWith(WORKING_CACHE_PREFIX)) && !group2.startsWith(LIVE_CACHE_PREFIX) && !group2.startsWith(WORKING_CACHE_PREFIX) ) {
                return 1;
            } else { // neither group1 nor group2 are live or working
                return group1.compareToIgnoreCase(group2);
            }
        }

    }

}