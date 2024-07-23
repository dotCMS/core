package com.dotcms.rest.api.v1.page;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PageResourceHelperTest {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Template that is not advanced and the Multi_tree has a
     * relation_type legacy value
     * should: copy the Contentlet anyway
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void copyContentlet() throws DotDataException, DotSecurityException {

        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final Field field_2 = new FieldDataGen()
                .name("field2")
                .velocityVarName("field2")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .field(field_2)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .setProperty("field2", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen().setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType("1")
                .contentId(contentlet.getIdentifier())
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Contentlet contentletCopy = PageResourceHelper.getInstance().copyContentlet(copyContentletForm,
                APILocator.systemUser(), PageMode.PREVIEW_MODE, language);

        assertEquals(contentlet.getStringProperty("field1"), contentletCopy.getStringProperty("field1"));
        assertEquals(contentlet.getStringProperty("field2"), contentletCopy.getStringProperty("field2"));

        assertNotEquals(contentlet.getIdentifier(), contentletCopy.getIdentifier());
        assertNotEquals(contentlet.getInode(), contentletCopy.getInode());
    }

    /**
     * Method to test: {@link PageResourceHelper#copyContentlet(CopyContentletForm, User, PageMode, Language)}
     * when: Try to copy a Content from a Page where the Template is advanced and the Multi_tree has a
     * relation_type legacy value
     * should: copy the Contentlet anyway
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void copyContentletAdvancedTemplate() throws DotDataException, DotSecurityException {
        final RandomString randomString = new RandomString();

        final Field field_1 = new FieldDataGen()
                .name("field1")
                .velocityVarName("field1")
                .type(TextField.class)
                .next();

        final Field field_2 = new FieldDataGen()
                .name("field2")
                .velocityVarName("field2")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(field_1)
                .field(field_2)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("field1", randomString.nextString())
                .setProperty("field2", randomString.nextString())
                .nextPersisted();

        final Container container = new ContainerDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container, ContainerUUID.UUID_LEGACY_VALUE)
                .drawedBody(String.format("#parseContainer('%s')", container.getIdentifier()))
                .drawed(false)
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen().setContentlet(contentlet)
                .setPage(page)
                .setContainer(container)
                .setPersonalization(MultiTree.DOT_PERSONALIZATION_DEFAULT)
                .setInstanceID(ContainerUUID.UUID_LEGACY_VALUE)
                .nextPersisted();

        final CopyContentletForm copyContentletForm = new CopyContentletForm.Builder()
                .pageId(page.getIdentifier())
                .containerId(container.getIdentifier())
                .relationType(ParseContainer.PARSE_CONTAINER_UUID_PREFIX + "1")
                .contentId(contentlet.getIdentifier())
                .build();

        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        final Contentlet contentletCopy =  PageResourceHelper.getInstance().copyContentlet(copyContentletForm, APILocator.systemUser(),
                PageMode.PREVIEW_MODE, language);

        assertEquals(contentlet.getStringProperty("field1"), contentletCopy.getStringProperty("field1"));
        assertEquals(contentlet.getStringProperty("field2"), contentletCopy.getStringProperty("field2"));

        assertNotEquals(contentlet.getIdentifier(), contentletCopy.getIdentifier());
        assertNotEquals(contentlet.getInode(), contentletCopy.getInode());
    }
}
