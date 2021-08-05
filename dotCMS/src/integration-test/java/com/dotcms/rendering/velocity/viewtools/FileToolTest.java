package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertNull;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileToolTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testGetFile_givenNotLiveFile_ShouldReturnNull()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet fileAsset = new FileAssetDataGen(site, binary).nextPersisted();

        final FileTool fileTool = new FileTool();
        final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                true, fileAsset.getLanguageId());
        assertNull(result);
    }

}
