package com.dotcms.publisher.endpoint.business;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

public class PublishingEndPointFactoryImpl extends PublishingEndPointFactory {
    private PublishingEndPointCache cache = null;

    public PublishingEndPointFactoryImpl() {
        super();
        cache = CacheLocator.getPublishingEndPointCache();
    }

    public void ensureCacheIsLoaded() throws DotDataException{
        if(cache.isLoaded() == false) {
            getEndPoints();
        }
    }

    public List<PublishingEndPoint> getEndPoints() throws DotDataException {
        if(cache.isLoaded())
            return cache.getEndPoints();

        List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
        DotConnect dc = new DotConnect();
        dc.setSQL(GET_END_POINTS);
        List<Map<String, Object>> res = dc.loadObjectResults();
        for(Map<String, Object> row : res){
            PublishingEndPoint endPoint = PublisherUtil.getObjectByMap(row);
            endPoints.add(endPoint);
            cache.add(endPoint);
        }
        cache.setLoaded(true);
        return endPoints;
    }

    public List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException {
        ensureCacheIsLoaded();
        List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
        List<PublishingEndPoint> allEndPoints = getEndPoints();
        for(PublishingEndPoint endPoint : allEndPoints) {
            if(endPoint.isSending() == false)
                endPoints.add(endPoint);
        }
        return endPoints;
    }

    public PublishingEndPoint getEndPointById(String id) throws DotDataException {
        ensureCacheIsLoaded();
        return cache.getEndPointById(id);
    }

    public void store(PublishingEndPoint anEndPoint) throws DotDataException {
        ensureCacheIsLoaded();
        anEndPoint.setId(UUID.randomUUID().toString());
        DotConnect dc = new DotConnect();
        dc.setSQL(SET_END_POINT);
        dc.addParam(anEndPoint.getId());
        dc.addParam(anEndPoint.getGroupId());
        dc.addParam(anEndPoint.getServerName().toString());
        dc.addParam(anEndPoint.getAddress());
        dc.addParam(anEndPoint.getPort());
        dc.addParam(anEndPoint.getProtocol());
        dc.addParam(anEndPoint.isEnabled());
        dc.addParam(anEndPoint.getAuthKey().toString());
        dc.addParam(anEndPoint.isSending());
        dc.loadResult();
        cache.clearCache(); // clear cache to make sure that all nodes in the cluster update
    }

    public void update(PublishingEndPoint anEndPoint) throws DotDataException {
        ensureCacheIsLoaded();
        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_END_POINT);
        dc.addParam(anEndPoint.getGroupId());
        dc.addParam(anEndPoint.getServerName().toString());
        dc.addParam(anEndPoint.getAddress());
        dc.addParam(anEndPoint.getPort());
        dc.addParam(anEndPoint.getProtocol());
        dc.addParam(anEndPoint.isEnabled());
        dc.addParam(anEndPoint.getAuthKey().toString());
        dc.addParam(anEndPoint.isSending());
        dc.addParam(anEndPoint.getId());
        dc.loadResult();
        cache.clearCache();                //clear cache to make sure all nodes in the cluster update
    }

    public void deleteEndPointById(String id) throws DotDataException {
        ensureCacheIsLoaded();
        DotConnect dc = new DotConnect();
        dc.setSQL(DELETE_END_POINT_BY_ID);
        dc.addParam(id);
        dc.loadResult();
        cache.removeEndPointById(id);
    }

    public PublishingEndPoint getEnabledSendingEndPointByAddress(String address) throws DotDataException {
        ensureCacheIsLoaded();
        List<PublishingEndPoint> allEndPoints = getEndPoints();
        for(PublishingEndPoint endPoint : allEndPoints) {
            if(isMatchingEndpoint(endPoint.getAddress(), address) && endPoint.isEnabled() && endPoint.isSending())
                return endPoint;
        }
        return null;
    }

    boolean isMatchingEndpoint(String endPointAddress, String requestAddress) {

        boolean result = false;
        try {
            InetAddress endPointInetAddress = getInetAddress(endPointAddress);
            InetAddress requestInetAddress = getInetAddress(requestAddress);
            result = endPointInetAddress.equals(requestInetAddress);
        }
        catch(UnknownHostException e) {
            Logger.error(this.getClass(), "Unable to compare endpoints.", e);
        }
        return result;

    }

    private InetAddress getInetAddress(String address) throws UnknownHostException {
        try {
            return InetAddress.getByName(address);
        }
        catch(UnknownHostException e) {
            Logger.error(this.getClass(), "Unable to resolve inetAddress for " + address);
            throw e;
        }
    }

    public List<PublishingEndPoint> getSendingEndPointsByEnvironment(String environmentId) throws DotDataException {
        ensureCacheIsLoaded();
        List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
        for(PublishingEndPoint endPoint : getEndPoints()) {
            if(endPoint.getGroupId().equals(environmentId) && endPoint.isSending()==false) {
                endPoints.add(endPoint);
            }
        }
        return endPoints;
    }

    public List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException {
        ensureCacheIsLoaded();
        List<PublishingEndPoint> receiverEndPoints = new ArrayList<PublishingEndPoint>();
        List<PublishingEndPoint> allEndPoints = getEndPoints();
        for(PublishingEndPoint endPoint : allEndPoints) {
            if(endPoint.isSending() == false && endPoint.isEnabled()) {
                receiverEndPoints.add(endPoint);
            }
        }
        return receiverEndPoints;
    }

    @Override
    public List<String> findSendGroups() throws DotDataException {
        ensureCacheIsLoaded();
        List<String> sendGroups = new ArrayList<String>();
        List<PublishingEndPoint> allEndPoints = getEndPoints();
        for(PublishingEndPoint endPoint : allEndPoints) {
            if(endPoint.getGroupId() != null && sendGroups.contains(endPoint.getGroupId()) == false) {
                sendGroups.add(endPoint.getGroupId());
            }
        }
        return sendGroups;
    }

    @Override
    public PublishingEndPoint getEndPointByName(String name) throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL(SELECT_END_POINT_BY_NAME);
        dc.addParam(name);
        List<Map<String, Object>> res = dc.loadObjectResults();
        PublishingEndPoint e = null;

        if(res!=null && !res.isEmpty()) {
            Map<String, Object> row = res.get(0);
            e = PublisherUtil.getObjectByMap(row);
        }

        return e;

    }
}