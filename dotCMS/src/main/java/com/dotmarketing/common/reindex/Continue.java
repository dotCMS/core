package com.dotmarketing.common.reindex;

/*
 * TODO: Need to be able to compose many Continues.
 *       This will enable swapping two Continues atomically.
 */

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Provides a mechanism to pause multiple threads.
 * If wish your thread to participate, then it must regularly check in with an instance of this object.
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Continue {

    public void checkIn() throws InterruptedException;

    public void checkInUntil(Date deadline) throws InterruptedException;

    public void checkIn(long nanosTimeout) throws InterruptedException;

    public void checkIn(long time, TimeUnit unit) throws InterruptedException;

    public void checkInUninterruptibly();

    public boolean isPaused();

    public void pause();

    public void resume();

}