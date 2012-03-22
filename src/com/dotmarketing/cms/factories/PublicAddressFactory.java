/**
 */

package com.dotmarketing.cms.factories;

import java.util.List;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.NoSuchAddressException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.AddressUtil;
import com.liferay.portal.model.Address;

/**
 *
 *
 */
public class PublicAddressFactory extends AddressUtil {

	@SuppressWarnings("unchecked")
	public static List<Address> getAddressesByUserId(String p0) throws SystemException {
		return findByUserId(p0);
	}
	
	public static Address getAddressById(String p0) throws SystemException {
	    
	    	try{
	    	    return findByPrimaryKey(p0);
	    	}
	    	catch(NoSuchAddressException nsae){
	    	    
	    	    return getInstance();
	    	}
	    	catch(SystemException se){
	    	    throw new DotRuntimeException("Can't get Address");
	    	}

	}
	
	
	
	
	
	
	/*
	public static Address getPrimaryAddress(String user) throws SystemException {
	    
			List addresses = findByUserId(user);
			try{
			    return (Address) addresses.get(0);
			}
			catch(Exception e){
			    return new Amode;
			    
			}

	}
	*/
	public static Address getInstance(){
	    String addressId = null;
	    try{
	        addressId = 	Long.toString(CounterManagerUtil.increment(	Address.class.getName() ));
	    
	    }
	    catch(SystemException e){
	        throw new DotRuntimeException("Can't get a counter");
	    }
	    return new Address(addressId);
	}
	
	
	public static void save(Address a)  {
	    
	    try{
	        update(a);
	    }
	    catch(SystemException e){
	        throw new DotRuntimeException("Can't save the address");
	    }

	}
	public static void delete(Address a ){
	    try{
	        remove(a.getAddressId());
	        
	    }
	    catch (NoSuchAddressException e) {
	    	Logger.error(PublicAddressFactory.class, "No such Address to delete");
        }
	    catch (SystemException e) {
	    	Logger.error(PublicAddressFactory.class, "Deleting Address", e);
        }
	    
	    
	    
	}
	
	
	
	
}