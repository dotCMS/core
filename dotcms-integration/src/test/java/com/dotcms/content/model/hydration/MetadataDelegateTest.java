package com.dotcms.content.model.hydration;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.security.apps.SecretsStore;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.validation.constraints.AssertTrue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataDelegateTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    /**
     * Tested method {@link MetadataDelegate#normalize(File)}
     * The problem this method intends to fix is basically matching invalid paths coming from a starter that was generated and exported with an absolute route that does not exist locally.
     * Given scenario: We want to test that given both a valid and an invalid file we still get a normalized file
     * Expected Result: If the file we pass can be matched with a local file the returned file should exist. If it does not then the returned file should not exist
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_File_Asset_Path_Normalized() throws IOException, DotDataException, DotSecurityException {
        //Let's instantiate the class we want to test
        final MetadataDelegate delegate = new MetadataDelegate();
        //Now let's create a file
        final File file = FileUtil.createTemporaryFile("test", ".txt", "this is a test");
        //We know for sure the file exists so our tested method should agree with us
        Assert.assertTrue(delegate.normalize(file).exists());

        //Now lets mimic a situation where we have a contentlet that has a valid file asset locally
        //But we receive the same route but with prepended absolute route.
        //This typically what happens when we unpack a starter that was generated in cloud
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().name("testFolder").site(host).nextPersisted();
        final Contentlet contentlet = new FileAssetDataGen(file).host(host).folder(folder).setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        final File binary = (File)contentlet.get(FileAssetAPI.BINARY_FIELD);
        Assert.assertTrue(String.format("binary %s should exist. ",binary.getPath()),delegate.normalize(binary).exists());
        final String rootPath = ConfigUtils.getAbsoluteAssetsRootPath();
        final String path = binary.getPath();
        final int index = path.indexOf(rootPath);
        //Drop the relative part and prepend a route like the one that typically has a file packed in the starter
        final String assetPath = path.substring(index + rootPath.length() );
        //The route is slightly different, so we don't get an unwanted match when running tests in cloud
        final Path absoluteNoneExistingPath = Path.of("/data/shared2/" + assetPath);
        //Since we're mimicking an invalid file scenario. The file must not exist.
        Assert.assertFalse(absoluteNoneExistingPath.toFile().exists());
        //Moment of truth the file should be fixed and converted into the local equivalent file
        final File normalizedExistingPath = delegate.normalize(absoluteNoneExistingPath.toFile());

        Assert.assertEquals("We expect the normalized path to be the same as the binary",
                binary.getPath(), normalizedExistingPath.getPath());

        Assert.assertTrue(
                String.format("Path %s is expected to exist. binary path is %s. Root path %s. ",
                        normalizedExistingPath.getPath(), path, rootPath),
                normalizedExistingPath.exists());
    }


}
