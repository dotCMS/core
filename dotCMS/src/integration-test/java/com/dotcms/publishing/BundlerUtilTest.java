package com.dotcms.publishing;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundlerUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void writeBundleXML() throws IOException {
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.add(contentType, PusheableAsset.CONTENT_TYPE, "");

        final File bundlerUtilTest = FileUtil.createTemporaryDirectory("BundlerUtilTest");
        final BundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, bundlerUtilTest);

        final File bundleXMLFile = new File(bundlerUtilTest, "bundle.xml");

        assertTrue(bundleXMLFile.exists());

        PushPublisherConfig readConfig = (PushPublisherConfig) BundlerUtil.xmlToObject(bundleXMLFile);
        List<PublishQueueElement> bundlerAssets = readConfig.getAssets();

        assertEquals(1, bundlerAssets.size());
        assertEquals(contentType.id(), bundlerAssets.get(0).getAsset());

    }
}
