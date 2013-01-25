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
import com.dotmarketing.util.Logger;

/**
 * @author brent griffin
 *
 */
public class PublisherEndpointAPITest extends TestBase{

	private static PublisherEndpointAPI api;
	private static ArrayList<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
	
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
	
	@BeforeClass
	public static void init() {
		api = APILocator.getPublisherEndpointAPI();
		endPoints.clear();
		endPoints.add(CreatePublishingEndPoint("01", "G01", "Alpha", "192.168.1.1", "81", "HTTPS", true, "AuthKey01", false));
		endPoints.add(CreatePublishingEndPoint("02", "G01", "Beta", "192.168.1.2", "82", "HTTPS", true, "AuthKey02", false));
		endPoints.add(CreatePublishingEndPoint("03", "G01", "Gamma", "192.168.1.3", "83", "HTTPS", false, "AuthKey03", false));
		endPoints.add(CreatePublishingEndPoint("04", "G01", "Delta", "192.168.1.4", "84", "HTTPS", true, "AuthKey04", true));
		endPoints.add(CreatePublishingEndPoint("05", "G02", "Epsilon", "192.168.1.5", "85", "HTTPS", true, "AuthKey05", false));
		endPoints.add(CreatePublishingEndPoint("06", "G02", "Zeta", "192.168.1.6", "86", "HTTPS", false, "AuthKey06", false));
		endPoints.add(CreatePublishingEndPoint("07", "G02", "Eta", "192.168.1.7", "87", "HTTPS", true, "AuthKey07", false));
		endPoints.add(CreatePublishingEndPoint("08", "G02", "Theta", "192.168.1.8", "88", "HTTPS", true, "AuthKey08", true));
		endPoints.add(CreatePublishingEndPoint("09", "G02", "Iota", "192.168.1.9", "89", "HTTPS", false, "AuthKey09", true));
	}
	
	@AfterClass
	public static void cleanup() {
		
	}

	
	// There should not be any endpoints at the beginning of the test
	@Test
	public void ensureProperStartingState() throws DotDataException {
		List<PublishingEndPoint> endPoints = api.getAllEndpoints();
		assertTrue(endPoints.size() == 0);
		endPoints = api.findReceiverEndpoints();
		assertTrue(endPoints.size() == 0);
		endPoints = api.getAllEndpoints();
		assertTrue(endPoints.size() == 0);
	}
	
	@Test
	public void insertionOfEndPoints() throws DotDataException {
		HibernateUtil.startTransaction();
		for(PublishingEndPoint endPoint : endPoints) {
			api.saveEndpoint(endPoint);
		}
		HibernateUtil.commitTransaction();
		List<PublishingEndPoint> savedEndPoints = api.getAllEndpoints();
		assertTrue(savedEndPoints.size() == endPoints.size());
	}
	
	@Test
	public void receivingEndPoints() throws DotDataException {
		List<PublishingEndPoint> endPoints = api.getReceivingEndpoints();
		Logger.debug(PublisherEndpointAPITest.class, "endPoints.size() = " + endPoints);
	}
	
	@Test
	public void deletionOfEndPoints() throws DotDataException {
		HibernateUtil.startTransaction();
		List<PublishingEndPoint> savedEndPoints = api.getAllEndpoints();
		for(PublishingEndPoint endPoint : savedEndPoints) {
			api.deleteEndpointById(endPoint.getId());
		}
		HibernateUtil.commitTransaction();
		savedEndPoints = api.getAllEndpoints();
		assertTrue(savedEndPoints.size() == 0);
	}
}
