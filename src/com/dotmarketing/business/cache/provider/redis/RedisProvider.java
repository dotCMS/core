package com.dotmarketing.business.cache.provider.redis;

import com.dotcms.repackage.org.apache.commons.collections.map.LRUMap;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.io.*;
import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 10/9/15
 */
public class RedisProvider extends CacheProvider {

    private static final long serialVersionUID = -855583393078878276L;

    //Global Map of contents that could not be added to this cache
    private static Map<String, String> cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));

    //Global list of groups
    private Set<String> groups = new HashSet<>();

    private JedisPool writePool;//Master
    private JedisPool readPool;//Slave
    private final char delimit = ';';

    @Override
    public String getName () {
        return "Redis Provider";
    }

    @Override
    public String getKey () {
        return "redisProvider";
    }

    @Override
    public void init () {

        Logger.info(this.getClass(), "*** Initializing [" + getName() + "].");

        //Reading the configuration settings
        String writeHost = Config.getStringProperty("redis.server.write.address",
                Config.getStringProperty("redis.server.address", Protocol.DEFAULT_HOST));
        int writePort = Config.getIntProperty("redis.server.write.port",
                Config.getIntProperty("redis.server.port", Protocol.DEFAULT_PORT));

        int timeout = Config.getIntProperty("redis.server.timeout", Protocol.DEFAULT_TIMEOUT);
        int maxClients = Config.getIntProperty("redis.pool.max.clients", 100);
        int maxIdle = Config.getIntProperty("redis.pool.max.idle", 20);
        int minIdle = Config.getIntProperty("redis.pool.min.idle", 5);
        boolean testReturn = Config.getBooleanProperty("redis.pool.test.on.return", false);
        boolean blockExhausted = Config.getBooleanProperty("redis.pool.block.when.exhausted", false);
        String redisPass = Config.getStringProperty("redis.password");

        //Set the read configuration
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxClients);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setTestOnReturn(testReturn);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setBlockWhenExhausted(blockExhausted);

        //Creating the redis write pool (For the master)
        writePool = UtilMethods.isSet(redisPass)
                ? new JedisPool(jedisPoolConfig, writeHost, writePort, timeout, redisPass)
                : new JedisPool(jedisPoolConfig, writeHost, writePort, timeout);

        Logger.info(this.getClass(), "***\t [" + getName() + "] -- Master [" + writeHost + ":" + writePort + "].");

        //Slave configuration
        String readHost = Config.getStringProperty("redis.server.read.address", Config.getStringProperty("redis.server.address", Protocol.DEFAULT_HOST));
        int readPort = Config.getIntProperty("redis.server.read.port", Config.getIntProperty("redis.server.port", Protocol.DEFAULT_PORT));

        //Verify if a slave data was set
        if ( readHost.equals(writeHost) && readPort == writePort ) {
            //No slave data was set, we will read from the master server
            readPool = writePool;
        } else {
            readPool = UtilMethods.isSet(redisPass)
                    ? new JedisPool(jedisPoolConfig, readHost, readPort, timeout, redisPass)
                    : new JedisPool(jedisPoolConfig, readHost, readPort, timeout);

            Logger.info(this.getClass(), "***\t [" + getName() + "] -- Slave [" + readHost + ":" + readPort + "].");
        }

        Logger.info(this.getClass(), "*** Initialized Cache Provider [" + getName() + "].");
    }

    @Override
    public void put ( String group, String key, Object content ) {

        if ( key == null || group == null ) {
            return;
        }

        group = group.toLowerCase();
        key = key.toLowerCase();

        //Building the key
        StringWriter compoundKey = new StringWriter();
        compoundKey.append(group);
        compoundKey.append(delimit);
        compoundKey.append(key);

        if ( cannotCacheCache.get(compoundKey.toString()) != null ) {
            Logger.info(this, "Returning because object is in cannot cache cache - Redis: group [" + group + "] - key [" + key + "].");
            return;
        }

        //Check if we must exclude this record from this cache
        if ( exclude(group, key) ) {
            return;
        }

        ObjectOutputStream output = null;
        OutputStream outputStream;

        try ( Jedis jedis = writePool.getResource() ) {

            //Prepare the object to be store
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            outputStream = new BufferedOutputStream(arrayOutputStream, 8192);

            output = new ObjectOutputStream(outputStream);
            output.writeObject(content);
            output.flush();

            byte[] data = arrayOutputStream.toByteArray();
            if ( data == null || data.length == 0 ) {
                return;
            }

            //Add the object to redis master
            jedis.set(compoundKey.toString().getBytes(), data);

            //Add the given group to the global list of groups
            groups.add(group);

        } catch ( NotSerializableException ex ) {
            remove(group, key);
            cannotCacheCache.put(compoundKey.toString(), compoundKey.toString());
            Logger.error(this, "Error Adding to Redis [NotSerializableException]: group [" + group + "] - key [" + key + "].");
            Logger.debug(this, "Error Adding to Redis: group [" + group + "] - key [" + key + "].", ex);
        } catch ( Exception e ) {
            Logger.error(this, "Error Adding to Redis: group [" + group + "] - key [" + key + "].", e);
        } finally {
            try {
                if ( output != null ) {
                    output.close();
                }
            } catch ( Exception e ) {
                Logger.error(this, "Error trying to close Stream.", e);
            }
        }
    }

    @Override
    public Object get ( String group, String key ) {

        if ( key == null || group == null ) {
            return null;
        }

        //Building the key
        StringWriter compoundKey = new StringWriter();
        compoundKey.append(group.toLowerCase());
        compoundKey.append(delimit);
        compoundKey.append(key.toLowerCase());

        //Reading the record from the Redis slave
        byte[] data = null;
        try ( Jedis jedis = readPool.getResource() ) {
            data = jedis.get(compoundKey.toString().getBytes());
        } catch ( Exception e ) {
            Logger.error(this, "Error retrieving from Redis: group [" + group + "] - key [" + key + "].", e);
        }

        if ( data == null ) {
            return null;
        }

        //Reconstructing the object to send it back
        ObjectInputStream input = null;
        InputStream bin = null;
        InputStream is = null;

        try {
            is = new ByteArrayInputStream(data);
            bin = new BufferedInputStream(is, 8192);
            input = new ObjectInputStream(bin);
            return input.readObject();
        } catch ( Exception e ) {
            Logger.error(this, "Error retrieving from Redis: group [" + group + "] - key [" + key + "].", e);
        } finally {
            try {
                if ( input != null ) {
                    input.close();
                }
            } catch ( Exception e ) {
                Logger.error(this, "Error trying to close Stream.", e);
            }
            try {
                if ( bin != null ) {
                    bin.close();
                }
            } catch ( Exception e ) {
                Logger.error(this, "Error trying to close Stream.", e);
            }
            try {
                if ( is != null ) {
                    is.close();
                }
            } catch ( Exception e ) {
                Logger.error(this, "Error trying to close Stream.", e);
            }
        }

        return null;
    }

    @Override
    public void remove ( final String group, final String key ) {

        if ( key == null || group == null ) {
            return;
        }

        Runnable cacheRemoveRunnable = new Runnable() {
            public void run () {

                //Building the key
                StringWriter compoundKey = new StringWriter();
                compoundKey.append(group.toLowerCase());
                compoundKey.append(delimit);
                compoundKey.append(key.toLowerCase());

                //Deleting the record from the Redis master
                try ( Jedis jedis = writePool.getResource() ) {
                    jedis.del(compoundKey.toString());
                } catch ( Exception e ) {
                    Logger.error(this, "Error removing from Redis: group [" + group + "] - key [" + key + "].", e);
                }
            }
        };
        try {
            if ( !DbConnectionFactory.getConnection().getAutoCommit() ) {
                HibernateUtil.addCommitListener(cacheRemoveRunnable);
                return;
            }
        } catch ( Exception e ) {
            Logger.error(this, "Error removing from Redis: group [" + group + "] - key [" + key + "].", e);
        }

        cacheRemoveRunnable.run();
    }

    @Override
    public void remove ( String group ) {

        if ( group == null ) {
            return;
        }

        group = group.toLowerCase();

        //Getting all the keys for the given groups
        Set<String> keys = getKeys(group);

        Iterator<String> keysIterator = keys.iterator();

        try ( Jedis jedis = writePool.getResource() ) {

            boolean go = keysIterator.hasNext();

            //The list of keys we want to remove
            List<String> keysToDelete = new ArrayList<>();

            while ( go ) {

                while ( keysIterator.hasNext() ) {

                    keysToDelete.add(keysIterator.next());

                    //Deleting groups of 100
                    if ( keysToDelete.size() > 100 ) {
                        jedis.del(keysToDelete.toArray(new String[keysToDelete.size()]));
                        keysToDelete = new ArrayList<>();
                        break;
                    }

                }

                //If something missing to be delete it
                jedis.del(keysToDelete.toArray(new String[keysToDelete.size()]));
                break;
            }

        } catch ( Exception e ) {
            Logger.error(this, "Error removing from Redis: group [" + group + "].", e);
        }

        //Remove the given group from the global list of groups
        groups.remove(group);
    }

    @Override
    public void removeAll () {

        try ( Jedis jedis = writePool.getResource() ) {
            jedis.flushAll();
        } catch ( Exception e ) {
            Logger.error(this, "Error removing all from Redis.", e);
        }

        //Clear the global list of groups
        groups.clear();

        //Reset the list of objects that cannot be in the Cache
        resetCannotCacheCache();
    }

    @Override
    public Set<String> getKeys ( String group ) {

        if ( group == null ) {
            return null;
        }

        group = group.toLowerCase();

        //Reading the keys from the redis slave
        try ( Jedis jedis = readPool.getResource() ) {
            return jedis.keys(group + "*");
        } catch ( Exception e ) {
            Logger.error(this, "Error retrieving Redis keys", e);
        }

        return new HashSet<String>();
    }

    @Override
    public Set<String> getGroups () {
        return groups;
    }

    @Override
    public List<Map<String, Object>> getStats () {

        List<Map<String, Object>> list = new ArrayList<>();

        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());

        for ( String group : currentGroups ) {

            Map<String, Object> stats = new HashMap<>();
            stats.put("name", getName());
            stats.put("key", getKey());
            stats.put("region", group);
            stats.put("toDisk", false);

            stats.put("isDefault", false);
            stats.put("memory", "0");
            stats.put("disk", -1);
            stats.put("configuredSize", -1);

            list.add(stats);
        }

        return list;
    }

    @Override
    public void shutdown () {

        Logger.info(this.getClass(), "*** Destroying [" + getName() + "] pool.");
        writePool.destroy();
        readPool.destroy();
    }

    /**
     * Resets the Map of cache records that could be added to this cache
     */
    private void resetCannotCacheCache () {
        cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));
    }

    /**
     * Method that verifies if must exclude content that can not or must not be added to this Redis cache
     * based on the given cache group and key
     *
     * @param group
     * @param key
     * @return
     */
    private boolean exclude ( String group, String key ) {

        Boolean exclude = false;

        if ( key.startsWith("velocitycacheusers") ) {
            exclude = false;
        } else if ( key.startsWith("velocitycache") ) {
            if ( !(key.contains("live") || key.contains("working")) ) {
                exclude = true;
            }
        } else if ( group.equals("velocitycache") && key.endsWith(".vm") ) {//Velocity macros cannot be cached
            exclude = true;
        } else if ( key.startsWith("velocitymenucache") ) {
            exclude = true;
        }

        if ( exclude ) {

            //Building the key
            StringWriter compoundKey = new StringWriter();
            compoundKey.append(group);
            compoundKey.append(delimit);
            compoundKey.append(key);

            cannotCacheCache.put(compoundKey.toString(), compoundKey.toString());
        }

        return exclude;
    }

}