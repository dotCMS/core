package com.dotcms.enterprise.publishing.remote;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.RelationshipDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reads back every file a real bundle is made of, with the serializer a receiver reads it with.
 *
 * <p>The trusted list narrows which types that serializer will reconstruct, and the question it
 * leaves open cannot be answered by looking at the list: whether anything a bundler actually
 * writes falls outside it. The other tests in this area name a type and assert what happens to
 * it, which only ever covers the types somebody thought of. This one asserts over whatever the
 * bundlers produced, so a type that reaches a bundle through a field nobody listed still has to
 * come back.</p>
 */
public class BundleTrustedListRoundTripTest extends IntegrationTestBase {

    private static final String FILTER_KEY = "TrustedListRoundTrip.yml";

    /**
     * The kinds of document this bundle has to contain for the round trip to mean anything. The
     * four bundlers that write JSON — content types, experiments, variants and OSGi plugins —
     * are deliberately absent: they go through Jackson, not the serializer the trusted list
     * governs.
     */
    private static final Set<String> EXPECTED_DOCUMENT_KINDS = Set.of(
            ".content.xml", ".contentworkflow.xml", ".workflow.xml", ".language.xml",
            ".host.xml", ".template.xml", ".folder.xml", ".container.xml", ".link.xml",
            ".rule.xml", ".relationship.xml", ".user.xml");

    private static User systemUser;
    private static Host site;
    private static Folder folder;
    private static Template template;
    private static Container container;
    private static Contentlet news;
    private static HTMLPageAsset page;
    private static Link link;
    private static Rule rule;
    private static Relationship relationship;
    private static User pushedUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        systemUser = APILocator.systemUser();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("dependencies", true);
        filters.put("relationships", true);
        filters.put("forcePush", false);
        APILocator.getPublisherAPI().addFilterDescriptor(
                new FilterDescriptor(FILTER_KEY, FILTER_KEY, filters, true,
                        "DOTCMS_BACK_END_USER"));

        site = new SiteDataGen().nextPersisted();
        folder = new FolderDataGen().site(site).nextPersisted();

        //A content type with a spread of field types, so the bundle carries more than strings
        final ContentType newsType = TestDataUtils.getNewsLikeContentType(site);
        news = TestDataUtils.getNewsContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(), newsType.id(), site);

        //Each of these is here to make one more kind of document appear in the bundle: the
        //container and the template it hangs off, the menu link, the rule on the page, and the
        //relationship between two content types.
        container = new ContainerDataGen().site(site)
                .withContentType(newsType, "$!{title}").nextPersisted();
        template = new TemplateDataGen().site(site).withContainer(container, "1").nextPersisted();
        page = new HTMLPageDataGen(folder, template).nextPersisted();
        link = new LinkDataGen(folder).hostId(site.getIdentifier()).nextPersisted();
        rule = new RuleDataGen().page(page).nextPersisted();
        relationship = new RelationshipDataGen(false).parentContentType(newsType)
                .childContentType(newsType).nextPersisted();

        //The only asset that brings a com.liferay.portal.model.User into a bundle, which is the
        //one entry the trusted list carries for that package. Its bundler runs only when a user
        //is pushed on purpose, so nothing else in this fixture would reach it.
        pushedUser = new UserDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (pushedUser != null) {
            UserDataGen.remove(pushedUser);
        }
        if (site != null) {
            APILocator.getHostAPI().archive(site, systemUser, false);
            APILocator.getHostAPI().delete(site, systemUser, false);
        }
    }

    /**
     * Builds a bundle with the real bundlers and reads every document in it back. A type the
     * trusted list does not cover shows up here as a failed read naming that type, which is the
     * one failure mode this change can introduce on a bundle that was fine before.
     */
    @Test
    public void testGeneratedBundle_everyDocumentShouldReadBack() throws Exception {
        final File bundleRoot = generateBundle(Arrays.asList(page.getIdentifier(),
                news.getIdentifier(), link.getIdentifier(), container.getIdentifier(),
                template.getIdentifier(), folder.getIdentifier(),
                UserBundler.USER_PREFIX + pushedUser.getUserId()));

        final List<Path> documents;
        try (Stream<Path> walk = Files.walk(bundleRoot.toPath())) {
            documents = walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        }

        Assert.assertFalse("The bundlers wrote no documents, so nothing was exercised",
                documents.isEmpty());
        Logger.info(BundleTrustedListRoundTripTest.class, "Bundle documents under test: "
                + documents.stream().map(path -> bundleRoot.toPath().relativize(path).toString())
                        .collect(Collectors.joining("\n  ", "\n  ", "")));

        //Named on purpose rather than left implicit: a bundler that stops emitting its kind of
        //document would otherwise quietly shrink what this test covers while still passing.
        final List<String> missingKinds = EXPECTED_DOCUMENT_KINDS.stream()
                .filter(suffix -> documents.stream()
                        .noneMatch(path -> path.toString().endsWith(suffix)))
                .collect(Collectors.toList());
        Assert.assertTrue("No document of these kinds reached the bundle, so the round trip "
                + "never exercised them: " + missingKinds, missingKinds.isEmpty());

        final List<String> failures = new ArrayList<>();
        for (final Path document : documents) {
            try {
                Assert.assertNotNull(BundlerUtil.xmlToObject(document.toFile()));
            } catch (final Exception e) {
                failures.add(bundleRoot.toPath().relativize(document) + ": " + e.getMessage());
            }
        }

        Assert.assertTrue(
                "Every document a bundler wrote must read back. Failed:\n"
                        + String.join("\n", failures),
                failures.isEmpty());
    }

    /**
     * Runs the publisher's own bundlers over the given assets and returns the directory they
     * wrote into. Mirrors what {@code PublisherAPIImpl} does when it builds a bundle, minus the
     * packing and the audit bookkeeping, neither of which produces a document.
     *
     * @param assetIds identifiers of the assets to put in the bundle
     * @return the directory holding the bundle's documents
     */
    private static File generateBundle(final List<String> assetIds) throws Exception {
        final Bundle bundle = new Bundle("TrustedListRoundTrip" + System.currentTimeMillis(),
                null, null, systemUser.getUserId(), false, FILTER_KEY);
        APILocator.getBundleAPI().saveBundle(bundle);
        PublisherAPI.getInstance().saveBundleAssets(assetIds, bundle.getId(), systemUser);

        final PushPublisherConfig config = new PushPublisherConfig();
        final List<PublishQueueElement> queued =
                PublisherAPI.getInstance().getQueueElementsByBundleId(bundle.getId());
        config.setDownloading(true);
        config.setOperation(Operation.PUBLISH);
        config.setAssets(new ArrayList<>(queued));
        config.setLuceneQueries(PublisherUtil.prepareQueries(queued));
        config.setId(bundle.getId());
        config.setUser(systemUser);

        final Publisher publisher = new PushPublisher();
        publisher.init(config);

        final File bundleRoot = BundlerUtil.getBundleRoot(config);
        final DirectoryBundleOutput output = new DirectoryBundleOutput(config, bundleRoot);
        BundlerUtil.writeBundleXML(config, output);

        for (final Class<IBundler> clazz : publisher.getBundlers()) {
            final IBundler bundler = clazz.newInstance();
            bundler.setConfig(config);
            bundler.setPublisher(publisher);
            Logger.info(BundleTrustedListRoundTripTest.class,
                    "Start of Bundler: " + clazz.getSimpleName());
            bundler.generate(output, new BundlerStatus(bundler.getClass().getName()));
        }

        return bundleRoot;
    }
}
