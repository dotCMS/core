package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.enterprise.publishing.bundlers.FileAssetBundler.FILE_ASSET_EXTENSION;
import static com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class StaticPublisherIntegrationTestHelper {

    public static TestCase  getURLMapPageWithImage()
            throws DotDataException, IOException, DotSecurityException, WebAssetException {

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        final File testImage = new File(url.getFile());

        final Contentlet imageContentlet = FileAssetDataGen
                .createImageFileAssetDataGen(testImage)
                .host(host)
                .languageId(language.getId())
                .folder(folder)
                .nextPersisted();

        ContentletDataGen.publish(imageContentlet);

        final PageWithDependencies livePageWithContent = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder)
                .language(language)
                .buildAndPublishWithImage(imageContentlet);

        final ContentTypeWithDependencies urlMapContentType = createURLMapContentType(
                livePageWithContent);

        final Map<String, String> assetsMap = getAssetsMap(urlMapContentType.detailPage.page);

        final String xmlFilePath = getXmlFilePath(livePageWithContent);
        final String pageFilePath = getPageFilePath(livePageWithContent);
        final String imageFilePath = getImageFilePath(
                language,
                imageContentlet, urlMapContentType.detailPage);

        final List<FileExpected> filesExpected = list(
                new FileExpected(xmlFilePath, null),
                new FileExpected(pageFilePath, String.format("<div>%s</div>", urlMapContentType.detailPage.code)),
                new FileExpected(imageFilePath, testImage)
        );

        return new TestCase(urlMapContentType.detailPage.page, filesExpected, list(language), assetsMap);
    }

    @NotNull
    private static Map<String, String> getAssetsMap(Contentlet page) {
        return map(
                page.getIdentifier(), PusheableAsset.CONTENTLET.getType()
        );
    }

    public static TestCase getPageWithImage()
            throws IOException, DotDataException, DotSecurityException, WebAssetException {

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        final File testImage = new File(url.getFile());

        final Contentlet imageContentlet = FileAssetDataGen
                .createImageFileAssetDataGen(testImage)
                .host(host)
                .languageId(language.getId())
                .folder(folder)
                .nextPersisted();

        ContentletDataGen.publish(imageContentlet);

        final PageWithDependencies livePageWithContent = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder)
                .language(language)
                .buildAndPublishWithImage(imageContentlet);

        final String xmlFilePath = getXmlFilePath(livePageWithContent);
        final String pageFilePath = getPageFilePath(livePageWithContent);
        final String imageFilePath = getImageFilePath(language, imageContentlet, livePageWithContent);

        final List<FileExpected> filesExpected = list(
                new FileExpected(xmlFilePath, null),
                new FileExpected(pageFilePath, String.format("<div>%s</div>", livePageWithContent.code)),
                new FileExpected(imageFilePath, testImage)
        );

        final Map<String, String> assetsMap = getAssetsMap(livePageWithContent.page);

        return new TestCase(livePageWithContent.page, filesExpected, list(language), assetsMap);
    }

    private static String getImageFilePath(final Language language,
            final Contentlet imageContentlet,
            final PageWithDependencies livePageWithContent) {
        final String imageFilePath = File.separator
                + "live" + File.separator
                + livePageWithContent.host.getHostname() + File.separator
                + language.getId() + File.separator
                + "dA" + File.separator
                + imageContentlet.getIdentifier();
        return imageFilePath;
    }

    public static TestCase getContentTypeWithURlMap()
            throws DotDataException, DotSecurityException, WebAssetException {

        final ContentTypeWithDependencies contentTypeWithDependencies = createURLMapContentType();

        final Map<String, String> assetsMap = map(
                contentTypeWithDependencies.contentType.id(), PusheableAsset.CONTENT_TYPE.getType()
        );

        final Language language = APILocator.getLanguageAPI()
                .getLanguage(contentTypeWithDependencies.detailPage.language.getId());
        return new TestCase(contentTypeWithDependencies.contentType, list(), list(language), assetsMap);
    }

    public static TestCase getDeletedContentWithURlMap()
            throws WebAssetException, DotDataException, DotSecurityException {
        final TestCase workingContentWithURlMap = getWorkingContentWithURlMap();

        final Contentlet contentlet = (Contentlet) workingContentWithURlMap.addToBundle;
        ContentletDataGen.archive(contentlet);
        ContentletDataGen.delete(contentlet);

        return workingContentWithURlMap;
    }

    public static TestCase getLiveContentWithURlMap()
            throws WebAssetException, DotDataException, DotSecurityException {
        final TestCase workingContentWithURlMap = getWorkingContentWithURlMap();
        ContentletDataGen.publish((Contentlet) workingContentWithURlMap.addToBundle);

        return workingContentWithURlMap;
    }

    public static TestCase getWorkingContentWithURlMap()
            throws WebAssetException, DotDataException, DotSecurityException {

        final ContentTypeWithDependencies contentTypeWithDependencies = createURLMapContentType();

        final Host host = APILocator.getHostAPI()
                .find(contentTypeWithDependencies.detailPage.host.getIdentifier(), APILocator.systemUser(),
                        false);

        final Contentlet contentlet = new ContentletDataGen(contentTypeWithDependencies.contentType)
                .languageId(contentTypeWithDependencies.detailPage.language.getId())
                .host(host)
                .setProperty(contentTypeWithDependencies.textFieldUseInURLMap.variable(), "testing")
                .nextPersisted();

        ContentletDataGen.publish(contentlet);

        final Map<String, String> assetsMap = getAssetsMap(contentlet);

        final Language language = APILocator.getLanguageAPI()
                .getLanguage(contentlet.getLanguageId());

        final String urlMapPath = File.separator
                + "live" + File.separator
                + host.getHostname() + File.separator
                + language.getId()  + File.separator
                + "url-map-testing" + File.separator;

        final List<FileExpected> filesExpected = list(
                new FileExpected(urlMapPath + "testing.dotUrlMap.xml", null),
                new FileExpected(urlMapPath + "testing", "<div>Testing Field Value</div>")
        );

        return new TestCase(contentlet, filesExpected, list(language), assetsMap);
    }

    private static String getPageFileParentFolder(final PageWithDependencies detailPage) {
        final String pageFilePath = getPageFilePath(detailPage);

        final File file = new File(pageFilePath);
        return file.getParent();
    }

    private static ContentTypeWithDependencies createURLMapContentType()
            throws WebAssetException, DotDataException, DotSecurityException {
        return createURLMapContentType(null);
    }

    private static ContentTypeWithDependencies createURLMapContentType(PageWithDependencies livePageWithContent )
        throws WebAssetException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Template template = new TemplateDataGen().site(host).nextPersisted();
        TemplateDataGen.publish(template);

        final Language language;

        if (!UtilMethods.isSet(livePageWithContent)) {
            language = new LanguageDataGen().nextPersisted();

            livePageWithContent = new PageWithDependenciesBuilder()
                    .host(host)
                    .folder(folder)
                    .language(APILocator.getLanguageAPI().getDefaultLanguage())
                    .buildAndPublish();

            createNewVersionInDifferentLang(livePageWithContent.page, language, null);
            livePageWithContent.language = language;
        } else {
            language = livePageWithContent.language;
        }

        final ContentType pageContentletContentType = livePageWithContent.contentlet.getContentType();
        final Contentlet contentlet = createNewVersionInDifferentLang(livePageWithContent.contentlet, language,
                map(pageContentletContentType.fields().get(0).variable(), "Testing Field Value"));

        ContentletDataGen.publish(contentlet);
        ContentletDataGen.publish(livePageWithContent.page);

        final Field textField = new FieldDataGen().type(TextField.class).next();

        final ContentType contentType = new ContentTypeDataGen()
                .urlMapPattern("/url-map-testing/{" + textField.variable() + "}")
                .detailPage(livePageWithContent.page.getIdentifier())
                .field(textField)
                .nextPersisted();

        return new ContentTypeWithDependencies(contentType, livePageWithContent, textField);
    }

    public static TestCase getTwoPageDifferentHostSamePath()
            throws WebAssetException, DotDataException, DotSecurityException {

        final String folderName = "getTwoPageDifferentHostSamePath_" + System.currentTimeMillis();
        final Host host_1 = new SiteDataGen().nextPersisted();
        final Folder folder_1 = new FolderDataGen()
                .name(folderName)
                .site(host_1).nextPersisted();

        final PageWithDependencies livePageWithContent = new PageWithDependenciesBuilder()
                .host(host_1)
                .folder(folder_1)
                .buildAndPublish();

        final Host host_2 = new SiteDataGen().nextPersisted();
        final Folder folder_2 = new FolderDataGen()
                .name(folderName)
                .site(host_2).nextPersisted();

        final PageWithDependencies livePageWithContentAnotherHost = new PageWithDependenciesBuilder()
                .host(host_2)
                .folder(folder_2)
                .language(livePageWithContent.language)
                .buildAndPublish();

        final String xmlFilePath = getXmlFilePath(livePageWithContent);
        final String pageFilePath = getPageFilePath(livePageWithContent);

        final List<FileExpected> filesExpected = list(
                new FileExpected(xmlFilePath, null),
                new FileExpected(pageFilePath, "<div>Testing Field Value</div>")
        );

        final Map<String, String> assetsMap = getAssetsMap(livePageWithContent.page);

        return new TestCase(livePageWithContent.page, filesExpected, livePageWithContent.language, assetsMap);
    }

    public static TestCase getLiveFileAssetDifferentLangIncludingJustOneg()
            throws DotDataException, DotSecurityException, IOException {
        final TestCase livePageWithDifferentLang = getLiveFileAssetDifferentLang();
        livePageWithDifferentLang.languages = livePageWithDifferentLang.languages.stream()
                .limit(1).collect(Collectors.toList());
        livePageWithDifferentLang.filesExpected = livePageWithDifferentLang.filesExpected.stream()
                .limit(1).collect(Collectors.toList());
        return livePageWithDifferentLang;
    }

    public static TestCase getLiveFileAssetDifferentLang()
            throws DotDataException, DotSecurityException, IOException {
        final Language language_1 = new LanguageDataGen().nextPersisted();
        final Language language_2 = new LanguageDataGen().nextPersisted();

        final Contentlet fileAssetLive_1 = createFileAsset(language_1);
        ContentletDataGen.publish(fileAssetLive_1);


        final Contentlet fileAssetLive_2 = createNewVersionInDifferentLang(fileAssetLive_1, language_2, null);
        ContentletDataGen.publish(fileAssetLive_2);

        final Map<String, String> assetsMap = getAssetsMap(fileAssetLive_1);

        final List<FileExpected> filesExpected = list(
            new FileExpected(getFileAssetPath(fileAssetLive_1), "LIVE File Assets"),
            new FileExpected(getFileAssetPath(fileAssetLive_2), "LIVE File Assets")
        );

        return new TestCase(fileAssetLive_1, filesExpected, list(language_1, language_2), assetsMap);
    }

    public static TestCase getLiveFileAsset() throws DotDataException, DotSecurityException, IOException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Contentlet fileAssetLive = createFileAsset(language);

        ContentletDataGen.publish(fileAssetLive);

        final Map<String, String> assetsMap = getAssetsMap(fileAssetLive);

        final List<FileExpected> filesExpected = list(
                new FileExpected(getFileAssetPath(fileAssetLive), "LIVE File Assets")
        );

        return new TestCase(fileAssetLive, filesExpected, language, assetsMap);
    }

    public static TestCase getWorkingFileAsset() throws DotDataException, DotSecurityException, IOException {
        final Language language = new LanguageDataGen().nextPersisted();
        final Contentlet fileAssetWorking = createFileAsset(language);

        final Map<String, String> assetsMap = getAssetsMap(fileAssetWorking);

        return new TestCase(fileAssetWorking, list(), language, assetsMap);
    }

    private static Contentlet createFileAsset(Language language)
            throws DotSecurityException, DotDataException, IOException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        return new FileAssetDataGen(folder, "LIVE File Assets")
                .languageId(language.getId())
                .nextPersisted();
    }

    public static TestCase getFolderWithLiveFileAssetAndPage()
            throws WebAssetException, DotDataException, DotSecurityException, IOException {

        final HostWithDependencies hostWithDependencies = createHostWithDependencies();

        final Folder folderToAddInBundle = hostWithDependencies.folders.stream().findFirst().get();
        final Map<String, String> assetsMap = map(
                folderToAddInBundle.getIdentifier(), PusheableAsset.FOLDER.getType()
        );

        final List<FileExpected> filesExpected = new ArrayList<>();

        final Set<Language> languages = new HashSet<>();

        hostWithDependencies.getPages(folderToAddInBundle).stream()
                .filter(pageWithDependencies -> isLive(pageWithDependencies.page))
                .forEach(pageWithDependencies -> {
                    filesExpected.add(new FileExpected(getXmlFilePath(pageWithDependencies)));
                    filesExpected.add(new FileExpected(getPageFilePath(pageWithDependencies)));

                    languages.add(pageWithDependencies.language);
                });

        hostWithDependencies.getFileAssets(folderToAddInBundle).stream()
                .filter(fileAsset -> isLive(fileAsset))
                .forEach(fileAsset -> {
                    filesExpected.add(new FileExpected(getFileAssetPath(fileAsset), "LIVE File Assets in Folder " + folderToAddInBundle.getIdentifier()));

                    final Language language = APILocator.getLanguageAPI()
                            .getLanguage(fileAsset.getLanguageId());
                    languages.add(language);
                });

        return new TestCase(folderToAddInBundle, filesExpected, languages, assetsMap);
    }

    public static TestCase getHostWithLiveFileAssetAndPage()
            throws WebAssetException, DotDataException, DotSecurityException, IOException {

        final HostWithDependencies hostWithDependencies = createHostWithDependencies();

        final Map<String, String> assetsMap = map(
                hostWithDependencies.host.getIdentifier(), PusheableAsset.SITE.getType()
        );

        final List<FileExpected> filesExpected = new ArrayList<>();

        final Set<Language> languages = new HashSet<>();

        for (final Folder folder : hostWithDependencies.folders) {
            hostWithDependencies.getPages(folder).stream()
                    .filter(pageWithDependencies -> isLive(pageWithDependencies.page))
                    .forEach(pageWithDependencies -> {
                        filesExpected.add(new FileExpected(getXmlFilePath(pageWithDependencies)));
                        filesExpected.add(new FileExpected(getPageFilePath(pageWithDependencies)));

                        languages.add(pageWithDependencies.language);
                    });

            hostWithDependencies.getFileAssets(folder).stream()
                    .filter(fileAsset -> isLive(fileAsset))
                    .forEach(fileAsset -> {
                        filesExpected.add(new FileExpected(getFileAssetPath(fileAsset), "LIVE File Assets in Folder " + folder.getIdentifier()));

                        final Language language = APILocator.getLanguageAPI()
                                .getLanguage(fileAsset.getLanguageId());
                        languages.add(language);
                    });

        }

        return new TestCase(hostWithDependencies.host, filesExpected, languages, assetsMap);
    }

    private static boolean isLive(final Contentlet page) {
        try {
            return page.isLive();
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException();
        }
    }

    private static HostWithDependencies createHostWithDependencies()
            throws DotDataException, DotSecurityException, IOException, WebAssetException {

        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Folder folder_1 = new FolderDataGen().site(host).nextPersisted();
        final Contentlet fileAssetLive_1 = new FileAssetDataGen(folder_1, "LIVE File Assets in Folder " + folder_1.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        final PageWithDependencies livePageWithContent_1 = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder_1)
                .buildAndPublish();

        final PageWithDependencies workingPageWithContent_1 = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder_1)
                .build();

        final Folder folder_2 = new FolderDataGen().site(host).nextPersisted();
        final Contentlet fileAssetLive_2 = new FileAssetDataGen(folder_2, "LIVE File Assets in Folder " + folder_2.getIdentifier())
                .languageId(language.getId())
                .nextPersisted();

        final PageWithDependencies livePageWithContent_2 = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder_2)
                .buildAndPublish();

        final PageWithDependencies workingPageWithContent_2 = new PageWithDependenciesBuilder()
                .host(host)
                .folder(folder_2)
                .build();

        ContentletDataGen.publish(fileAssetLive_1);
        ContentletDataGen.publish(fileAssetLive_2);

        final Contentlet fileAssetWorking_1 = new FileAssetDataGen(folder_1, "WORKING File Assets in folder 1")
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet fileAssetWorking_2 = new FileAssetDataGen(folder_2, "WORKING File Assets in folder 2")
                .languageId(language.getId())
                .nextPersisted();

        HostWithDependencies hostWithDependencies =  new HostWithDependencies(host);
        hostWithDependencies.addFolder(folder_1,
                list(livePageWithContent_1, workingPageWithContent_1),
                list(fileAssetLive_1, fileAssetWorking_1));
        hostWithDependencies.addFolder(folder_2,
                list(livePageWithContent_2, workingPageWithContent_2),
                list(fileAssetLive_2, fileAssetWorking_2));

        return hostWithDependencies;
    }

    private static String getFileAssetPath(final Contentlet fileAsset) {
        try {
            final Identifier identifier = APILocator.getIdentifierAPI()
                    .find(fileAsset.getIdentifier());

            final Host host = APILocator.getHostAPI()
                    .find(fileAsset.getHost(), APILocator.systemUser(), false);

            final Language language = APILocator.getLanguageAPI()
                    .getLanguage(fileAsset.getLanguageId());

            final String filAssetPath = File.separator
                    + "live" + File.separator
                    + host.getHostname() + File.separator + language.getId()
                    + identifier.getURI().replace("/", File.separator);
            return filAssetPath;
        }catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestCase getLivePage()
            throws WebAssetException, DotDataException, DotSecurityException, IOException {
        final PageWithDependencies livePageWithContent = new PageWithDependenciesBuilder().buildAndPublish();

        final String xmlFilePath = getXmlFilePath(livePageWithContent);
        final String pageFilePath = getPageFilePath(livePageWithContent);

        final List<FileExpected> filesExpected = list(
                new FileExpected(xmlFilePath, null),
                new FileExpected(pageFilePath, "<div>Testing Field Value</div>")
        );

        final Map<String, String> assetsMap = getAssetsMap(livePageWithContent.page);

        return new TestCase(livePageWithContent.page, filesExpected, livePageWithContent.language, assetsMap);
    }

    public static TestCase getWorkingPage() {
        final PageWithDependencies workingPageWithContent = new PageWithDependenciesBuilder().build();

        return new TestCase(workingPageWithContent.page,
                Collections.EMPTY_LIST,
                workingPageWithContent.language,
                java.util.Collections.emptyMap());
    }

    public static TestCase getLivePageWithDifferentLangIncludingJustOne()
            throws WebAssetException, DotDataException, DotSecurityException {
        final TestCase livePageWithDifferentLang = getLivePageWithDifferentLang();
        livePageWithDifferentLang.languages = livePageWithDifferentLang.languages.stream()
                .limit(1).collect(Collectors.toList());
        livePageWithDifferentLang.filesExpected = livePageWithDifferentLang.filesExpected.stream()
                .limit(2).collect(Collectors.toList());
        return livePageWithDifferentLang;
    }

    public static TestCase getLivePageWithDifferentLang()
            throws WebAssetException, DotDataException, DotSecurityException {
        final PageWithDependencies livePageWithContent = new PageWithDependenciesBuilder().buildAndPublish();

        final Language language = new LanguageDataGen().nextPersisted();
        final List<Field> fields = livePageWithContent.contentlet.getContentType().fields();

        final Contentlet contentlet = createNewVersionInDifferentLang(
                livePageWithContent.contentlet, language, map(fields.get(0).variable(), "Content in another Lang"));
        ContentletDataGen.publish(contentlet);

        final PageWithDependencies pageAnotherLang = new PageWithDependenciesBuilder()
                .buildAnotherVersion(livePageWithContent, language);

        ContentletDataGen.publish(pageAnotherLang.page);

        final List<FileExpected> filesExpected = list(
                new FileExpected(getXmlFilePath(livePageWithContent)),
                new FileExpected(getPageFilePath(livePageWithContent), "<div>Testing Field Value</div>"),
                new FileExpected(getXmlFilePath(livePageWithContent, language)),
                new FileExpected(getPageFilePath(livePageWithContent, language), "<div>Content in another Lang</div>")
        );

        final Map<String, String> assetsMap = getAssetsMap(livePageWithContent.page);

        return new TestCase(livePageWithContent.page,
                filesExpected, list(livePageWithContent.language, language), assetsMap);
    }

    private static Contentlet createNewVersionInDifferentLang(final Contentlet contentlet,
            final Language language, final Map<String, String> fieldValues) throws DotDataException, DotSecurityException {

        final Contentlet contentletCheckout = APILocator.getContentletAPI().checkout(
                contentlet.getInode(), APILocator.systemUser(), false);
        final List<Field> fields = contentlet.getContentType().fields();

        if (UtilMethods.isSet(fieldValues))
        for (Entry<String, String> value : fieldValues.entrySet()) {
            final Field fieldFound = fields.stream()
                    .filter(field -> field.variable().equals(value.getKey()))
                    .collect(Collectors.toList()).get(0);

            contentletCheckout.setProperty(fieldFound.variable(), value.getValue());
        }

        contentletCheckout.setLanguageId(language.getId());
        contentletCheckout.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentletCheckout.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentletCheckout.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        return APILocator.getContentletAPI().checkin(contentletCheckout, APILocator.systemUser(), false);
    }

    private static String getXmlFilePath(final PageWithDependencies livePageWithContent) {
        return getXmlFilePath(livePageWithContent, livePageWithContent.language);
    }

    private static String getXmlFilePath(final PageWithDependencies livePageWithContent,
            final Language language) {
        try {
            return File.separator + "live" + File.separator
                    + livePageWithContent.host.getHostname() + File.separator
                    + language.getId()
                    + livePageWithContent.page.getURI().replace("/", File.separator)
                    + HTMLPAGE_ASSET_EXTENSION;
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getPageFilePath(final PageWithDependencies livePageWithContent, final Language language) {
        try {
            return File.separator
                    + "live" + File.separator
                    + livePageWithContent.host.getHostname() + File.separator
                    + language.getId()
                    + livePageWithContent.page.getURI().replace("/", File.separator);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getPageFilePath(final PageWithDependencies livePageWithContent) {
        return getPageFilePath(livePageWithContent, livePageWithContent.language);
    }

    public static class FileExpected {
        String path;
        Object content;

        public FileExpected(final String path) {
            this(path, null);
        }

        public FileExpected(final String path, final Object content) {
            this.path = path;
            this.content = content;
        }
    }

    public static class TestCase {
        Object addToBundle;
        List<FileExpected> filesExpected;
        Collection<Language> languages;
        Map<String, String> assetsMap;

        public TestCase(final Object addToBundle,
                final List<FileExpected> filesExpected,
                final Language language,
                final Map<String, String> assetsMap) {
            this(addToBundle, filesExpected, list(language), assetsMap);
        }

        public TestCase(final Object addToBundle,
                final List<FileExpected> filesExpected,
                final Collection<Language> languages,
                final Map<String, String> assetsMap) {
            this.addToBundle = addToBundle;
            this.filesExpected = filesExpected;
            this.languages = languages;
            this.assetsMap = assetsMap;
        }

        Optional<FileExpected> getFileExpected(final String absolutePath){
            for (FileExpected fileExpected : filesExpected) {
                if (absolutePath.endsWith(fileExpected.path)) {
                    return Optional.of(fileExpected);
                }
            }

            return Optional.empty();
        }
    }

    private static class PageWithDependencies {
        HTMLPageAsset page;
        Language language;
        Host host;
        Contentlet contentlet;
        Container container;
        Template template;
        String code;

        public PageWithDependencies(final HTMLPageAsset page,
                final Language language, final Host host, final Contentlet contentlet,
                final Container container, final Template template, final String code) {
            this.page = page;
            this.language = language;
            this.host = host;
            this.contentlet = contentlet;
            this.template = template;
            this.container = container;
            this.code = code;

        }
    }

    private static class HostWithDependencies {
        Host host;
        Set<Folder> folders = new HashSet<>();
        private Map<String, List<PageWithDependencies>> pagesByFolder = new HashMap<>();
        private Map<String, List<Contentlet>> fileAssetsByFolder = new HashMap<>();

        public HostWithDependencies(Host host) {
            this.host = host;
        }

        void addFolder(final Folder folder, final List<PageWithDependencies> pages, final List<Contentlet> fileAssets){
            folders.add(folder);
            pagesByFolder.put(folder.getIdentifier(), pages);
            fileAssetsByFolder.put(folder.getIdentifier(), fileAssets);
        }

        List<PageWithDependencies> getPages(final Folder folder) {
            return pagesByFolder.get(folder.getIdentifier());
        }

        List<Contentlet> getFileAssets(final Folder folder) {
            return fileAssetsByFolder.get(folder.getIdentifier());
        }
    }

    private static class ContentTypeWithDependencies{
        ContentType contentType;
        PageWithDependencies detailPage;
        Field textFieldUseInURLMap;

        public ContentTypeWithDependencies(ContentType contentType,
                PageWithDependencies detailPage, Field textFieldUseInURLMap) {
            this.contentType = contentType;
            this.detailPage = detailPage;
            this.textFieldUseInURLMap = textFieldUseInURLMap;
        }
    }

    private static class PageWithDependenciesBuilder {
        Host host;
        Folder folder;
        Language language;
        Contentlet contentlet;
        String code;

        PageWithDependenciesBuilder code(String code) {
            this.code = code;
            return this;
        }

        PageWithDependenciesBuilder host(Host host) {
            this.host = host;
            return this;
        }

        PageWithDependenciesBuilder folder(Folder folder) {
            this.folder = folder;
            return this;
        }

        PageWithDependenciesBuilder language(Language language) {
            this.language = language;
            return this;
        }

        PageWithDependenciesBuilder contentlet(Contentlet contentlet) {
            this.contentlet = contentlet;
            return this;
        }

        PageWithDependencies build() {

            if (!UtilMethods.isSet(host)){
                host = new SiteDataGen().nextPersisted();
            }

            if (!UtilMethods.isSet(folder)){
                folder = new FolderDataGen().site(host).nextPersisted();
            }

            if (!UtilMethods.isSet(language)){
                language = new LanguageDataGen().nextPersisted();
            }

            final Field textField = new FieldDataGen().type(TextField.class).next();
            final ContentType contentType = new ContentTypeDataGen()
                    .field(textField)
                    .nextPersisted();

            final Container container = new ContainerDataGen()
                    .site(host)
                    .withContentType(contentType, "$!{" + textField.variable() + "}")
                    .nextPersisted();

            final Template template = new TemplateDataGen()
                    .site(host)
                    .withContainer(container.getIdentifier())
                    .nextPersisted();

            if (!UtilMethods.isSet(contentlet)) {
                contentlet = new ContentletDataGen(contentType)
                        .host(host)
                        .languageId(language.getId())
                        .setProperty(textField.variable(), "Testing Field Value")
                        .nextPersisted();
            }

            final HTMLPageAsset page = ((HTMLPageDataGen) new HTMLPageDataGen(folder, template)
                    .host(host)
                    .languageId(language.getId()))
                    .nextPersisted();

            new MultiTreeDataGen()
                    .setContainer(container)
                    .setPage(page)
                    .setContentlet(contentlet)
                    .nextPersisted();

            return new PageWithDependencies(page, language, host, contentlet, container, template, code);
        }

        PageWithDependencies buildAndPublish()
                throws WebAssetException, DotDataException, DotSecurityException {

            final PageWithDependencies pageWithDependencies = build();
            ContentletDataGen.publish(contentlet);

            TemplateDataGen.publish(pageWithDependencies.template);
            ContainerDataGen.publish(pageWithDependencies.container);
            HTMLPageDataGen.publish(pageWithDependencies.page);

            return pageWithDependencies;
        }

        private PageWithDependencies buildWithImage(final Contentlet imageContentlet) {

            code = String.format("<img src=\"/dA/%s\" style=\"width:33px;\" class=\"img-circles border mr-2\">",
                    imageContentlet.getIdentifier());

            ContentType contentType = ContentTypeDataGen.createWidgetContentType(code)
                    .host(host)
                    .nextPersisted();

            contentlet = new ContentletDataGen(contentType)
                    .host(host)
                    .languageId(language.getId())
                    .setProperty("widgetTitle", "title_" + System.currentTimeMillis())
                    .nextPersistedAndPublish();

            return new PageWithDependenciesBuilder()
                    .host(host)
                    .folder(folder)
                    .language(language)
                    .contentlet(contentlet)
                    .code(code)
                    .build();
        }

        private PageWithDependencies buildAndPublishWithImage(final Contentlet imageContentlet)
                throws DotDataException, DotSecurityException, WebAssetException {

            final PageWithDependencies pageWithDependencies = buildWithImage(imageContentlet);
            ContentletDataGen.publish(contentlet);

            TemplateDataGen.publish(pageWithDependencies.template);
            ContainerDataGen.publish(pageWithDependencies.container);
            HTMLPageDataGen.publish(pageWithDependencies.page);

            return pageWithDependencies;
        }

        public static PageWithDependencies buildAnotherVersion(final PageWithDependencies pageWithDependencies,
                final Language language)
                throws DotDataException, DotSecurityException {

            Contentlet pageAnotherLang = APILocator.getContentletAPI().checkout(
                    pageWithDependencies.page.getInode(), APILocator.systemUser(), false);
            pageAnotherLang.setLanguageId(language.getId());
            pageAnotherLang.setIndexPolicy(IndexPolicy.WAIT_FOR);
            pageAnotherLang.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            pageAnotherLang.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            pageAnotherLang = APILocator.getContentletAPI().checkin(pageAnotherLang, APILocator.systemUser(), false);

            final HTMLPageAsset pageAnotherVersion = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI()
                    .findPage(pageAnotherLang.getInode(), APILocator.systemUser(), false);

            return new PageWithDependencies(pageAnotherVersion, language, pageWithDependencies.host,
                    pageWithDependencies.contentlet, pageWithDependencies.container, pageWithDependencies.template, null);
        }
    }

}
