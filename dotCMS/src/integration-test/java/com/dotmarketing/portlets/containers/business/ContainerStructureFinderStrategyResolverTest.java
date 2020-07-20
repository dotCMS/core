package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolver.PathContainerStructureFinderStrategyImpl;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;

public class ContainerStructureFinderStrategyResolverTest {


    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }


    /**
     * This tests that file based container content types are based on the file name, and not the
     * contentlet title. We create a file based container and verify the content type name. Then we
     * change the title on the contentlet (bt do not change the file name) which should not make a
     * difference. The content type returned should still be the same
     * 
     * @throws Exception
     */

    @Test
    public void get_container_structure_from_file_name_() throws Exception {

        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("texto", ".vtl");
        FileUtil.write(file, "helloworld");

        final String fileName = file.getName().replace(".vtl", "");
        Contentlet con = new FileAssetDataGen(folder, file).nextPersisted();
        FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(con);
        ContainerStructureFinderStrategyResolver a = new ContainerStructureFinderStrategyResolver();
        PathContainerStructureFinderStrategyImpl longNamedStrategy =
                        new ContainerStructureFinderStrategyResolver().new PathContainerStructureFinderStrategyImpl();

        assertEquals(longNamedStrategy.getVelocityVarName(fileAsset), fileName);

        // change the title, it should still work
        fileAsset.setTitle("thisIsNotMyName.vtl");
        fileAsset.setInode(null);
        con = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
        fileAsset = APILocator.getFileAssetAPI().fromContentlet(con);

        assertEquals(longNamedStrategy.getVelocityVarName(fileAsset), fileName);
    }

}
