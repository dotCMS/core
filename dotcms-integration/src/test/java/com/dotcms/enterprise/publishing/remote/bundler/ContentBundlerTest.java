package com.dotcms.enterprise.publishing.remote.bundler;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentBundlerTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link ContentBundler#generate(BundleOutput, BundlerStatus)}
     * When: Have a Content with LIVE and Working version
     * Should: Create a xml file for each veersion
     * @throws DotBundleException
     */
    @Test
    public void processContentWithTwoVersions() throws DotBundleException {
        final Field textField = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen().field(textField)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "Content-1")
                .nextPersisted();

        ContentletDataGen.publish(contentlet);

        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        checkout.setProperty(textField.variable(), "Content-2");
        ContentletDataGen.checkin(checkout);

        final PushPublisherConfig config = new PushPublisherConfig();

        config.add(contentlet, PusheableAsset.CONTENTLET, StringPool.BLANK);
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(contentType))
                .filter(filterDescriptor)
                .nextPersisted();

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);
        final BundlerStatus status = mock(BundlerStatus.class);

        ContentBundler contentBundler = new ContentBundler();
        contentBundler.setConfig(config);
        contentBundler.generate(directoryBundleOutput, status);

        final File bundleRoot = BundlerUtil.getBundleRoot(config.getName(), false);
        final List<File> files = FileUtil.listFilesRecursively(bundleRoot).stream()
                .filter(file -> file.isFile())
                .collect(Collectors.toList());


        assertEquals(4, files.size());
    }

}
