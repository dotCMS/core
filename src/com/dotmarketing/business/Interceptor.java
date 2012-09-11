/**
 * 
 */
package com.dotmarketing.business;

import java.util.List;

/**
 * @author Jason Tesser
 *
 */
public interface Interceptor {
	
	/**
	 * All class names of the hooks
	 * @return
	 */
	public List<String> getPreHooks();
	
	/**
	 * All class names of the hooks
	 * @return
	 */
	public List<String> getPostHooks();
	
	/**
	 * Adds a hook to the end of the chain
     *
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public void addPostHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException;	
	
	/**
	 * Adds a hook to the end of the chain
     *
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
    public void addPostHook ( Object postHook ) throws InstantiationException, IllegalAccessException, ClassNotFoundException;

    /**
     * Adds a hook to the end of the chain
     *
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
	public void addPreHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
	
	/**
     * Adds a hook to the end of the chain
     *
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void addPreHook ( Object preHook ) throws InstantiationException, IllegalAccessException, ClassNotFoundException;

	/**
	 * Adds a hook to the list at the specified index
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public void addPreHook(String className, int indexToAddAt) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
	
	/**
	 * Adds a hook to the list at the specified index
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public void addPostHook(String className, int indexToAddAt) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
	
	/**
	 * Adds a hook to the list at the specified index
	 */
	public void delPreHood(int indexToRemAt);
	
	/**
	 * Adds a hook to the list at the specified index
	 */
	public void delPostHood(int indexToRemAt);

}
