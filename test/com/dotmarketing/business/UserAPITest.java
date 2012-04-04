package com.dotmarketing.business;

import java.util.HashMap;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;


public class UserAPITest extends ServletTestCase {
	UserAPI uAPI;
	UserProxyAPI upAPI;
	User testUser;
	
	@Override
	protected void setUp() throws Exception {
		uAPI = APILocator.getUserAPI();
		upAPI = APILocator.getUserProxyAPI();
		testUser = uAPI.createUser("JUNIT TEST", "junitdev@dotcms.org");
		testUser.setFirstName("JUNITFIRSTTESTER");
		testUser.setLastName("JUNITLASTTESTER");
		uAPI.save(testUser, APILocator.getUserAPI().getSystemUser(), false);
	}
	
	@Override
	protected void tearDown() throws Exception {
		try{
			uAPI.delete(testUser, uAPI.getSystemUser(), false);
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void testLocatePermissionsAPI() {
		UserAPI api = APILocator.getUserAPI();
		assertNotNull(api);
		assertTrue(api instanceof UserAPI);
	}
	
	public void testEncryptUserId(){
		try{
			String encrypted = uAPI.encryptUserId("testjunituserid");
			fail("UserID should not have encryoted and should have thrown a DotStateException");
		}catch (Exception e) {
			
		}
		String encrypted = uAPI.encryptUserId(testUser.getUserId());
		assertNotNull(encrypted);
		assertNotSame("Ecypted userid is the smae as original",encrypted, "testuserid");
	}
	
	public void testLoadUserById() throws NoSuchUserException, DotDataException, DotSecurityException{
		User u = uAPI.loadUserById(testUser.getUserId(), uAPI.getSystemUser(), false);
		assertNotNull(u);
		assertEquals(u.getUserId(), testUser.getUserId());
		u = null;
		try{
			u = uAPI.loadUserById(testUser.getUserId(), testUser, false);
			fail("Should have thrown a SecurityException");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		assertNull(u);
		u = null;
		try{
			u = uAPI.loadUserById("My ID Which should not exist", uAPI.getSystemUser(), false);
			fail("Should have thrown a NoSuchUserExcpetion");
		}catch (NoSuchUserException e) {
			// TODO: handle exception
		}
		assertNull(u);
	}
	
	public void testLoadByUserByEmail() throws NoSuchUserException, DotDataException, DotSecurityException{
		User u = uAPI.loadByUserByEmail(testUser.getEmailAddress(), uAPI.getSystemUser(), false);
		assertNotNull(u);
		assertEquals(u.getEmailAddress(), testUser.getEmailAddress());
		u = null;
		try{
			u = uAPI.loadByUserByEmail(testUser.getEmailAddress(), testUser, false);
			fail("Should have thrown a SecurityException");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		assertNull(u);
		u = null;
		try{
			u = uAPI.loadByUserByEmail("thisemaildoesntexist@asdf.com", uAPI.getSystemUser(), false);
			fail("Should have thrown a NoSuchUserExcpetion");
		}catch (NoSuchUserException e) {
			// TODO: handle exception
		}
		assertNull(u);
	}
	
	public void testFindAllUsers() throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL("select count(user_.userId) as c1 from user_ where companyid = 'dotcms.org'");
		long count = new Long(((HashMap<String, String>)(dc.loadResults().get(0))).get("c1"));
		List<User> users = uAPI.findAllUsers();
		assertEquals("Find all users returned a different number then what exists in the DB", count, users.size());
	}
	
	public void testGetUsersByName() throws DotDataException{
		List<User> u = uAPI.getUsersByName("JUNITFIRSTTESTER", 0, 0, APILocator.getUserAPI().getSystemUser(), false);
		assertNotNull(u);
		assertEquals(1,u.size());
	}
	
	public void testCreateUser() throws DotDataException{
		User u = null;
		try {
			u = uAPI.createUser("JUNIT TEST", "dev@dotcms.org");
			fail("User should already exist and should not be able to be created");
		} catch (DuplicateUserException e) {
			
		}	
	}
	
	public void testGetDefaultUser() throws DotDataException{
		User u = uAPI.getDefaultUser();
		assertNotNull(u.getUserId());
	}
	
	public void testGetSystemUser() throws DotDataException{
		User u = uAPI.getSystemUser();
		assertNotNull(u.getUserId());
	}

	public void testGetAnonymousUser() throws DotDataException{
		User u = uAPI.getSystemUser();
		assertNotNull(u.getUserId());
	}
	
	public void testUserExistsWithEmail() throws DotDataException, NoSuchUserException{
		assertTrue(uAPI.userExistsWithEmail(testUser.getEmailAddress()));
	}
	
	public void testGetCountUsersByNameOrEmail() throws DotDataException{
		long i  = uAPI.getCountUsersByNameOrEmail(testUser.getFirstName());
		assertEquals(1, i);
		i = uAPI.getCountUsersByNameOrEmail(testUser.getEmailAddress());
		assertEquals(1, i);
	}
	
	public void testGetUsersByNameOrEmail(String filter,int page,int pageSize) throws DotDataException{
		List<User> users = uAPI.getUsersByNameOrEmail(testUser.getFirstName(), -1,-1);
		assertEquals(1, users.size());
		users = uAPI.getUsersByNameOrEmail(testUser.getLastName(), -1,-1);
		assertEquals(1, users.size());
		users = uAPI.getUsersByNameOrEmail(testUser.getEmailAddress(), -1,-1);
		assertEquals(1, users.size());
	}
	
	public void testSave() throws DotDataException, DotSecurityException{
		try{
			uAPI.save(testUser, testUser, false);
			fail("Should not be able to save with the test user");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		uAPI.save(testUser, APILocator.getUserAPI().getSystemUser(), false);
	}
	
	public void testDelete() throws DotDataException, DotSecurityException{
		try{
			uAPI.delete(testUser, testUser, false);
			fail("Should not be able to save with the test user");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		uAPI.delete(testUser, APILocator.getUserAPI().getSystemUser(), false);
	}
	
	public void testGetUserProxy() throws DotRuntimeException, DotSecurityException, DotDataException{
		try{
			upAPI.getUserProxy(testUser.getUserId(), testUser, false);
			fail("Should not be able to get the userproxy with the testuser");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		upAPI.getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
	}
	
	public void testGetUserProxyByLongLiveCookie() throws DotRuntimeException, DotSecurityException, DotDataException{
		UserProxy up = upAPI.getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
		up.setLongLivedCookie("mycookie");
		upAPI.saveUserProxy(up, APILocator.getUserAPI().getSystemUser(), false);
		try{
			upAPI.getUserProxyByLongLiveCookie("mycookie", testUser, false);
			fail("Should not be able to get the userproxy with the testuser");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		upAPI.getUserProxyByLongLiveCookie("mycookie", APILocator.getUserAPI().getSystemUser(), false);
	}
	
	public void testSaveUserProxy() throws DotRuntimeException, DotDataException, DotSecurityException{
		UserProxy up = upAPI.getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
		up.setSchool("dotSchool");
		try{
			upAPI.saveUserProxy(up, testUser, false);
			fail("Should not be able to get the userproxy with the testuser");
		}catch (DotSecurityException e) {
			// TODO: handle exception
		}
		upAPI.saveUserProxy(up, APILocator.getUserAPI().getSystemUser(), false);
	}
	
    public void testFindUsersTitle() throws DotDataException, DotRuntimeException, DotSecurityException{
    	UserProxy up = upAPI.getUserProxy(testUser.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
		up.setTitle("My Junit Title");
		upAPI.saveUserProxy(up, APILocator.getUserAPI().getSystemUser(), false);
		List<String> ss = upAPI.findUsersTitle();
		boolean found = false;
		for (String s : ss) {
			if(s.equals("My Junit Title")){
				found = true;
			}
		}
		if(found){
			
		}else{
			fail("Title not found");
		}
    }
	
}
