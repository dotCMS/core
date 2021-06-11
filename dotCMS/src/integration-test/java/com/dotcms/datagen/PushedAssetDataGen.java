package com.dotcms.datagen;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.Publisher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import java.util.Calendar;
import java.util.Date;

public class PushedAssetDataGen extends AbstractDataGen<PushedAsset> {

    private String assetId;
    private String assetType;
    private Bundle bundle;
    private PublishingEndPoint publishingEndPoint;
    private Environment environment;
    private Date pushDate;
    private Class<? extends Publisher> publisherClass;

    public PushedAssetDataGen assetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public PushedAssetDataGen assetType(String assetType) {
        this.assetType = assetType;
        return this;
    }

    public PushedAssetDataGen bundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public PushedAssetDataGen publishingEndPoint(PublishingEndPoint publishingEndPoint) {
        this.publishingEndPoint = publishingEndPoint;
        return this;
    }

    public PushedAssetDataGen environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    public PushedAssetDataGen pushDate(Date pushDate) {
        this.pushDate = pushDate;
        return this;
    }

    @Override
    public PushedAsset next() {
        final PushedAsset pushedAsset = new PushedAsset();
        pushedAsset.setAssetId(assetId);
        pushedAsset.setAssetType(assetType);
        pushedAsset.setBundleId(bundle.getId());
        pushedAsset.setEndpointId(publishingEndPoint.getId());
        pushedAsset.setEnvironmentId(environment.getId());
        pushedAsset.setPublisher(publisherClass.getName());

        if (pushDate != null) {
            pushedAsset.setPushDate(pushDate);
        }

        return pushedAsset;
    }

    @Override
    public PushedAsset persist(PushedAsset pushedAsset) {
        try {
            APILocator.getPushedAssetsAPI().savePushedAsset(pushedAsset);
            return pushedAsset;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public PushedAssetDataGen publisher(final Class<? extends Publisher> publisherClass) {
        this.publisherClass = publisherClass;
        return this;
    }
}
