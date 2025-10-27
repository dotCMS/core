package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsCache.CACHE_404;
import static com.dotcms.security.apps.AppsUtil.digest;
import static com.dotcms.security.apps.SecretsKeyStoreHelper.SECRETS_KEYSTORE_PASSWORD_KEY;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This is basically a safe repository implemented using java.security.KeyStore
 * Which according to the official Java documentation Represents a storage facility for cryptographic keys and certificates.
 * This Class serves as the entry point to the repository whose implantation really resides in SecretsKeyStoreHelper
 * This class adds caching
 */
public class SecretCachedKeyStoreImpl implements SecretsStore {

    private final SecretsKeyStoreHelper secretsKeyStore;
    private final AppsCache cache;

    SecretCachedKeyStoreImpl(final AppsCache cache) {
        this.secretsKeyStore = new SecretsKeyStoreHelper(
                        () -> cache.getFromCache(SECRETS_KEYSTORE_PASSWORD_KEY,
                        () -> Config.getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY,
                        digest(ClusterFactory.getClusterSalt())).toCharArray()),
                        List.of(this::flushCache)
        );
        this.cache = cache;
    }

    public SecretCachedKeyStoreImpl() {
       this(CacheLocator.getAppsCache());
    }

    /**
     * Verifies if the key exists in the store
     * @param variableKey
     * @return
     */
    @Override
    public boolean containsKey(final String variableKey) {
        try {
            return getKeysFromCache().contains(variableKey);
        } catch (Exception e) {
            Logger.debug(SecretCachedKeyStoreImpl.class,e.getMessage());
            throw new DotRuntimeException(e);
        }
    }

    /**
     * while in cache it must be encrypted!
     * once returned from this method it should get decrypted
     * @param variableKey
     * @return
     */
    @Override
    public Optional<char[]> getValue(final String variableKey) {
        final char[] fromCache = getFromCache(variableKey,
                () -> secretsKeyStore.getValue(variableKey));
        return Arrays.equals(fromCache, CACHE_404) ? Optional.empty()
                : Optional.ofNullable(secretsKeyStore.decrypt(fromCache));
    }

    /**
     * Lists all keys.
     * Keys will be loaded from cache if available otherwise they will be loaded from disk
     * @return
     */
    @Override
    public Set<String> listKeys() {
        return Sneaky.sneak(this::getKeysFromCache);
    }

    @Override
    public boolean deleteAll() throws Exception {
        secretsKeyStore.destroy();
        flushCache();
        return true;
    }

    /**
     * {@inheritDoc}
     * @param variableKey
     * @param variableValue
     * @return
     */
    @Override
    public boolean saveValue(final String variableKey, final char[] variableValue) {
        try {
            putInCache(variableKey, secretsKeyStore.saveValue(variableKey, variableValue));
        } catch (DotCacheException e) {
            Logger.warn(SecretCachedKeyStoreImpl.class, "Unable to save secret in cache " ,e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @param secretKey
     */
    @Override
    public void deleteValue(final String secretKey) {
        secretsKeyStore.deleteValue(secretKey);
        try{
            flushCache(secretKey);
        } catch (DotCacheException e) {
            Logger.warn(SecretCachedKeyStoreImpl.class, "Error flushing cache while removing value from secrets store ", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backupAndRemoveKeyStore() throws IOException {
        secretsKeyStore.backupAndRemoveKeyStore();
    }

    /**
     * Short hand accessor to get values from cache impl
     * @param key
     * @param defaultValue
     * @return
     */
    private char[] getFromCache(final String key, final Supplier<char[]> defaultValue) {
        return cache.getFromCache(key, defaultValue);
    }

    /**
     * Short hand accessor to put values in cache impl
     * @param key
     * @param val
     * @throws DotCacheException
     */
    private void putInCache(final String key, final char[] val)
            throws DotCacheException {
        cache.putSecret(key, val);
        putInCache(key);
    }

    /**
     * Short hand accessor to get values from cache impl
     * @param key
     * @throws DotCacheException
     */
    private void putInCache(final String key) throws DotCacheException{
        final Set <String> keys = getKeysFromCache();
        keys.add(key);
        cache.putKeys(keys);
    }

    /**
     * short hand accessor to get values from cache impl and provided a list of keys
     * @return
     * @throws DotCacheException
     */
    private Set<String> getKeysFromCache()
            throws DotCacheException {
        return cache.getKeysFromCache(() -> {
            final KeyStore keyStore = secretsKeyStore.getSecretsStore();
            try {
                final Set<String> keySet = ConcurrentHashMap.newKeySet();
                keySet.addAll(Collections.list(keyStore.aliases()));
                return keySet;
            } catch (KeyStoreException e) {
                Logger.warn(SecretsKeyStoreHelper.class, "Error building keystore keys cache. ", e);
                throw new DotRuntimeException(e);
            }
        });
    }

    /**
     * short hand accessor to clear cache.
     */
    void flushCache() {
        cache.flushSecret();
    }

    /**
     * short hand accessor to clear cache.
     * @param key
     * @throws DotCacheException
     */
    private void flushCache(final String key) throws DotCacheException {
        cache.flushSecret(key);
    }

}
