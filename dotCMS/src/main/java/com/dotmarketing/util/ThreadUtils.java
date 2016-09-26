package com.dotmarketing.util;


/**
 * Copyright 2008, David Robert Nadeau, NadeauSoftware.com
 *
 * This file is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2 of
 * the License, or (at your option) any later version.
 *
 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this file.  If not, see
 * <a href="http://www.gnu.org/licenses">GNU Licenses</a>.
 */

import java.lang.management.*;

/**
 * The ThreadUtilities class provides a collection of static methods
 * for listing and finding Thread, ThreadGroup, and ThreadInfo objects.
 * <p>
 * Please see the accompanying article:
 * <a href="http://nadeausoftware.com/articles/2008/04/java_tip_how_list_and_find_threads_and_thread_groups">Java tip:  How to list and find threads and thread groups</a>
 *
 * @author	<a href="http://NadeauSoftware.com/">David R. Nadeau</a>
 */
public final class ThreadUtils
{
// Dummy constructor
	/**
	 * The ThreadUtilities class cannot be instanced.
	 */
	private ThreadUtils( )
	{
	}





// Thread groups
	/**
	 * The root thread group saved on the first search for it.
	 * The root group doesn't change for the life of the JVM,
	 * so once found there is no need to find it again.
	 */
	private static ThreadGroup rootThreadGroup = null;

	/**
	 * Get the root thread group in the thread group tree.
	 * Since there is always a root thread group, this
	 * method never returns null.
	 *
	 * @return		the root thread group
	 */
	public static ThreadGroup getRootThreadGroup( )
	{
		if ( rootThreadGroup != null )
			return rootThreadGroup;

		ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
		ThreadGroup ptg;
		while ( (ptg = tg.getParent( )) != null )
			tg = ptg;
		rootThreadGroup = tg;
		return tg;
	}

	/**
	 * Get a list of all thread groups.  Since there is
	 * always at least one thread group (the root), this
	 * method never returns a null or empty array.
	 *
	 * @return		an array of thread groups
	 */
	public static ThreadGroup[] getAllThreadGroups( )
	{
		final ThreadGroup root = getRootThreadGroup( );
		int nAlloc = root.activeGroupCount( );
		int n = 0;
		ThreadGroup[] groups = null;
		do
		{
			nAlloc *= 2;
			groups = new ThreadGroup[ nAlloc ];
			n = root.enumerate( groups, true );
		} while ( n == nAlloc );
		ThreadGroup[] allGroups = new ThreadGroup[n+1];
		allGroups[0] = root;
		System.arraycopy( groups, 0, allGroups, 1, n );
		return allGroups;
	}

	/**
	 * Get the thread group with the given name.  A null is
	 * returned if no such group is found.  If more than one
	 * group has the same name, the first one found is returned.
	 *
	 * @param	name	the thread group name to search for
	 * @return		the thread group, or null if not found
	 * @throws	NullPointerException
	 * 			if the name is null
	 */
	public static ThreadGroup getThreadGroup( final String name )
	{
		if ( name == null )
			throw new NullPointerException( "Null name" );
		final ThreadGroup[] groups = getAllThreadGroups( );
		for ( ThreadGroup group : groups )
			if ( group.getName( ).equals( name ) )
				return group;
		return null;
	}





// Threads
	/**
	 * Get a list of all threads.  Since there is always at
	 * least one thread, this method never returns null or
	 * an empty array.
	 *
	 * @return		an array of threads
	 */
	public static Thread[] getAllThreads( )
	{
		final ThreadGroup root = getRootThreadGroup( );
		final ThreadMXBean thbean =
			ManagementFactory.getThreadMXBean( );
		int nAlloc = thbean.getThreadCount( );
		int n = 0;
		Thread[] threads = null;
		do
		{
			nAlloc *= 2;
			threads = new Thread[ nAlloc ];
			n = root.enumerate( threads, true );
		} while ( n == nAlloc );
		return threads.clone();
	}

	/**
	 * Get a list of all threads in a thread group.  An empty
	 * array is returned if there are no threads in the group.
	 *
	 * @param	group	the thread group to list
	 * @return		an array of threads
	 * @throws	NullPointerException
	 * 			if the group is null
	 */
	public static Thread[] getGroupThreads( final ThreadGroup group )
	{
		if ( group == null )
			throw new NullPointerException( "Null group" );
		int nAlloc = group.activeCount( );
		int n = 0;
		Thread[] threads = null;
		do
		{
			nAlloc *= 2;
			threads = new Thread[ nAlloc ];
			n = group.enumerate( threads, false );
		} while ( n == nAlloc );
		return threads.clone();
	}

	/**
	 * Get a list of all threads in a named thread group.
	 * A null is returned if the group cannot be found.
	 * An empty array is returned if there are no threads in
	 * the group.
	 *
	 * @param	name	the name of the thread group
	 * @return		an array of threads, or null if the
	 *			group is not found
	 * @throws	NullPointerException
	 * 			if the name is null
	 */
	public static Thread[] getGroupThreads( final String name )
	{
		final ThreadGroup group = getThreadGroup( name );
		if ( group == null )
			return null;
		return getGroupThreads( group );
	}

	/**
	 * Get a list of all threads, sorted from highest to
	 * lowest priority.  Since there is always at least one
	 * thread, this method never returns null or an empty
	 * array.  Since threads may change their priority during
	 * this method's sort, the returned thread list may not be
	 * correct by the time it is returned.
	 *
	 * @return		an array of threads
	 */
	public static Thread[] getAllThreadsPrioritized( )
	{
		final Thread[] allThreads = getAllThreads( );
		java.util.Arrays.sort( allThreads,
		new java.util.Comparator<Thread>( )
		{
			public int compare( final Thread t1, final Thread t2 )
			{ return t2.getPriority( ) - t1.getPriority( ); }
		} );
		return allThreads;
	}

	/**
	 * Get a list of all daemon threads.  An empty array is
	 * returned if there are no daemon threads.
	 *
	 * @return		an array of daemon threads
	 */
	public static Thread[] getAllDaemonThreads( )
	{
		final Thread[] allThreads = getAllThreads( );
		final Thread[] daemons = new Thread[allThreads.length];
		int nDaemon = 0;
		for ( Thread thread : allThreads )
			if ( thread.isDaemon( ) )
				daemons[nDaemon++] = thread;
		return daemons.clone();
	}

	/**
	 * Get a list of all threads with a given thread state.
	 * Thread states are defined in the Thread.State enum for
	 * the Thread class.  Principal thread states include
	 * RUNNABLE, WAITING, TIMED_WAITING, and BLOCKED.  An
	 * empty array is returned if there are no threads in
	 * the chosen state.
	 *
	 * @param	state	the state to look for
	 * @return		an array of threads in that state
	 */
	public static Thread[] getAllThreads( final Thread.State state )
	{
		final Thread[] allThreads = getAllThreads( );
		final Thread[] found = new Thread[allThreads.length];
		int nFound = 0;
		for ( Thread thread : allThreads )
			if ( thread.getState( ) == state )
				found[nFound++] = thread;
		return found.clone();
	}

	/**
	 * Get the thread with the given name.  A null is returned
	 * if no such thread is found.  If more than one thread has
	 * the same name, the first one found is returned.
	 *
	 * @param	name	the thread name to search for
	 * @return		the thread, or null if not found
	 * @throws	NullPointerException
	 * 			if the name is null
	 */
	public static Thread getThread( final String name )
	{
		if ( name == null )
			throw new NullPointerException( "Null name" );
		final Thread[] threads = getAllThreads( );
		for ( Thread thread : threads )
			if ( thread!=null && thread.getName( ).equals( name ) ){
				return thread;
			}
		return null;
	}

	/**
	 * Get the thread with the given ID.  A null is returned
	 * if no such thread is found.
	 *
	 * @param	id	the thread ID to search for
	 * @return		the thread, or null if not found
	 */
	public static Thread getThread( final long id )
	{
		final Thread[] threads = getAllThreads( );
		for ( Thread thread : threads )
			if ( thread.getId( ) == id )
				return thread;
		return null;
	}

	/**
	 * Get the thread for the given thread info.  A null
	 * is returned if the thread cannot be found.
	 *
	 * @param	info	the thread info to search for
	 * @return		the thread, or null if not found
	 * @throws	NullPointerException
	 * 			if info is null
	 */
	public static Thread getThread( final ThreadInfo info )
	{
		if ( info == null )
			throw new NullPointerException( "Null info" );
		return getThread( info.getThreadId( ) );
	}





// ThreadInfo
	/**
	 * Get a list of all thread info objects.  Since there is
	 * always at least one thread running, there is always at
	 * least one thread info object.  This method never returns
	 * a null or empty array.
	 *
	 * @return		an array of thread infos
	 */
	/*
	public static ThreadInfo[] getAllThreadInfos( )
	{
		final ThreadMXBean thbean =
			ManagementFactory.getThreadMXBean( );
		final long[] ids = thbean.getAllThreadIds( );

		// Get thread info with lock info, when available.
		ThreadInfo[] infos;
		if ( !thbean.isObjectMonitorUsageSupported( ) ||
			!thbean.isSynchronizerUsageSupported( ) )
			infos = thbean.getThreadInfo( ids );
		else
			infos = thbean.getThreadInfo( ids, true, true );

		// Clean nulls from array if threads have died.
		final ThreadInfo[] notNulls = new ThreadInfo[infos.length];
		int nNotNulls = 0;
		for ( ThreadInfo info : infos )
			if ( info != null )
				notNulls[nNotNulls++] = info;
		if ( nNotNulls == infos.length )
			return infos;	// Original had no nulls
		return java.util.Arrays.copyOf( notNulls, nNotNulls );
	}
	*/

	/**
	 * Get the thread info for the thread with the given name.
	 * A null is returned if no such thread info is found.
	 * If more than one thread has the same name, the thread
	 * info for the first one found is returned.
	 *
	 * @param	name	the thread name to search for
	 * @return		the thread info, or null if not found
	 * @throws	NullPointerException
	 * 			if the name is null
	 */
	/*
	public static ThreadInfo getThreadInfo( final String name )
	{
		if ( name == null )
			throw new NullPointerException( "Null name" );

		final Thread[] threads = getAllThreads( );
		for ( Thread thread : threads )
			if ( thread.getName( ).equals( name ) )
				return getThreadInfo( thread.getId( ) );
		return null;
	}
	*/

	/**
	 * Get the thread info for the thread with the given ID.
	 * A null is returned if no such thread info is found.
	 *
	 * @param	id	the thread ID to search for
	 * @return		the thread info, or null if not found
	 * @throws	IllegalArgumentException
	 *			if id <= 0
	 */
	/*
	public static ThreadInfo getThreadInfo( final long id )
	{
		final ThreadMXBean thbean =
			ManagementFactory.getThreadMXBean( );

		// Get thread info with lock info, when available.
		if ( !thbean.isObjectMonitorUsageSupported( ) ||
			!thbean.isSynchronizerUsageSupported( ) )
			return thbean.getThreadInfo( id );

		final ThreadInfo[] infos = thbean.getThreadInfo(
			new long[] { id }, true, true );
		if ( infos.length == 0 )
			return null;
		return infos[0];
	}
	*/

	/**
	 * Get the thread info for the given thread.  A null is
	 * returned if the thread info cannot be found.
	 *
	 * @param	thread	the thread to search for
	 * @return		the thread info, or null if not found
	 * @throws	NullPointerException
	 * 			if thread is null
	 */
	/*
	public static ThreadInfo getThreadInfo( final Thread thread )
	{
		if ( thread == null )
			throw new NullPointerException( "Null thread" );
		return getThreadInfo( thread.getId( ) );
	}
	*/





// MonitorInfo and LockInfo
	/**
	 * Get the thread holding a lock on the given object.
	 * A null is returned if there is no such thread.
	 *
	 * @param	object		the object to look for a lock on
	 * @return			the thread holding a lock, or
	 * 				null if there is none
	 * @throws	NullPointerException
	 * 				if the object is null
	 */
	/*
	public static Thread getLockingThread( final Object object )
	{
		if ( object == null )
			throw new NullPointerException( "Null object" );
		final long identity = System.identityHashCode( object );

		final Thread[] allThreads = getAllThreads( );
		ThreadInfo info = null;
		MonitorInfo[] monitors = null;
		for ( Thread thread : allThreads )
		{
			info = getThreadInfo( thread.getId( ) );
			if ( info == null )
				continue;
			monitors = info.getLockedMonitors( );
			for ( MonitorInfo monitor : monitors )
				if ( identity == monitor.getIdentityHashCode( ) )
					return thread;
		}
		return null;
	}
	*/

	/**
	 * Get the thread who's lock is blocking the given thread.
	 * A null is returned if there is no such thread.
	 *
	 * @param	blockedThread	the blocked thread
	 * @return			the blocking thread, or null if
	 * 				there is none
	 * @throws	NullPointerException
	 * 				if the blocked thread is null
	 */
	/*
	public static Thread getBlockingThread( final Thread blockedThread )
	{
		final ThreadInfo info = getThreadInfo( blockedThread );
		if ( info == null )
			return null;
		final long id = info.getLockOwnerId( );
		if ( id == -1 )
			return null;
		return getThread( id );
	}
	*/
}
