package com.dotcms.datagen;

import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;


public class PushPublishingEndPointDataGen extends AbstractDataGen<PushPublishingEndPoint> {

    private String address = "127.0.0.1";
    private boolean sending = false;
    private String authKey = "123567";
    private String port = "8080";
    private boolean enabled = true;
    private String serverName = null;
    private Environment environment;

    public PushPublishingEndPointDataGen address(final String address){
        this.address = address;
        return this;
    }

    public PushPublishingEndPointDataGen sending(final boolean sending){
        this.sending = sending;
        return this;
    }

    public PushPublishingEndPointDataGen authKey(final String authKey){
        this.authKey = authKey;
        return this;
    }

    public PushPublishingEndPointDataGen port(final String port){
        this.port = port;
        return this;
    }

    public PushPublishingEndPointDataGen enabled(final boolean enabled){
        this.enabled = enabled;
        return this;
    }

    public PushPublishingEndPointDataGen serverName(final String serverName){
        this.serverName = serverName;
        return this;
    }

    public PushPublishingEndPointDataGen environment(final Environment environment){
        this.environment = environment;
        return this;
    }

    @Override
    public PushPublishingEndPoint next() {
        final PushPublishingEndPoint publishingEndPoint = new PushPublishingEndPoint();
        publishingEndPoint.setAddress(address);
        publishingEndPoint.setSending(sending);
        publishingEndPoint.setAuthKey(PublicEncryptionFactory.encryptString(authKey));
        publishingEndPoint.setPort(port);
        publishingEndPoint.setEnabled(enabled);
        publishingEndPoint.setServerName(
                new StringBuilder(serverName == null ? "ServerName_" + System.currentTimeMillis()
                        : serverName)
        );
        publishingEndPoint.setGroupId(environment.getId());
        publishingEndPoint.setProtocol("http");

        return publishingEndPoint;
    }

    @Override
    public PushPublishingEndPoint persist(PushPublishingEndPoint publishingEndPoint) {
        try {
            APILocator.getPublisherEndPointAPI().saveEndPoint(publishingEndPoint);
            return (PushPublishingEndPoint) APILocator.getPublisherEndPointAPI().findEndPointById(publishingEndPoint.getId());
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}
