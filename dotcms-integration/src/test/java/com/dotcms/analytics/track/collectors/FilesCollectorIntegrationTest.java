package com.dotcms.analytics.track.collectors;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static com.dotcms.analytics.track.collectors.Collector.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Dependent
@RunWith(JUnit4WeldRunner.class)
public class FilesCollectorIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when:
     * - Create a Content TYpe with an Image field, let called it 'contentTypeWithImageField'
     * - Create a FileAsset pointing to an Image
     * - Create a Contentlet using the 'contentTypeWithImageField' ContentType created in the first step
     * - Try to collect the Analytics data pretending that the Image was hit using a
     * /dA/[contentTypeWithImageField Content's id]/{contentTypeWithImageField image field variable name}
     *
     * Should: collect the data using the Image Contentlet not the 'contentTypeWithImageField'  Contentlet
     */
    @Test
    public void registerdAUriInAnalytics() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field imageField = new FieldDataGen().type(ImageField.class).next();

        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(language.getId())
                .folder(imageFolder).nextPersisted();

        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();
        final Contentlet contentletWithImage = new ContentletDataGen(contentTypeWithImageField)
                .host(host)
                .setProperty(imageField.variable(), imageFileAsset.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        ContentletDataGen.publish(contentletWithImage);
        ContentletDataGen.publish(imageFileAsset);

        final String uri = String.format("/dA/%s/%s", contentletWithImage.getIdentifier(), imageField.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(collectorPayloadBean.get(EVENT_TYPE), EventType.FILE_REQUEST.getType());
        final HashMap<String, String> fileObjectFromPayload = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        final ContentType imageContentType = imageFileAsset.getContentType();

        assertEquals( imageFileAsset.getIdentifier(), fileObjectFromPayload.get(ID));
        assertEquals(imageFileAsset.getTitle(), fileObjectFromPayload.get(TITLE));
        assertEquals( uri, fileObjectFromPayload.get(URL));
        assertEquals(imageContentType.id(), fileObjectFromPayload.get(CONTENT_TYPE_ID));
        assertEquals(imageContentType.name(), fileObjectFromPayload.get(CONTENT_TYPE_NAME));
        assertEquals(imageContentType.variable(), fileObjectFromPayload.get(CONTENT_TYPE_VAR_NAME));
        assertEquals(imageContentType.baseType().name(), fileObjectFromPayload.get(BASE_TYPE));

    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when:
     * - Create a Content TYpe with an Image field, let called it 'contentTypeWithImageField'
     * - Create a FileAsset pointing to an Image
     * - Create a Contentlet using the 'contentTypeWithImageField' ContentType created in the first step
     * - Try to collect the Analytics data pretending that the Image was hit using a
     * /contentAsset/image/[contentTypeWithImageField Content's id]/{contentTypeWithImageField image field variable name}
     *
     * Should: collect the data using the Image Contentlet not the 'contentTypeWithImageField'  Contentlet
     */
    @Test
    public void registerContentAssetsUriInAnalytics() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field fieldTitle = new FieldDataGen().type(TextField.class).name("title").next();
        final Field fieldImage = new FieldDataGen().type(ImageField.class).name("image").next();

        final ContentType contentType = new ContentTypeDataGen().field(fieldTitle).field(fieldImage).nextPersisted();

        final Language imageFileLanguage = new UniqueLanguageDataGen().nextPersisted();
        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(imageFileLanguage.getId())
                .folder(imageFolder).nextPersisted();

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();
        final Contentlet contentletWithImage = new ContentletDataGen(contentTypeWithImageField)
                .host(host)
                .setProperty(imageField.variable(), imageFileAsset.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        final String uri = String.format("/contentAsset/image/%s/%s", contentletWithImage.getIdentifier(), fieldImage.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(collectorPayloadBean.get(EVENT_TYPE), EventType.FILE_REQUEST.getType());
        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        final ContentType imageContentType = imageFileAsset.getContentType();

        assertEquals(imageFileAsset.getIdentifier(), fileObject.get(ID));
        assertEquals(imageFileAsset.getTitle(), fileObject.get(TITLE));
        assertEquals(uri, fileObject.get(URL));
        assertEquals(imageContentType.id(), fileObject.get(CONTENT_TYPE_ID));
        assertEquals(imageContentType.name(), fileObject.get(CONTENT_TYPE_NAME));
        assertEquals(imageContentType.variable(), fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertEquals(imageContentType.baseType().name(), fileObject.get(BASE_TYPE));

    }

    @Test
    public void registerDotAssetsUriInAnalytics() throws IOException, DotDataException, DotSecurityException {
     throw new DotRuntimeException("test");
    }
}
