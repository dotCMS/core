package org.apache.velocity.util;

import com.dotmarketing.util.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.velocity.runtime.RuntimeServices;

/**
 * 
 * @see <a
 *      href=http://www.javacodegeeks.com/2013/08/simple-and-lightweight-pool-
 *      implementation.html>simple pool</>
 */
@SuppressWarnings("hiding")
public class ConcurrentPool<Parser> {
	private ConcurrentLinkedQueue<Parser> pool;

	private ScheduledExecutorService executorService;
	final RuntimeServices rsvc;
	long lastLog = System.currentTimeMillis();
	long totalParsers = 0;
	long totalParserCreationTime = 0;
	int poolMax = 0;

	/**
	 * Creates the pool.
	 *
	 * @param minIdle
	 *            minimum number of objects residing in the pool
	 * @param maxIdle
	 *            maximum number of objects residing in the pool
	 * @param validationInterval
	 *            time in seconds for periodical checking of minIdle / maxIdle
	 *            conditions in a separate thread. When the number of objects is
	 *            less than minIdle, missing instances will be created. When the
	 *            number of objects is greater than maxIdle, too many instances
	 *            will be removed.
	 */
	public ConcurrentPool(final int minIdle, final int maxIdle, final long validationInterval, RuntimeServices rsvc) {
		this.rsvc = rsvc;
		// initialize pool
		initialize(minIdle);

        // check pool conditions in a virtual thread with scheduled execution
        executorService = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        executorService.scheduleWithFixedDelay(() -> {
            int size = pool.size();

            if (size < minIdle) {
                int sizeToBeAdded = minIdle - size;
                for (int i = 0; i < sizeToBeAdded; i++) {
                    pool.add(createObject());
                }
            } else if (size > maxIdle) {
                int sizeToBeRemoved = size - maxIdle;
                for (int i = 0; i < sizeToBeRemoved; i++) {
                    pool.poll();
                }
            } else if (size > minIdle) {
                // slowly tick the pool down to idle, loosing one connection
                // per validationInterval sec
                pool.poll();
            }
            if (size > poolMax) {
                poolMax = size;
            }
            // log once an hour
            if ((lastLog + 1000 * 60 * 60) < System.currentTimeMillis()) {
                lastLog = System.currentTimeMillis();
                Logger.info(ConcurrentPool.class,
                        "Parsers waiting:" + size + ", max at load:" + poolMax + ", total created:"
                                + totalParsers + ", avg creation ms:" + ((totalParserCreationTime / totalParsers)
                                / 1000) + "ms");
            } else {
                Logger.debug(ConcurrentPool.class,
                        "Parsers waiting:" + size + ", max at load:" + poolMax + ", total created:"
                                + totalParsers + ", avg creation ms:" + ((totalParserCreationTime / totalParsers)
                                / 1000) + "ms");
			}
		}, validationInterval, validationInterval, TimeUnit.SECONDS);
	}

	/**
	 * Gets the next free object from the pool. If the pool doesn't contain any
	 * objects, a new object will be created and given to the caller of this
	 * method back.
	 *
	 * @return T borrowed object
	 */
	public Object borrowObject() {
		Object object;
		if ((object = pool.poll()) == null) {
			object = createObject();
		}

		return object;
	}

	/**
	 * Returns object back to the pool.
	 *
	 * @param object
	 *            object to be returned
	 */
	public void returnObject(Parser object) {
		if (object == null) {
			return;
		}

		pool.offer(object);
	}

	/**
	 * Shutdown this pool.
	 */
	public void shutdown() {
		if (executorService != null) {
			executorService.shutdown();
		}
	}

	/**
	 * Creates a new object.
	 *
	 * @return T new object
	 */
	protected Parser createObject() {
		long start = System.nanoTime();
		try {
			return (Parser) rsvc.createNewParser();
		} finally {
			totalParserCreationTime += (System.nanoTime() - start);
			totalParsers++;
		}
	}

	private void initialize(final int minIdle) {
		pool = new ConcurrentLinkedQueue<>();

		for (int i = 0; i < minIdle; i++) {
			pool.add(createObject());
		}
	}
}
