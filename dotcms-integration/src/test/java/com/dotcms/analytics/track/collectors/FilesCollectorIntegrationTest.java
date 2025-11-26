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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static com.dotcms.analytics.track.collectors.Collector.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Dependent
@RunWith(JUnit4WeldRunner.class)
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
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
         final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(language.getId())
                .folder(imageFolder).nextPersisted();

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();
        final Contentlet contentletWithImage = new ContentletDataGen(contentTypeWithImageField)
                .host(host)
                .setProperty(imageField.variable(), imageFileAsset.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        ContentletDataGen.publish(contentletWithImage);
        ContentletDataGen.publish(imageFileAsset);

        final String uri = String.format("/contentAsset/image/%s/%s", contentletWithImage.getIdentifier(), imageField.variable());

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

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /dA end point an identifier that does not exist
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitNotExistsIdWithDAEndpoint()  {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();
        
        final String uri = String.format("/dA/not_exists/%s", imageField.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }


    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /contentAssets end point an identifier that does not exist
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitNotExistsIdWithContentAssetEndpoint() {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();

        final String uri = String.format("/contentAsset/image/not_exists/%s", imageField.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to hoy using the /dA end point and the field name does not exist
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitNotExistsFieldNameWithDAEndpoint() throws IOException, DotDataException, DotSecurityException {
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

        final String uri = String.format("/dA/%s/not_exists", contentletWithImage.getIdentifier());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to hoy using the /dA end point and the field name does not exist
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitNotExistsFieldNameWithContentAssetsEndpoint() throws IOException, DotDataException, DotSecurityException {
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

        final String uri = String.format("/contentAsset/image/%s/not_exists", contentletWithImage.getIdentifier());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));

    }


    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /dA end point with a wrong syntax
     * /dA/fieldName
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitDAEndpointWithNoRightSyntaxNotId()  {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field imageField = new FieldDataGen().type(ImageField.class).next();
        final ContentType contentTypeWithImageField = new ContentTypeDataGen().host(host).field(imageField).nextPersisted();

        final String uri = String.format("/dA/%s", imageField.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /dA end point with a wrong syntax
     * /dA/
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitDAEndpointWithNoRightSyntaxNotIdFieldName()  {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final String uri = "/dA";

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /contentAssets end point with a wrong syntax
     * /contentAssets/image/fieldName
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitContentAssetsEndpointWithNoRightSyntaxNotId()  {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Field imageField = new FieldDataGen().type(ImageField.class).next();

        final String uri = String.format("/contentAsset/image/%s", imageField.variable());

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when: Try to use the /dA end point with a wrong syntax
     * /contentAssets/
     *
     * Should: not throw a NullPointerException
     */
    @Test
    public void hitContentAssetsEndpointWithNoRightSyntaxNotIdNotFieldName()  {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final String uri = "/contentAssets";

        when(collectorContextMap.get(CollectorContextMap.URI)).thenReturn(uri);
        when(collectorContextMap.get(CollectorContextMap.HOST)).thenReturn(host.getIdentifier());
        when(collectorContextMap.get(CollectorContextMap.CURRENT_HOST)).thenReturn(host);
        when(collectorContextMap.get(CollectorContextMap.LANG_ID)).thenReturn(language.getId());
        when(collectorContextMap.get(CollectorContextMap.LANG)).thenReturn(language.getLanguageCode());

        CollectorPayloadBean collect = filesCollector.collect(collectorContextMap, collectorPayloadBean);

        assertEquals(EventType.FILE_REQUEST.getType(), collectorPayloadBean.get(EVENT_TYPE));
        assertEquals(uri, collectorPayloadBean.get(URL));

        final HashMap<String, String> fileObject = (HashMap<String, String>) collectorPayloadBean.get(OBJECT);

        assertNull(fileObject.get(ID));
        assertNull(fileObject.get(TITLE));
        assertNull( fileObject.get(URL));
        assertNull(fileObject.get(CONTENT_TYPE_ID));
        assertNull( fileObject.get(CONTENT_TYPE_NAME));
        assertNull( fileObject.get(CONTENT_TYPE_VAR_NAME));
        assertNull( fileObject.get(BASE_TYPE));
    }

    /**
     * Method to test: {@link FilesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     * when:
     * - Create a Image Contentlet
     * - Try to collect the Analytics data pretending that the Image was hit using a
     *
     * /dA/[Shorty ID]/[COntentlet's title]?language_id=[language's id]
     *
     * Should: collect the data using the Image Contentlet
     */
    @Test
    public void usingDAURLWithTitleAndShortyID() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(language.getId())
                .folder(imageFolder).nextPersisted();

        ContentletDataGen.publish(imageFileAsset);

        final String imageContentletShortify = APILocator.getShortyAPI().shortify(imageFileAsset.getIdentifier());

        final String uri = String.format("/dA/%s/$s?language_id=%s", imageContentletShortify, imageFileAsset.getTitle(),
                imageFileAsset.getLanguageId());

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
     * - Create a Image Contentlet
     * - Try to collect the Analytics data pretending that the Image was hit using a
     *
     * /dA/[Shorty INODE]/asset/20250215_161621.jpg
     *
     * Should: collect the data using the Image Contentlet
     */
    @Test
    public void usingDAURLWithTitleAndShortyInode() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(language.getId())
                .folder(imageFolder).nextPersisted();

        ContentletDataGen.publish(imageFileAsset);

        final String imageContentletShortify = APILocator.getShortyAPI().shortify(imageFileAsset.getInode());

        final String uri = String.format("/dA/%s/$s?language_id=%s", imageContentletShortify, imageFileAsset.getTitle(),
                imageFileAsset.getLanguageId());

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
     * - Create a Image Contentlet
     * - Try to collect the Analytics data pretending that the Image was hit using a
     *
     * /dA/[Shorty INODE]/asset/20250215_161621.jpg
     *
     * Should: collect the data using the Image Contentlet
     */
    @Test
    public void usingDAURLWithTitleAndShortyIDAndAssetWord() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final FilesCollector filesCollector = new FilesCollector();

        final CollectorContextMap collectorContextMap = mock(CollectorContextMap.class);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBeanWithBaseMap(new HashMap<>());

        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();

        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .languageId(language.getId())
                .folder(imageFolder).nextPersisted();

        ContentletDataGen.publish(imageFileAsset);

        final String imageContentletShortify = APILocator.getShortyAPI().shortify(imageFileAsset.getInode());

        final String uri = String.format("/dA/%s/asset/$s?language_id=%s", imageContentletShortify, imageFileAsset.getTitle(),
                imageFileAsset.getLanguageId());

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
}
