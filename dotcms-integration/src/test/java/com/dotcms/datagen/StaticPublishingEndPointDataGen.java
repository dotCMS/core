package com.dotcms.datagen;


import com.dotcms.publisher.endpoint.bean.impl.StaticPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

public class StaticPublishingEndPointDataGen extends AbstractDataGen<StaticPublishingEndPoint> {

    private String address = "127.0.0.1";
    private boolean sending = false;
    private String authKey = "123567";
    private String port = "8080";
    private String protocol = "http";
    private boolean enabled = true;
    private String serverName = null;
    private Environment environment;

    public StaticPublishingEndPointDataGen address(final String address){
        this.address = address;
        return this;
    }

    public StaticPublishingEndPointDataGen sending(final boolean sending){
        this.sending = sending;
        return this;
    }

    public StaticPublishingEndPointDataGen authKey(final String authKey){
        this.authKey = authKey;
        return this;
    }

    public StaticPublishingEndPointDataGen port(final String port){
        this.port = port;
        return this;
    }

    public StaticPublishingEndPointDataGen protocol(final String protocol){
        this.protocol = protocol;
        return this;
    }

    public StaticPublishingEndPointDataGen enabled(final boolean enabled){
        this.enabled = enabled;
        return this;
    }

    public StaticPublishingEndPointDataGen serverName(final String serverName){
        this.serverName = serverName;
        return this;
    }

    public StaticPublishingEndPointDataGen environment(final Environment environment){
        this.environment = environment;
        return this;
    }

    @Override
    public StaticPublishingEndPoint next() {
        final StaticPublishingEndPoint publishingEndPoint = new StaticPublishingEndPoint();
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
        publishingEndPoint.setProtocol(protocol);

        return publishingEndPoint;
    }

    @Override
    public StaticPublishingEndPoint persist(StaticPublishingEndPoint publishingEndPoint) {
        try {
            APILocator.getPublisherEndPointAPI().saveEndPoint(publishingEndPoint);
            return (StaticPublishingEndPoint) APILocator.getPublisherEndPointAPI().findEndPointById(publishingEndPoint.getId());
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}
