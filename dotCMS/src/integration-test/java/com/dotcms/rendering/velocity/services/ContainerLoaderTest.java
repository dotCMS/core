package com.dotcms.rendering.velocity.services;

import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.util.PageMode;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContainerLoaderTest {

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ContainerLoader#writeObject(VelocityResourceKey)}
     * when: the File Container's html is build
     * Should: return a div with the File Comntainer's id
     */
    @Test
    public void aaa() throws DotDataException, DotSecurityException, IOException {

        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        final FileAssetContainer container = (FileAssetContainer) APILocator.getContainerAPI()
                .find(fileAssetContainer.getInode(), APILocator.systemUser(), true);

        final ContainerLoader containerLoader = new ContainerLoader();

        final VelocityResourceKey velocityResourceKey = new VelocityResourceKey(fileAssetContainer, PageMode.EDIT_MODE);
        final InputStream inputStream = containerLoader.writeObject(velocityResourceKey);

        final String velocityCode = IOUtils.toString(inputStream, String.valueOf(StandardCharsets.UTF_8));

        final String expected = "<div data-dot-object=\"container\" data-dot-inode=\"" + container.getInode() +
                "\" data-dot-identifier=\"//" + host.getName() + container.getPath() + "\"";

        assertTrue(velocityCode.contains(expected));
    }
}
