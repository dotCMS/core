package com.dotmarketing.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.cache.Fqn;

/**
 * 
 * @author Andres Olarte
 * @author Jose Orsini
 * 
 */
public class FQNLock {

	public static FQNLock staticLock = new FQNLock();

	public static FQNLock getLock() {
		return staticLock;
	}

	public Map<Fqn, ReentrantReadWriteLock> locks = new ConcurrentHashMap<Fqn, ReentrantReadWriteLock>();
	public Map<Fqn, Integer> counts = new ConcurrentHashMap<Fqn, Integer>();

	public void acquireLock(Fqn fqn, boolean exclusive) {
		if (!isTopLevel(fqn)) {
			Fqn parentFqn = getParent(fqn);
			ReentrantReadWriteLock parentLock = getLock(parentFqn);
			parentLock.readLock().lock();
		}
		ReentrantReadWriteLock lock = getLock(fqn);

		if (exclusive) {
			lock.writeLock().lock();
		} else {
			lock.readLock().lock();
		}

	}

	public void releaseLock(Fqn fqn) {
		if (!isTopLevel(fqn)) {
			Fqn parentFqn = getParent(fqn);
			ReentrantReadWriteLock parentLock = getLock(getParent(fqn));
			parentLock.readLock().unlock();
			remove(parentFqn);

		}
		ReentrantReadWriteLock lock = getLock(fqn);
		if (lock.isWriteLockedByCurrentThread()) {
			lock.writeLock().unlock();
		} else {
			lock.readLock().unlock();
		}

		remove(fqn);
	}

	protected void remove(Fqn fqn) {
		synchronized (fqn.toString().intern()) {
			Integer count = counts.get(fqn);
			if (count!=null) {
				if (count <= 1) {
					//System.out.println("Removing  FQN: " + fqn + " THREAD: "
					//		+ Thread.currentThread());
					locks.remove(fqn);
					counts.remove(fqn);
				} else {
					count--;
					counts.put(fqn, count);
				}
			}
		}
	}

	protected Fqn getParent(Fqn key) {
		if (key.size() < 2) {
			return key;
		}
		return key.getParent();

	}

	protected boolean isTopLevel(Fqn key) {
		if (key.size() < 2) {
			return true;
		}
		return false;
	}

	private ReentrantReadWriteLock getLock(Fqn key) {
		ReentrantReadWriteLock lock = null;
		synchronized (key.toString().intern()) {
			lock = locks.get(key);
			if (lock == null) {
				lock = new ReentrantReadWriteLock();
				locks.put(key, lock);
				counts.put(key, 1);

			} else {
				Integer count = counts.get(key);
				if (count==null) {
					System.out.println("NULL");
				}
				counts.put(key, count + 1);
			}
			//System.out.println("Lock: " + lock);
		}
		return lock;
	}
}
