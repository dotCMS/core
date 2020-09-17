package com.dotcms.storage;

import static org.junit.Assert.assertNotNull;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentletMetadataAPITest {

    private static ContentletMetadataAPI contentletMetadataAPI;
    private static long langId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        contentletMetadataAPI = APILocator.getContentletMetadataAPI();
        langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

    }

    @Test
    public void Test_Generate_Metadata_From_FileAssets() throws IOException {
        final Contentlet fileAssetContentJPG = TestDataUtils
                .getFileAssetContent(true, langId, TestFile.JPG);

        final Contentlet fileAssetContentGIF = TestDataUtils
                .getFileAssetContent(true, langId, TestFile.GIF);

        final Contentlet fileAssetContentPNG = TestDataUtils
                .getFileAssetContent(true, langId, TestFile.PNG);

        final Contentlet fileAssetContentSVG = TestDataUtils
                .getFileAssetContent(true, langId, TestFile.SVG);

        final Contentlet fileAssetContentTXT = TestDataUtils
                .getFileAssetContent(true, langId, TestFile.TEXT);

        //JPG
        final ContentletMetadata metadataForJPG = contentletMetadataAPI
                .generateContentletMetadata(fileAssetContentJPG);

        assertNotNull(metadataForJPG);

        assertNotNull(metadataForJPG.getBasicMetadataMap());
        assertNotNull(metadataForJPG.getFullMetadataMap());

        //GIF
        final ContentletMetadata metadataForGIF = contentletMetadataAPI
                .generateContentletMetadata(fileAssetContentGIF);

        assertNotNull(metadataForGIF);

        assertNotNull(metadataForGIF.getBasicMetadataMap());
        assertNotNull(metadataForGIF.getFullMetadataMap());

        //PNG
        final ContentletMetadata metadataForPNG = contentletMetadataAPI
                .generateContentletMetadata(fileAssetContentPNG);

        assertNotNull(metadataForPNG);

        assertNotNull(metadataForPNG.getBasicMetadataMap());
        assertNotNull(metadataForPNG.getFullMetadataMap());

        //SVG
        final ContentletMetadata metadataForSVG = contentletMetadataAPI
                .generateContentletMetadata(fileAssetContentSVG);

        assertNotNull(metadataForSVG);

        assertNotNull(metadataForSVG.getBasicMetadataMap());
        assertNotNull(metadataForSVG.getFullMetadataMap());

        //TXT
        final ContentletMetadata metadataForTXT = contentletMetadataAPI
                .generateContentletMetadata(fileAssetContentTXT);

        assertNotNull(metadataForTXT);

        assertNotNull(metadataForTXT.getBasicMetadataMap());
        assertNotNull(metadataForTXT.getFullMetadataMap());

    }

    @Test
    public void Test_Generate_Metadata_From_ContentType_With_Multiple_Binary_Fields() throws IOException {
        //Multiple binary fields
        final Contentlet binariesContent = TestDataUtils
                .getMultipleBinariesContent(true, langId, null);

        final ContentletMetadata multiBinaryMetadata = contentletMetadataAPI
                .generateContentletMetadata(binariesContent);

        System.out.println(multiBinaryMetadata.getFullMetadataMap());
        System.out.println(multiBinaryMetadata.getBasicMetadataMap());

    }

    @Test
    public void Test_Get_First_Indexed_Binary_Field() throws IOException {
        final ContentletMetadataAPIImpl impl = (ContentletMetadataAPIImpl)contentletMetadataAPI;
       // impl.findBinaryFields()
    }

}
