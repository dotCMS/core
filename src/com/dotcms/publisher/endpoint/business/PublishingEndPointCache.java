/**
 * 
 */
package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;

/**
 * @author Brent Griffin
 *
 */
public interface PublishingEndPointCache {
	public boolean isLoaded();
	public void setLoaded(boolean isLoaded);
	public List<PublishingEndPoint> getEndPoints();
	public PublishingEndPoint getEndPointById(String id);
	public void add(PublishingEndPoint anEndPoint);
	public void removeEndPointById(String id);
	public void clearCache();
}
