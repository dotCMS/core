package com.ettrema.cache;

import com.ettrema.common.Service;
import java.util.Map;

/**
 *
 * @author brad
 */
public class MemoryCache<K, T> implements Cache<K, T>, Service {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemoryCache.class);
    private final String name;
    private final int highWater;
    private final int lowWater;
    private final Thread clearer;
    private final Cache<K, T> auxillary;
    private final OrderedMap<K, T> map;
    private long memHits;
    private long auxHits;
    private long misses;
    private long numFlushed;
    private boolean started;
    private int lowMemoryLimit = 10;

    public MemoryCache(String name, int highWater, int lowWater) {
        this.name = name;
        this.map = new OrderedMap<K, T>();
        if (highWater < lowWater) {
            throw new IllegalArgumentException("highWater must be greater then lowWater");
        }
        this.highWater = highWater;
        this.lowWater = lowWater;
        clearer = new Thread(new Clearer());
        clearer.setDaemon(true);
        auxillary = null;
    }

    public MemoryCache(int highWater, int lowWater, Cache<K, T> auxillary) {
        this.name = auxillary.getName() + " Memory";
        this.map = new OrderedMap<K, T>();
        if (highWater < lowWater) {
            throw new IllegalArgumentException("highWater must be greater then lowWater");
        }
        this.highWater = highWater;
        this.lowWater = lowWater;
        clearer = new Thread(new Clearer());
        clearer.setDaemon(true);
        this.auxillary = auxillary;
    }

    @Override
    public Long getSize() {
        return (long) map.size();
    }

    @Override
    public void start() {
        log.debug("starting MemoryCache: " + name);
        started = true;
        clearer.start();
    }

    @Override
    public void stop() {
        started = false;
        clearer.interrupt();
    }

    @Override
    public void put(K key, T val) {
        synchronized (map) {
            map.put(key, val);
            if (log.isTraceEnabled()) {
                log.trace("put: cache: " + this.name + " key: " + key);
            }
            if (auxillary != null) {
                auxillary.remove(key);
            }
        }
    }

    @Override
    public T get(K key) {
        long tm = 0;
        if (log.isTraceEnabled()) {
            log.trace("get: cache: " + name + " - key: " + key + " size: " + this.getSize() + " high: " + this.highWater);
            tm = System.nanoTime();
        }

        T val = map.get(key);

        if(log.isTraceEnabled()) {
            tm = System.nanoTime() - tm;
            log.trace("query time: " + tm + "ns  found=" + (val != null));
        }

        if (val == null) {
            if (auxillary != null) {
                val = auxillary.get(key);
                if (val != null) {
                    put(key, val);
                    auxHits++;
                    misses++;
                } else {
                    log.trace("local cache miss and auxilliary cache miss");
                    misses++;
                }
            } else {
                log.trace("local cache miss");
                misses++;
            }
        } else {
            memHits++;
        }
        return val;
    }

    @Override
    public void flush() {
        log.trace("flush");
        map.clear();
        if( auxillary != null ) {
            auxillary.flush();
        }
    }

    @Override
    public void remove(K key) {
        if(log.isTraceEnabled()) {
            log.trace("remove: " + key);
        }
        if (auxillary != null) {
            auxillary.remove(key);
        }

        map.remove(key);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getHits() {
        return memHits;
    }

    @Override
    public long getMisses() {
        return misses;
    }

    public int getLowMemoryLimit() {
        return lowMemoryLimit;
    }

    public void setLowMemoryLimit(int lowMemoryLimit) {
        this.lowMemoryLimit = lowMemoryLimit;
    }

    public class Clearer implements Runnable {

        @Override
        public void run() {
            try {
                while (started) {
                    if (compactRequired()) {
                        log.debug("removing old keys: freeMemory" + Runtime.getRuntime().freeMemory() + " mamMemory: " + Runtime.getRuntime().maxMemory());
                        while (getSize() > lowWater) {
                            synchronized (map) {
                                removeOneKey();
                            }
                            Thread.sleep(1);
                        }
                        System.gc();
                        log.debug("finished removing old keys: freeMemory" + Runtime.getRuntime().freeMemory() + " mamMemory: " + Runtime.getRuntime().maxMemory());
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException interruptedException) {
                log.debug("clearer interrupted");
            } finally {
                log.warn("clearer finished: " + getName() + "------------------------------------------");
            }
        }

        private void removeOneKey() {
            Map.Entry<K, T> entry = map.removeFirst();
            if (auxillary != null) {
                auxillary.put(entry.getKey(), entry.getValue());
                numFlushed++;
            }
        }

        private boolean compactRequired() {
            if(log.isTraceEnabled()) {
                    long free = Runtime.getRuntime().freeMemory();
                    long total = Runtime.getRuntime().totalMemory();
                    long max = Runtime.getRuntime().maxMemory();
                    long actualFree = max - total + free; // free is the "free" amount plus unallocated space
                    long actualPerc = actualFree * 100 / max;
                    log.trace("memory free: " + actualPerc + "% of max: " + max / 1000000 + "Mb");
            }
            if (map.size() > highWater) {
                log.info("highwater exceeded: " + getName() + " highwater: " + highWater + " size: " + getSize());
                return true;
            } else {
                if (map.size() > lowWater) {
                    long free = Runtime.getRuntime().freeMemory();
                    long total = Runtime.getRuntime().totalMemory();
                    long max = Runtime.getRuntime().maxMemory();
                    long actualFree = max - total + free; // free is the "free" amount plus unallocated space
                    long actualPerc = actualFree * 100 / max;
                    if (log.isDebugEnabled()) {
                        log.debug("memory free: " + actualPerc + "% of max: " + max / 1000000 + "Mb");
                    }
                    if (actualPerc < lowMemoryLimit) {
                        log.warn("Free memory below " + lowMemoryLimit + "% so compacting cache: " + getName() + " Highwater: " + highWater + " Current Size: " + getSize());
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
