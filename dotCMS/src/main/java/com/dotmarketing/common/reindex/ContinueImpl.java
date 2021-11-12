package com.dotmarketing.common.reindex;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ContinueImpl implements Continue {
    private boolean isPaused;
    private ReentrantLock pauseLock = new ReentrantLock();
    private Condition unpaused = pauseLock.newCondition();

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#checkIn()
     */
    @Override
    public void checkIn() throws InterruptedException {
        if (isPaused) {
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.await();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#checkInUntil(java.util.Date)
     */
    @Override
    public void checkInUntil(Date deadline) throws InterruptedException {
        if (isPaused) {
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.awaitUntil(deadline);
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#checkIn(long)
     */
    @Override
    public void checkIn(long nanosTimeout) throws InterruptedException {
        if (isPaused) {
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.awaitNanos(nanosTimeout);
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#checkIn(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public void checkIn(long time, TimeUnit unit) throws InterruptedException {
        if (isPaused) {
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.await(time, unit);
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#checkInUninterruptibly()
     */
    @Override
    public void checkInUninterruptibly() {
        if (isPaused) {
            pauseLock.lock();
            try {
                while (isPaused)
                    unpaused.awaitUninterruptibly();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#isPaused()
     */
    @Override
    public boolean isPaused() {
        return isPaused;
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#pause()
     */
    @Override
    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    /* (non-Javadoc)
     * @see au.com.phiware.util.concurrent.Continue#resume()
     */
    @Override
    public void resume() {
        pauseLock.lock();
        try {
            if (isPaused) {
                isPaused = false;
                unpaused.signalAll();
            }
        } finally {
            pauseLock.unlock();
        }
    }
}