package com.dotmarketing.business;

import org.apache.cactus.ServletTestCase;

/**
 * 
 * This is the first version of this test class and only contains a few of methods to be tested for dotConnect
 * @author David Torres
 * @version 1.8
 * @since 1.8
 *
 */
public class FactoryLocatorTest extends ServletTestCase {

	public void testLocatePermissionsFactory() {
		PermissionFactory fac = FactoryLocator.getPermissionFactory();
		assertNotNull(fac);
		assertTrue(fac instanceof PermissionBitFactoryImpl);
	}
	
}
