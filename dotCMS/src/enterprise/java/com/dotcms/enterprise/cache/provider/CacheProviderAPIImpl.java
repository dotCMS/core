/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.cache.provider;


import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.CacheOSGIService;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.caffine.CaffineCache;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jonathan Gamba
 *         Date: 8/31/15
 */
public class CacheProviderAPIImpl implements CacheProviderAPI, CacheOSGIService {

    private static final String CACHE_POOL_DEFAULT_CHAIN = "cache.default.chain";

    // we use a cache for providers because ConcurrentHashMap has a recursion problem in its computeIfAbsent method
    private static final Cache<String, List<CacheProvider>> configuredChainsPerRegion = Caffeine.newBuilder().maximumSize(10000).build();
    private final List<String> noLicenseProviders = List.of(CaffineCache.class.getCanonicalName());

    private Map<String, CacheProvider> singletonProviders = new ConcurrentHashMap<>();

    public CacheProviderAPIImpl () {

    }


    private Optional<CacheProvider> getInstanceFor ( String providerClassName ) {
        return Try.of(() -> {
            Class<CacheProvider> providerClass = (Class<CacheProvider>) Class.forName(providerClassName.trim());
            CacheProvider provider = providerClass.newInstance();
            if (provider.isSingleton()) {
                return singletonProviders.computeIfAbsent(providerClassName, k -> provider);
            }
            return provider;
        }).onFailure(e -> Logger.error(this, "Error creating CacheProvider [" + providerClassName + "].", e)).toJavaOptional();
    }



    /**
     * Return all the registered CacheProviders, there are cases when is required to iterate over all the Providers, like on
     * a flush or a shutdown
     *
     */
    private List<CacheProvider> getAllProviders () {

        Map<String,CacheProvider> providers = new HashMap<>();

        for(List<CacheProvider> providerList : configuredChainsPerRegion.asMap().values()){
            providerList.forEach(p->providers.put(p.getClass().getCanonicalName(),p));
        }
        return new ArrayList<>(providers.values());


    }



    private List<String> getProviderNamesPerRegion(String group){
        if (null == group ) {
            return noLicenseProviders;
        }
        //Read from the properties the cache chain to use for this region, if nothing found the default chain will be used
        String[] poolChainClassNames = Config.getStringArrayProperty("cache." + group.toLowerCase() + ".chain",
                new String[0]);

        if(poolChainClassNames.length>0){
            return Arrays.asList(poolChainClassNames);
        }

        return Arrays.asList(Config.getStringArrayProperty(CACHE_POOL_DEFAULT_CHAIN,new String[]{ CaffineCache.class.getCanonicalName()}));


    }


    /**
     * Returns the list of CacheProviders to use for a given group, this class stores in
     * memory the initialized providers in order to impact as much as possible performance, keep in mind this class
     * is heavily used!.
     *
     * @param group
     */
    private List<CacheProvider> getProvidersForRegion ( String group ) {

        //The case is very simple here, no license no chance to modify any region chain

        return configuredChainsPerRegion.get(group, k -> {
            List<CacheProvider> providers = new ArrayList<>();
            for(String providerClassName : getProviderNamesPerRegion(group)){
                getInstanceFor(providerClassName).ifPresent(providers::add);
            }
            return List.copyOf(initProviders(providers));
        });

    }

    List<CacheProvider> initProviders(List<CacheProvider> cacheProviders) {
        cacheProviders.forEach(provider ->
            Try.run(provider::init).onFailure(
                    e -> Logger.error(this, "Error initializing CacheProvider [" + provider.getName() + "]." + e.getMessage(), e))
        );
        return cacheProviders;
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
        // not implemented

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
        configuredChainsPerRegion.invalidateAll();
        shutdownProviders(providers);


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

        List<CacheProviderStats> ret = new ArrayList<>();
        for ( CacheProvider provider : getAllProviders() ) {

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

        shutdownProviders(List.copyOf(getAllProviders()));
    }

    private void shutdownProviders(Collection<CacheProvider> providers) {

        for ( CacheProvider provider : providers ) {
            Try.run(provider::shutdown).onFailure(e -> Logger.error(this, "Error shutting down CacheProvider [" + provider.getName() + "].", e));
            singletonProviders.remove(provider.getClass().getCanonicalName());
        }
    }



}
