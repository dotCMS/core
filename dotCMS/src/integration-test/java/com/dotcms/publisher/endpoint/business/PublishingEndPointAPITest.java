/**
 * 
 */
package com.dotcms.publisher.endpoint.business;

import java.util.*;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.*;
import com.dotmarketing.business.*;
import com.dotcms.TestBase;
import com.dotcms.publisher.endpoint.bean.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author brent griffin
 *
 */
public class PublishingEndPointAPITest extends TestBase{

	private static PublishingEndPointAPI api;
	private static ArrayList<PublishingEndPoint> _endPoints = new ArrayList<PublishingEndPoint>();
	
	public static PublishingEndPoint createPublishingEndPoint(String id, String groupId, String serverName, String address, String port, String protocol, boolean enabled, String authKey, boolean sending) {
		PublishingEndPoint retValue = new PublishingEndPoint();
		retValue.setId(id);
		retValue.setGroupId(groupId);
		retValue.setServerName(new StringBuilder(serverName));
		retValue.setAddress(address);
		retValue.setPort(port);
		retValue.setProtocol(protocol);
		retValue.setEnabled(enabled);
		retValue.setAuthKey(new StringBuilder(authKey));
		retValue.setSending(sending);
		return retValue;
	}
	
	public static void testEndPointsForEquality(PublishingEndPoint searchedForEndPoint, PublishingEndPoint foundEndPoint) {
		assertTrue(searchedForEndPoint.getId().equals(foundEndPoint.getId()));
		assertTrue(searchedForEndPoint.getGroupId().equals(foundEndPoint.getGroupId()));
		assertTrue(searchedForEndPoint.getServerName().toString().equals(foundEndPoint.getServerName().toString()));
		assertTrue(searchedForEndPoint.getAddress().equals(foundEndPoint.getAddress()));
		assertTrue(searchedForEndPoint.getPort().equals(foundEndPoint.getPort()));
		assertTrue(searchedForEndPoint.getProtocol().equals(foundEndPoint.getProtocol()));
		assertTrue(searchedForEndPoint.isEnabled() == foundEndPoint.isEnabled());
		assertTrue(searchedForEndPoint.getAuthKey().toString().equals(foundEndPoint.getAuthKey().toString()));
		assertTrue(searchedForEndPoint.isSending() == foundEndPoint.isSending());
	}
	
	@BeforeClass
	public static void init() {
		api = APILocator.getPublisherEndPointAPI();
		_endPoints.add(createPublishingEndPoint("01", "G01", "Alpha", "192.168.1.1", "81", "https", true, "AuthKey01", false));
		_endPoints.add(createPublishingEndPoint("02", "G01", "Beta", "192.168.1.2", "82", "https", true, "AuthKey02", false));
		_endPoints.add(createPublishingEndPoint("03", "G01", "Gamma", "192.168.1.3", "83", "https", false, "AuthKey03", false));
		_endPoints.add(createPublishingEndPoint("04", "G01", "Delta", "192.168.1.4", "84", "https", true, "AuthKey04", true));
		_endPoints.add(createPublishingEndPoint("05", "G02", "Epsilon", "192.168.1.5", "85", "https", true, "AuthKey05", false));
		_endPoints.add(createPublishingEndPoint("06", "G02", "Zeta", "192.168.1.6", "86", "https", false, "AuthKey06", false));
		_endPoints.add(createPublishingEndPoint("07", "G02", "Eta", "192.168.1.7", "87", "https", true, "AuthKey07", false));
		_endPoints.add(createPublishingEndPoint("08", "G02", "Theta", "192.168.1.8", "88", "https", true, "AuthKey08", true));
		_endPoints.add(createPublishingEndPoint("09", "G02", "Iota", "192.168.1.9", "89", "https", false, "AuthKey09", true));
	}
	
	@AfterClass
	public static void cleanup() throws Exception {
		_endPoints.clear();		
		for(PublishingEndPoint pep : api.getAllEndPoints()) {
		    api.deleteEndPointById(pep.getId());
		}
	}

	
	// There should not be any end points at the beginning of the test
	@Test
	public void test() throws DotDataException {
		try {
			HibernateUtil.startTransaction();
			// Ensure proper starting state - no end points in database
			{
				List<PublishingEndPoint> savedEndPoints = api.getAllEndPoints();
				assertTrue(savedEndPoints.size() == 0);
				savedEndPoints = api.getEnabledReceivingEndPoints();
				assertTrue(savedEndPoints.size() == 0);
				savedEndPoints = api.getAllEndPoints();
				assertTrue(savedEndPoints.size() == 0);
				
				// Insert test end points
				for(PublishingEndPoint endPoint : _endPoints) {
					api.saveEndPoint(endPoint);
				}
				savedEndPoints = api.getAllEndPoints();
				assertTrue(savedEndPoints.size() == _endPoints.size());
				
				for(PublishingEndPoint savedEndPoint : savedEndPoints) {
					for(PublishingEndPoint endPoint : _endPoints) {
						if(savedEndPoint.getServerName().equals(endPoint.getServerName())){
							endPoint.setId(savedEndPoint.getId());
							break;
						}
					}
				}
			}
	
			// test receiving end point lookups
			{
				List<PublishingEndPoint> savedEndPoints = api.getReceivingEndPoints();
				assertTrue(savedEndPoints.size() == 6);
				
				savedEndPoints = api.getEnabledReceivingEndPoints();
				assertTrue(savedEndPoints.size() == 4);
			}
			
			// find end point by id
			{
				PublishingEndPoint searchForEndPoint = _endPoints.get(5);
				PublishingEndPoint foundEndPoint = api.findEndPointById(searchForEndPoint.getId());
				assertFalse(foundEndPoint == null);
				testEndPointsForEquality(searchForEndPoint, foundEndPoint);
			}
	
			// find sender by end point address
			{
				PublishingEndPoint searchForEndPoint = _endPoints.get(0);
				assertFalse(searchForEndPoint.isSending());
		
				// looking for address that is not a sender - should return null
				PublishingEndPoint foundEndPoint = api.findEnabledSendingEndPointByAddress(searchForEndPoint.getAddress());
				assertTrue(foundEndPoint == null);
			}
			
			// looking for address of valid sender
			{
				PublishingEndPoint searchForEndPoint = _endPoints.get(3);
				assertTrue(searchForEndPoint.isEnabled() == true);
				assertTrue(searchForEndPoint.isSending() == true);
				PublishingEndPoint foundEndPoint = api.findEnabledSendingEndPointByAddress(searchForEndPoint.getAddress());
				testEndPointsForEquality(searchForEndPoint, foundEndPoint);
			}
			
			// find send groups
			List<String> groupList = api.findSendGroups();
			assertTrue(groupList.size() == 2);
			assertTrue(groupList.contains("G01"));
			assertTrue(groupList.contains("G02"));
	
			// update end point
			PublishingEndPoint endPointToUpdate = api.findEndPointById(_endPoints.get(8).getId());
			assertTrue(endPointToUpdate.getAuthKey().toString().equals(_endPoints.get(8).getAuthKey().toString()));
			endPointToUpdate.setAuthKey(new StringBuilder("NewAuthKey"));
			api.updateEndPoint(endPointToUpdate);
			PublishingEndPoint endPointToValidate = api.findEndPointById(endPointToUpdate.getId());
			assertTrue(endPointToValidate.getAuthKey().toString().equals("NewAuthKey"));
	
			// delete end points
			{
				List<PublishingEndPoint>savedEndPoints = api.getAllEndPoints();
				for(PublishingEndPoint endPoint : savedEndPoints) {
					api.deleteEndPointById(endPoint.getId());
				}		
				savedEndPoints = api.getAllEndPoints();
				assertTrue(savedEndPoints.size() == 0);
			}
		}
		catch (DotDataException e)
		{
			HibernateUtil.rollbackTransaction();
			throw (e);
		}
		
		HibernateUtil.commitTransaction();
	}
}
