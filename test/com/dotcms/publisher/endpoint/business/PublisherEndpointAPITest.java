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
public class PublisherEndpointAPITest extends TestBase{

	private static PublisherEndpointAPI api;
	private static ArrayList<PublishingEndPoint> _endPoints = new ArrayList<PublishingEndPoint>();
	
	public static PublishingEndPoint CreatePublishingEndPoint(String id, String groupId, String serverName, String address, String port, String protocol, boolean enabled, String authKey, boolean sending) {
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
	
	public static void TestEndpointsForEquality(PublishingEndPoint searchedForEndpoint, PublishingEndPoint foundEndpoint) {
		assertTrue(searchedForEndpoint.getId().equals(foundEndpoint.getId()));
		assertTrue(searchedForEndpoint.getGroupId().equals(foundEndpoint.getGroupId()));
		assertTrue(searchedForEndpoint.getServerName().toString().equals(foundEndpoint.getServerName().toString()));
		assertTrue(searchedForEndpoint.getAddress().equals(foundEndpoint.getAddress()));
		assertTrue(searchedForEndpoint.getPort().equals(foundEndpoint.getPort()));
		assertTrue(searchedForEndpoint.getProtocol().equals(foundEndpoint.getProtocol()));
		assertTrue(searchedForEndpoint.isEnabled() == foundEndpoint.isEnabled());
		assertTrue(searchedForEndpoint.getAuthKey().toString().equals(foundEndpoint.getAuthKey().toString()));
		assertTrue(searchedForEndpoint.isSending() == foundEndpoint.isSending());
	}
	
	@BeforeClass
	public static void init() {
		api = APILocator.getPublisherEndpointAPI();
		_endPoints.add(CreatePublishingEndPoint("01", "G01", "Alpha", "192.168.1.1", "81", "https", true, "AuthKey01", false));
		_endPoints.add(CreatePublishingEndPoint("02", "G01", "Beta", "192.168.1.2", "82", "https", true, "AuthKey02", false));
		_endPoints.add(CreatePublishingEndPoint("03", "G01", "Gamma", "192.168.1.3", "83", "https", false, "AuthKey03", false));
		_endPoints.add(CreatePublishingEndPoint("04", "G01", "Delta", "192.168.1.4", "84", "https", true, "AuthKey04", true));
		_endPoints.add(CreatePublishingEndPoint("05", "G02", "Epsilon", "192.168.1.5", "85", "https", true, "AuthKey05", false));
		_endPoints.add(CreatePublishingEndPoint("06", "G02", "Zeta", "192.168.1.6", "86", "https", false, "AuthKey06", false));
		_endPoints.add(CreatePublishingEndPoint("07", "G02", "Eta", "192.168.1.7", "87", "https", true, "AuthKey07", false));
		_endPoints.add(CreatePublishingEndPoint("08", "G02", "Theta", "192.168.1.8", "88", "https", true, "AuthKey08", true));
		_endPoints.add(CreatePublishingEndPoint("09", "G02", "Iota", "192.168.1.9", "89", "https", false, "AuthKey09", true));
	}
	
	@AfterClass
	public static void cleanup() {
		_endPoints.clear();		
	}

	
	// There should not be any endpoints at the beginning of the test
	@Test
	public void test() throws DotDataException {
		try {
			HibernateUtil.startTransaction();
			// Ensure proper starting state - no endpoints in database
			{
				List<PublishingEndPoint> savedEndPoints = api.getAllEndpoints();
				assertTrue(savedEndPoints.size() == 0);
				savedEndPoints = api.findReceiverEndpoints();
				assertTrue(savedEndPoints.size() == 0);
				savedEndPoints = api.getAllEndpoints();
				assertTrue(savedEndPoints.size() == 0);
				
				// Insert test endpoints
				for(PublishingEndPoint endPoint : _endPoints) {
					api.saveEndpoint(endPoint);
				}
				savedEndPoints = api.getAllEndpoints();
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
	
			// test receiving endpoint lookups
			{
				List<PublishingEndPoint> savedEndPoints = api.getReceivingEndpoints();
				assertTrue(savedEndPoints.size() == 6);
				
				savedEndPoints = api.findReceiverEndpoints();
				assertTrue(savedEndPoints.size() == 4);
			}
			
			// find endpoint by id
			{
				PublishingEndPoint searchForEndpoint = _endPoints.get(5);
				PublishingEndPoint foundEndpoint = api.findEndpointById(searchForEndpoint.getId());
				assertFalse(foundEndpoint == null);
				TestEndpointsForEquality(searchForEndpoint, foundEndpoint);
			}
	
			// find sender by endpoint address
			{
				PublishingEndPoint searchForEndpoint = _endPoints.get(0);
				assertFalse(searchForEndpoint.isSending());
		
				// looking for address that is not a sender - should return null
				PublishingEndPoint foundEndpoint = api.findSenderEndpointByAddress(searchForEndpoint.getAddress());
				assertTrue(foundEndpoint == null);
			}
			
			// looking for address of valid sender
			{
				PublishingEndPoint searchForEndpoint = _endPoints.get(3);
				assertTrue(searchForEndpoint.isEnabled() == true);
				assertTrue(searchForEndpoint.isSending() == true);
				PublishingEndPoint foundEndpoint = api.findSenderEndpointByAddress(searchForEndpoint.getAddress());
				TestEndpointsForEquality(searchForEndpoint, foundEndpoint);
			}
			
			// find send groups
			List<String> groupList = api.findSendGroups();
			assertTrue(groupList.size() == 2);
			assertTrue(groupList.contains("G01"));
			assertTrue(groupList.contains("G02"));
	
			// update endpoint
			PublishingEndPoint endpointToUpdate = api.findEndpointById(_endPoints.get(8).getId());
			assertTrue(endpointToUpdate.getAuthKey().toString().equals(_endPoints.get(8).getAuthKey().toString()));
			endpointToUpdate.setAuthKey(new StringBuilder("NewAuthKey"));
			api.updateEndpoint(endpointToUpdate);
			PublishingEndPoint endpointToValidate = api.findEndpointById(endpointToUpdate.getId());
			assertTrue(endpointToValidate.getAuthKey().toString().equals("NewAuthKey"));
	
			// delete endpoints
			{
				List<PublishingEndPoint>savedEndPoints = api.getAllEndpoints();
				for(PublishingEndPoint endPoint : savedEndPoints) {
					api.deleteEndpointById(endPoint.getId());
				}		
				savedEndPoints = api.getAllEndpoints();
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
