package com.dotmarketing.portlets.browser;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowserUtilTest {

    public static final String CONTENTLET = "contentlet";
    public static final String FIELD = "field";
    public static final String HOST = "Host";
    public static final String FOLDER = "folder";
    public static final String LANGUAGE = "language";
    public static final String CONTENT_TYPE = "content_type";

    @BeforeClass
    public static void prepare () throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link Contentlet} has an Image field with value
     * Should: Return the folder from the Contentlet's Image value
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToExistingImage()
            throws DotDataException, IOException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldAndValue();
        addFolderHostField((ContentType) map.get(CONTENT_TYPE), (Contentlet) map.get(CONTENTLET));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        selectAsLastFolder(httpSession);

        addDefaultPathFieldVariable((Host) map.get(HOST), (Field) map.get(FIELD),
                ((Folder) map.get(FOLDER)).getPath());

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        assertEquals(((Folder) map.get(FOLDER)).getIdentifier(),
                defaultPathFolder.get().getIdentifier());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link Field} has a defaultPath's Field Variable with a relative path
     * Should: Return the folder set in the defaultPath
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFieldVariable() throws DotDataException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();
        addFolderHostField((ContentType) map.get(CONTENT_TYPE), (Contentlet) map.get(CONTENTLET));

        final Host host = (Host) map.get(HOST);
        final Folder folder = (Folder) map.get(FOLDER);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getIdentifier());

        selectAsLastFolder(httpSession);

        final Field field = (Field) map.get(FIELD);

        final Folder defaultPathFolderValue = new FolderDataGen().site(host).nextPersisted();
        addDefaultPathFieldVariable(host, field, defaultPathFolderValue.getPath());

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), field, APILocator.systemUser());

        assertEquals(defaultPathFolderValue.getIdentifier(), defaultPathFolder.get().getIdentifier());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link Field} has a wrong defaultPath's Field Variable
     * Should: Return a {@link Optional#empty()}
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToWrongFieldVariable() throws DotDataException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final Host host = (Host) map.get(HOST);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getIdentifier());

        final Field field = (Field) map.get(FIELD);

        addDefaultPathFieldVariable(host, field, "wrong_path");

        final Optional<Folder> addFolderHostField = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), field, APILocator.systemUser());

        assertFalse(addFolderHostField.isPresent());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link ContentType} has an Image field without value and a Host/Folder field
     * Should: Return the folder from the Contentlet's Host/Folder field value
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFolderHostField() {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();
        final Folder folder = addFolderHostField(
                (ContentType) map.get(CONTENT_TYPE),
                (Contentlet) map.get(CONTENTLET));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);

        selectAsLastFolder(httpSession);

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        assertEquals(folder.getIdentifier(), defaultPathFolder.get().getIdentifier());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link ContentType} has an Host/Folder field with no value
     * Should: Return the folder from the Contentlet's Host/Folder field value
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFolderHostFieldNoValue() {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();
        final Folder folder = addFolderHostFieldNoValue(
                (ContentType) map.get(CONTENT_TYPE),
                (Contentlet) map.get(CONTENTLET));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        assertFalse(defaultPathFolder.isPresent());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link ContentType} has an Image field and a Host/Folder field both without values
     * but with a Folder selected as Current File browser selected folder
     * Should: Return the Current File browser selected folder
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFolderSelectedFolder() {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        final Folder lastSelectedFolder = selectAsLastFolder(httpSession);

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        assertEquals(lastSelectedFolder.getIdentifier(), defaultPathFolder.get().getIdentifier());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link ContentType} has an Image field and a Host/Folder field both without values
     * and no Folder selected as Current File browser selected folder
     * Should: Return a empty Optional
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFolderCurrentHost() {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        assertFalse(defaultPathFolder.isPresent());
    }

    private Folder selectAsLastFolder(final HttpSession httpSession) {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        when(httpSession.getAttribute("LAST_SELECTED_FOLDER_ID")).thenReturn(folder.getIdentifier());

        return folder;
    }

    private Folder addFolderHostFieldNoValue(final ContentType contentType,
            final Contentlet contentlet) {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Field hostFolderField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .type(HostFolderField.class)
                .nextPersisted();
        ContentTypeDataGen.addField(hostFolderField);

        return folder;
    }

    private Folder addFolderHostField(final ContentType contentType,
            final Contentlet contentlet) {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Field hostFolderField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .type(HostFolderField.class)
                .nextPersisted();
        ContentTypeDataGen.addField(hostFolderField);

        contentlet.setProperty(hostFolderField.variable(), folder.getInode());

        return folder;
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolder(Contentlet, Field, User)}
     * When: The {@link Field} has a defaultPath's Field Variable with an absolute path
     * Should: Return the folder set in the defaultPath
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFieldVariableAbsolutePath() throws DotDataException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final Host host = (Host) map.get(HOST);
        final Folder folder = (Folder) map.get(FOLDER);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getIdentifier());

        final Field field = (Field) map.get(FIELD);

        addDefaultPathFieldVariable(host, field, "//" + host.getHostname() + folder.getPath());

        final Optional<Folder> defaultPathFolder = BrowserUtil.getDefaultPathFolder(
                (Contentlet) map.get(CONTENTLET), field, APILocator.systemUser());

        assertEquals(folder.getIdentifier(), defaultPathFolder.get().getIdentifier());
    }

    private void addDefaultPathFieldVariable(
            final Host host, final Field field, final String value)
            throws DotDataException, DotSecurityException {

        FieldVariable defaultPathVariable = ImmutableFieldVariable.builder()
                .fieldId(field.id())
                .key("defaultPath")
                .value(value)
                .userId(APILocator.systemUser().getUserId())
                .build();

        APILocator.getContentTypeFieldAPI().save(defaultPathVariable, APILocator.systemUser());
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolderPathIds(Contentlet, Field, User)}
     * When: The {@link Contentlet} has an Image field with value
     * Should: Return a list with all the Contentlet's Image value parent folders ids
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToExistingImagePathIds()
            throws DotDataException, IOException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldAndValue();

        final Host host = (Host) map.get(HOST);
        final Folder folder = (Folder) map.get(FOLDER);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);

        final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                (Contentlet) map.get(CONTENTLET), (Field) map.get(FIELD), APILocator.systemUser());

        final List<String> expected = list(host.getIdentifier(), folder.getParentPermissionable().getPermissionId(),
                folder.getInode());

        assertEquals(expected, defaultPathFolderPathIds);
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolderPathIds(Contentlet, Field, User)}
     * When: The {@link Field} has a defaultPath's Field Variable
     * Should: Return the PathIds for the folder set in the defaultPath
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFieldVariablePathIds() throws DotDataException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final Host host = (Host) map.get(HOST);
        final Folder folder = (Folder) map.get(FOLDER);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getIdentifier());

        final Field field = (Field) map.get(FIELD);

        addDefaultPathFieldVariable(host, field, folder.getPath());

        final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                (Contentlet) map.get(CONTENTLET), field, APILocator.systemUser()
        );

        final List<String> expected = list(host.getIdentifier(), folder.getParentPermissionable().getPermissionId(),
                folder.getInode());

        assertEquals(expected, defaultPathFolderPathIds);
    }

    /**
     * Method to test: {@link BrowserUtil#getDefaultPathFolderPathIds(Contentlet, Field, User)}
     * When: The {@link Field} has a defaultPath's Field Variable with an absolute path
     * Should: Return the PathIds for the folder set in the defaultPath
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void defaultPathEqualsToFieldVariableAbsolutePathPathIds() throws DotDataException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final Host host = (Host) map.get(HOST);
        final Folder folder = (Folder) map.get(FOLDER);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final HttpSession httpSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME)).thenReturn(host.getIdentifier());

        final Field field = (Field) map.get(FIELD);

        addDefaultPathFieldVariable(host, field, "//" + host.getHostname() + folder.getPath());

        final List<String> defaultPathFolderPathIds = BrowserUtil.getDefaultPathFolderPathIds(
                (Contentlet) map.get(CONTENTLET), field, APILocator.systemUser());

        final List<String> expected = list(host.getIdentifier(), folder.getParentPermissionable().getPermissionId(),
                folder.getInode());

        assertEquals(expected, defaultPathFolderPathIds);
    }

    private Map<String, Object> createContentletWithImageFieldWithoutValue() {

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder folder = new FolderDataGen()
                .parent(parentFolder)
                .nextPersisted();

        final Field fieldImage = new FieldDataGen()
                .type(ImageField.class)
                .next();
        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .field(fieldImage)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .nextPersisted();

        return new HashMap<>(Map.of(
                CONTENTLET, contentlet,
                FIELD, contentType.fields().get(0),
                HOST, host,
                FOLDER, folder,
                LANGUAGE, language,
                CONTENT_TYPE, contentType
        ));
    }

    private Map<String, Object> createContentletWithImageFieldAndValue()
            throws DotDataException, IOException, DotSecurityException {

        final Map<String, Object> map = createContentletWithImageFieldWithoutValue();

        final URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        final File testImage = new File(url.getFile());

        final Contentlet imageContentlet = FileAssetDataGen
                .createImageFileAssetDataGen(testImage)
                .host((Host) map.get(HOST))
                .languageId(((Language) map.get(LANGUAGE)).getId())
                .folder((Folder) map.get(FOLDER))
                .nextPersisted();

        final Field fieldImage = (Field) map.get(FIELD);
        Contentlet contentlet = (Contentlet) map.get(CONTENTLET);
        contentlet = ContentletDataGen.checkout(contentlet);
        contentlet.setProperty(fieldImage.variable(), imageContentlet.getIdentifier());

        ContentletDataGen.checkin(contentlet);

        map.put(CONTENTLET, contentlet);
        map.put(FIELD, fieldImage);

        return map;
    }
}
