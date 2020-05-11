package com.dotcms.enterprise;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

public class HTMLDiffUtilTest {

    ContentletAPI  contentletAPI;

    LanguageAPI languageAPI;

    MultiTreeAPI multiTreeAPI;

    private void prepare(){
    }

    public void test() throws DotDataException, DotSecurityException {
       Host site = new SiteDataGen().nextPersisted();
       Folder folder = new FolderDataGen().site(site).nextPersisted();
       Language defaultLang = languageAPI.getDefaultLanguage();

        final User systemUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
        String contentGenericId = contentGenericType.id();

     //  createHtmlPageAsset(folder,defaultLang,)

    }

    private HTMLPageAsset createHtmlPageAsset(final Folder folder, final Language lang, final Contentlet contentlet, final Host site,final User user)
            throws DotSecurityException, WebAssetException, DotDataException {


        Contentlet contentletDefaultLang = TestDataUtils
                .getEmployeeContent(true, lang.getId(), null, site);

        contentletDefaultLang = contentletAPI
                .find(contentletDefaultLang.getInode(), user, false);
        contentletDefaultLang.setStringProperty("firstName", "firstName");
        contentletDefaultLang = contentletAPI.checkin(contentletDefaultLang, user, false);
        ContentletDataGen.publish(contentletDefaultLang);

        final Container container = new ContainerDataGen().withContentType(contentlet
                .getContentType(), "$!{firstName}").nextPersisted();

        ContainerDataGen.publish(container);

        final String uuid = UUIDGenerator.generateUuid();

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid)
                .nextPersisted();

        TemplateDataGen.publish(template);

        HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(lang.getId())
                .nextPersisted();

        HTMLPageDataGen.publish(page);

        final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                container.getIdentifier(),
                contentlet.getIdentifier(), getDotParserContainerUUID(uuid), 0);

        multiTreeAPI.saveMultiTree(multiTree);

        return page;
    }

}
