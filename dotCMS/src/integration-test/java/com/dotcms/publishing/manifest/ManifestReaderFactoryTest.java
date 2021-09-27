package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.GenerateBundlePublisher;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import java.io.File;
import java.util.Collection;
import org.junit.BeforeClass;
import org.junit.Test;

public class ManifestReaderFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /***
     * Method to test: {@link CSVManifestReader#getAssets(ManifestReason)}
     * when: A Manifest is created and a {@link ContentType} is added
     * should: the method {@link CSVManifestReader#getAssets(ManifestReason)} called with
     * {@link ManifestReason#INCLUDE_BY_USER} should return the content tupe added
     *
     * @throws DotPublishingException
     */
    @Test
    public void createCSVManifestReader() throws DotPublishingException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(GenerateBundlePublisher.class));
        config.setOperation(PublisherConfig.Operation.PUBLISH);
        config.setLuceneQueries(list());
        config.setId("PublisherAPIImplTest_" + System.currentTimeMillis());

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(contentType))
                .filter(filterDescriptor)
                .nextPersisted();

        final PublishStatus publish = APILocator.getPublisherAPI().publish(config);

        final CSVManifestReader csvManifestReader = ManifestReaderFactory.INSTANCE
                .createCSVManifestReader(bundle.getId());

        assertNotNull(csvManifestReader);
        final Collection<ManifestInfo> assets = csvManifestReader
                .getAssets(ManifestReason.INCLUDE_BY_USER);

        assertEquals(1, assets.size());
        assertEquals(contentType.id(), assets.iterator().next().id());;
    }
}
